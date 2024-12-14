(ns clob.frontend.main
  (:gen-class)
  (:require [clojure.tools.reader.reader-types :refer [read-char unread]]
            [clojure.tools.reader.impl.utils]
            [clob.reader :refer [read-sh]]
            [clob.core :as core]
            [clob.platform.process :refer [process?]]
            [clob.env :refer [*clob-environment-requires* *clob-environment-init*] :as env]
            [clob.platform.eval :as eval]
            [clob.utils.clojure-main :refer [repl repl-requires with-read-known] :as clojure-main])
  (:refer-clojure :exclude [load-reader])
  (:import [clojure.lang Compiler RT LineNumberingPushbackReader]
           [java.io File FileInputStream InputStreamReader StringReader PipedWriter PipedReader PushbackReader BufferedReader]))

(def custom-environment
  (str "(do "
       "(alter-var-root #'load-file (constantly clob.frontend.main/compiler-load-file))"
       "(def ^{:dynamic true} *args* *command-line-args*)"
       " nil)"))

(defn skip-whitespace [reader]
  (loop []
    (let [ch (read-char reader)]
      (if (clojure.tools.reader.impl.utils/whitespace? ch)
        (recur)
        (if (nil? ch)
          false
          (do (unread reader ch)
              true))))))

(defn generate-padding [lines columns]
  (apply str (concat (repeat lines "\n")
                     (repeat (dec columns) " "))))

(defn make-custom-reader [rdr]
  (let [eof (Object.)
        opts {:eof eof :read-cond :allow :features #{:clj}}
        rdr (LineNumberingPushbackReader. rdr)
        writer (PipedWriter.)
        reader (PipedReader. writer)]
    (doseq [c (str
               custom-environment
               (prn-str *clob-environment-requires*))]
      (.write writer (int c)))
    (let [custom-reader
          (proxy [LineNumberingPushbackReader] [reader]
            (read []
              (let [^LineNumberingPushbackReader this this]
                (when-not (proxy-super ready)
                  (when (skip-whitespace rdr)
                    (proxy-super setLineNumber (dec (.getLineNumber rdr)))
                    (doseq [c (str "\n" (apply str (repeat (dec (.getColumnNumber rdr)) " ")))]
                      (.write writer (int c))))
                  (let [form (clob.reader/read opts rdr)]
                    (if (= form eof)
                      (.close writer)
                      (binding [*print-meta* true]
                        (doseq [c (pr-str (conj form 'clob.macros/sh-wrapper))]
                          (.write writer (int c)))))))
                (let [c (proxy-super read)]
                  c))))]
      (.setLineNumber custom-reader 0)
      custom-reader)))

(defn line-reader [f rdr]
  (let [rdr (BufferedReader. rdr)
        writer (PipedWriter.)
        reader (PipedReader. writer)]
    (proxy [PushbackReader] [reader]
      (read []
        (let [^PushbackReader this this]
          (when-not (proxy-super ready)
            (if-let [line (.readLine rdr)]
              (doseq [c (f (str line \newline))]
                (.write writer (int c)))
              (.close writer)))
          (proxy-super read))))))

(defn repl-read
  [request-prompt request-exit]
  (read-sh {:read-cond :allow} (line-reader core/expand-alias *in*)))

(defn repl-print
  [& args]
  (when-not (or (nil? (first args))
                (identical? (first args) env/success)
                (process? (first args)))
    (apply prn args)))

(defn repl-opt
  [[_ & args] inits]
  (repl :init (fn []
                (clojure-main/initialize args inits)
                (apply require repl-requires)
                (eval/eval-clob-requires)
                (eval/eval *clob-environment-init*))
        :read repl-read
        :print repl-print)
  (prn)
  (System/exit 0))

;; Reimplementation of Compiler.loadFile
(defn compiler-load-file [^String file]
  (let [f (FileInputStream. file)
        ;; rdr (InputStreamReader. f RT/UTF8)
        rdr (make-custom-reader (PushbackReader. (InputStreamReader. f RT/UTF8)))]
    (try
      (Compiler/load
       rdr
       (.getAbsolutePath (File. file))
       (.getName (File. file)))
      (finally
        (.close f)))))

;; clojure.main/load-script
(defn load-script
  "Loads Clojure source from a file or resource given its path. Paths
  beginning with @ or @/ are considered relative to classpath."
  [^String path]
  (if (.startsWith path "@")
    (RT/loadResourceScript
     (.substring path (if (.startsWith path "@/") 2 1)))
    ;; (Compiler/loadFile path)))
    (compiler-load-file path)))

;; clojure.core/load-reader
(defn load-reader
  "Sequentially read and evaluate the set of forms contained in the
  stream/file"
  {:added "1.0"
   :static true}
  [rdr]
  (let [clob-reader (make-custom-reader rdr)]
    ;; (. clojure.lang.Compiler (load rdr)))
    (Compiler/load clob-reader)))

(defn eval-opt
  "Evals expressions in str, prints each non-nil result using prn"
  [str]
  (let [eof (Object.)
        reader (make-custom-reader (StringReader. str))]
    (loop [input (with-read-known (read reader false eof))]
      (when-not (= input eof)
        (let [value (eval/eval input)]
          (when-not (nil? value)
            (prn value))
          (recur (with-read-known (read reader false eof))))))))

(defn help-opt
  "Print help text for main"
  [_ _]
  (println (-> (:doc (meta (var clojure-main/main)))
               (clojure.string/replace #"java -cp clojure\.jar clojure\.main" "clob.jar"))))

(defn -main [& args]
  (with-redefs [clojure-main/load-script load-script
                clojure-main/eval-opt eval-opt
                clojure-main/repl-opt repl-opt
                clojure-main/help-opt help-opt
                clojure.core/load-reader load-reader]
                ;; redef does not seem to work, must use alter var root
                ;; clojure.core/load-file compiler-load-file]
    (apply clojure-main/main args)))
