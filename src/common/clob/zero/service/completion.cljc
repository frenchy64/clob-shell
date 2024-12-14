(ns clob.zero.service.completion
  (:require [clojure.string]
            #?(:cljs [lumo.repl])
            #?(:clj [clojure.java.io :as io])
            #?(:clj [clob.zero.pipeline :as pipeline])
            [clob.zero.builtin :refer [getenv]]
            [clob.zero.platform.io :refer [out-stream]]
            [clob.zero.platform.process :refer [shx]]
            [clob.zero.macros #?(:clj :refer :cljs :refer-macros) [chain->]]))

#?(:cljs
   (defn- stream-output
     "Helper function to get output from a node stream as a string."
     [stream]
     (js/Promise.
      (fn [resolve _]
        (let [out (clob.zero.platform.io/stream-output stream)]
          (doto stream
            (.on "end" #(resolve @out))
            (.on "error" #(resolve ""))))))))

(defn escape-fish-string [s]
  (-> s
      (clojure.string/replace #"\\" "\\\\\\\\")
      (clojure.string/replace #"'" "\\\\'")
      (#(str "'" % "'"))))

(defn get-completions-spawn
  "Get completions by spawning a command."
  [shell cmd args]
  (let [proc #?(:cljs (shx (str (getenv "CLOB_SOURCES_PATH") "/resources/" cmd) args)
                :clj (if (= shell "fish")
                       (shx "fish" ["-c"
                                    (str "complete --do-complete=" (escape-fish-string (first args)))])
                       (if-let [resource (io/resource cmd)]
                         (pipeline/pipe (slurp resource)
                                        (shx shell (cons "-s" args)))
                         (shx (str (getenv "CLOB_SOURCES_PATH") "/resources/" cmd) args))))
        stream (out-stream proc)]
    (chain->
     #?(:cljs (stream-output stream)
        :clj @(clob.zero.platform.io/stream-output stream))
     (fn [stdout]
       (if (clojure.string/blank? stdout)
         []
         (clojure.string/split (clojure.string/trim stdout) #"\n"))))))

(defn complete-fish
  "Get completions from a fish shell. Spawns a process."
  [line]
  (try
    (chain-> (get-completions-spawn "fish" "completion/completion.fish" [line])
             (fn [completions] (map #(first (clojure.string/split % #"\t")) completions))) ; discard the tab-separated description
    (catch #?(:clj Exception :cljs :default) _
      nil)))

(defn complete-bash
  "Get completions from bash. Spawns a process."
  [line]
  (try
    (get-completions-spawn "bash" "completion/completion.bash" [line])
    (catch #?(:clj Exception :cljs :default) _
      nil)))

(defn complete-zsh
  "Get completions from zsh. Spawns a process."
  [line]
  (try
    (get-completions-spawn "zsh" "completion/completion.zsh" [line])
    (catch #?(:clj Exception :cljs :default) _
      nil)))

#?(:cljs
   (defn complete-lumo
     "Get completions from Lumo."
     [line]
     (js/Promise.
      (fn [resolve reject]
        (try
          (lumo.repl/get-completions line resolve)
          (catch :default e (reject e)))))))

(defn sanitize-completion [s]
  (-> s
      (clojure.string/trim)
      (clojure.string/replace #"\\ " " ")))

(defn append-completion
  "Appends completion to a line, discards the common part from in between."
  [line completion]
  (let [line-lower (clojure.string/lower-case line)
        completion-lower (clojure.string/lower-case completion)]
    (loop [i (count completion-lower)]
      (if (zero? i)
        (str line completion)
        (let [sub (subs completion-lower 0 i)]
          (if (clojure.string/ends-with? line-lower sub)
            (str (subs line 0 (- (count line) i)) completion)
            (recur (dec i))))))))

(defn process-completions
  "Processes completions for a given line, cleans up results by removing duplicates."
  [line completions]
  (->> completions
       (map #(append-completion line %))
       (filter #(not= line %))
       (distinct))) ; bash seems to return duplicates sometimes

(defn complete-shell [line]
  (chain-> (complete-fish line)
           #(if (seq %) % (complete-zsh line))
           #(if (seq %) % (complete-bash line))
           #(map sanitize-completion %)))

#?(:cljs
   (defn complete
     "Gets completions for a given line. Delegates to existing shells and Lumo. Callback style compatible with node's builtin readline completer function."
     [line cb]
     (->
      (chain-> (js/Promise.all
                #js[(when (re-find #"\([^)]*$" line) ; only send exprs with unmatched paren to lumo
                      (complete-lumo line))
                    (complete-shell line)])
               (fn [completions]
                 (->> completions
                      (map #(process-completions line %))
                      (interpose [""])
                      (apply concat)
                      (apply array)))
               #(cb nil #js[% line]))
      (.catch #(cb %)))))
