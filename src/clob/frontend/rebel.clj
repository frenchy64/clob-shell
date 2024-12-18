(ns clob.frontend.rebel
  (:gen-class)
  (:require [rebel-readline.clojure.line-reader :as clj-line-reader]
            [rebel-readline.clojure.main :refer [syntax-highlight-prn]]
            [rebel-readline.clojure.service.local :as clj-service]
            [rebel-readline.core :as core]
            [rebel-readline.jline-api :as api]
            [clojure.string :as string]
            [clojure.java.io :as jio]
            [clob.env :as env]
            [clob.reader]
            [clob.core :as clob.core]
            [clob.platform.process :refer [process?]]
            [clob.platform.eval :as eval]
            [clob.frontend.main :as main]
            [clob.service.completion :refer [complete-shell]]
            [clob.utils.clojure-main :refer [repl-requires] :as clojure-main]
            [clob.frontend.jline-history :as jline-history])
  (:import [org.jline.reader Completer ParsedLine LineReader]
           (org.jline.reader.impl LineReaderImpl)))

(defn handle-prompt-exception [e]
  (let [ex-map (Throwable->map e)]
    (println "\nError printing prompt:" (or (:cause ex-map)
                                            (-> ex-map :via first :type))))
  (println "Please check the definition of clob-prompt function in your ~/.clobrc")
  (print "$ "))

(defn repl-prompt []
  (try
    (eval/eval '(print (clob-prompt)))
    (catch Exception e
      (if (or (instance? java.lang.InterruptedException e)
              ;; SCI wraps the exception in ex-info
              (instance? java.lang.InterruptedException (ex-cause e)))
        (do (println "\nInterrupted")
            (Thread/interrupted)
            (try
              (eval/eval '(print (clob-prompt)))
              (catch Exception e
                (handle-prompt-exception e))))
        (handle-prompt-exception e))))
  (let [title
        (try
          (eval/eval '(clob-title))
          (catch Exception e
            (str "clob: Error in (clob-title): " (:cause (Throwable->map e)))))]
    (.print System/out (str "\u001b]0;" title "\u0007"))))

(def opts {:prompt repl-prompt})

; rebel-readline.clojure.main/create-repl-read
(def create-repl-read
  (core/create-buffered-repl-reader-fn
   (fn [s] (clojure.lang.LineNumberingPushbackReader.
            (java.io.StringReader. s)))
   core/has-remaining?
   clob.frontend.main/repl-read))

(defn repl-print
  [& args]
  (when-not (or (nil? (first args))
                (identical? (first args) env/success)
                (process? (first args)))
    (apply syntax-highlight-prn args)))

; rebel-readline.clojure.line-reader/clojure-completer
(defn clojure-completer []
  (proxy [Completer] []
    (complete [^LineReader reader ^ParsedLine line ^java.util.List candidates]
      (let [word (.word line)]
        (when (and (:completion @api/*line-reader*)
                   (not (string/blank? word))
                   (pos? (count word)))
          (let [options (let [ns' (clj-line-reader/current-ns)
                              context (clj-line-reader/complete-context line)]
                          (cond-> {}
                            ns'     (assoc :ns ns')
                            context (assoc :context context)))
                {:keys [cursor word-cursor line]} (meta line)]
            ;; not sure how to unit test this, but sometimes these are nil and triggers NPE
            ;; $ (let [foo 1] f<space>
            (when (and cursor word-cursor line)
              (let [paren-begin (= \( (get line (- cursor word-cursor 1)))
                    shell-completions (->> (complete-shell (subs line 0 cursor))
                                           (map (fn [candidate] {:candidate candidate})))
                    clj-completions (clj-line-reader/completions word options)]
                (->>
                  (if paren-begin
                    (concat
                      clj-completions
                      shell-completions)
                    (concat
                      shell-completions
                      clj-completions))
                  (map #(clj-line-reader/candidate %))
                  (take 10)
                  (.addAll candidates))))))))))

(defn load-init-file
  "Loads init file."
  [init-path]
  (when (.isFile (jio/file init-path))
    (eval/eval `(~'load-file ~init-path))))

(defn register-sigint-handler []
  (let [thread (Thread/currentThread)]
    (sun.misc.Signal/handle
     (sun.misc.Signal. "INT")
     (proxy [sun.misc.SignalHandler] []
       (handle [signal]
         ;; Thread.stop() method is deprecated, but it is used instead of Thread.interrupt()
         ;; which does not work for computations like `(doseq [i (range 1000000)] (println i))`
         (.stop thread))))))

(defn repl [[_ & args] inits]
  (core/ensure-terminal
   (core/with-line-reader
     (let [line-reader ^LineReaderImpl (clj-line-reader/create
                                        (clj-service/create
                                         (when api/*line-reader* @api/*line-reader*))
                                        {:completer (clojure-completer)})]
       (.setVariable line-reader LineReader/HISTORY_FILE (str (jio/file (System/getProperty "user.home") ".clob" "history")))
       (try
         (.setHistory line-reader (doto (jline-history/sqlite-history)
                                    (.moveToEnd)))
         (catch Exception e
           (binding [*out* *err*]
             (println "Error while initializing history file ~/.clob/clob.sqlite:\n" e))))
       line-reader)
     (binding [*out* (api/safe-terminal-writer api/*line-reader*)]
       (when-let [prompt-fn (:prompt opts)]
         (swap! api/*line-reader* assoc :prompt prompt-fn))
        ; (println (core/help-message))
       (apply
        clojure.main/repl
        (-> {:init (fn []
                     (clojure-main/initialize args inits)
                     (in-ns 'user)
                     (apply require repl-requires)
                     (in-ns 'user)
                     (eval/eval-clob-requires)
                     (eval/eval env/*clob-environment-init*)
                     (try
                       (load-init-file (.getCanonicalPath (jio/file (System/getProperty "user.home") ".clobrc")))
                       (catch Exception e
                         (binding [*out* *err*]
                           (println "Error while loading init file ~/.clobrc:\n" e)))))
             :print repl-print
             :read (create-repl-read)
             :eval (fn [form]
                     (try
                       (register-sigint-handler)
                       (eval/eval form)
                       ;; Catching ThreadDeath caused by Thread.stop() method from SIGINT handler
                       (catch ThreadDeath _
                         (throw (InterruptedException.)))))}
            (merge opts {:prompt (fn [])})
            seq
            flatten))))))

(defn -main [& args]
  (if (= args '("--version"))
    (prn {:clob (clob.core/clob-version)
          :clojure (clojure-version)})
    (with-redefs [clojure-main/load-script main/load-script
                  clojure-main/eval-opt main/eval-opt
                  clojure-main/repl-opt repl
                  clojure-main/help-opt main/help-opt
                  clojure.core/load-reader main/load-reader]
      (apply clojure-main/main args))))
