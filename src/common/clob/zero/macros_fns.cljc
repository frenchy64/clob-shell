(ns clob.macros-fns
  (:require [clob.parser]
            [clob.compiler]
            [clob.platform.process]))

(defn sh
  "Expands tokens in command mode to executable code."
  [& tokens]
  `(-> ~(clob.compiler/compile-interactive (clob.parser/parse tokens))
       (clob.pipeline/wait-for-pipeline)))

(defn sh-value
  "Expands tokens in command mode to executable code."
  [& tokens]
  `(-> ~(clob.compiler/compile-batch (clob.parser/parse tokens))
       (clob.pipeline/process-value)))

(defn sh-val
  "Expands command mode returning process output as output value."
  [& tokens]
  `(-> ~(clob.compiler/compile-batch (clob.parser/parse tokens))
       (clob.pipeline/process-output)))

(defn sh-str
  "Expands command mode returning process output as string."
  [& tokens]
  `(-> ~(clob.compiler/process-command-list
         (clob.parser/parse tokens)
         clob.compiler/process-pipeline-command-substitution)
       (clob.pipeline/process-output)
       (str)
       (clojure.string/trim)))

(defn sh-seq
  "Expands command mode collecting process output returning it as a sequence of strings split by whitespace."
  [& tokens]
  `(-> ~(clob.compiler/process-command-list
         (clob.parser/parse tokens)
         clob.compiler/process-pipeline-command-substitution)
       (clob.pipeline/process-output)
       (clojure.string/trim)
       (clojure.string/split #"\s+")))

(defn sh-lines
  "Expands command mode collecting process output returning it as a sequence of lines."
  [& tokens]
  `(-> ~(clob.compiler/process-command-list
         (clob.parser/parse tokens)
         clob.compiler/process-pipeline-command-substitution)
       (clob.pipeline/process-output)
       (clojure.string/trim)
       (clojure.string/split #"\n")))

(defn sh-code
  "Expands command mode returning process exit code."
  [& tokens]
  `(-> ~(clob.compiler/compile-interactive (clob.parser/parse tokens))
       (clob.platform.process/wait)
       (clob.platform.process/exit-code)))

(defn sh-ok
  "Expands command mode returning true if process completed with non exit code."
  [& tokens]
  `(-> ~(clob.compiler/compile-interactive (clob.parser/parse tokens))
       (clob.platform.process/wait)
       (clob.platform.process/exit-code)
       (zero?)))

(defn sh-wrapper
  "Like sh macro but if the result is a process then returns nil. This is useful for eval mode so that process objects are not printed out."
  [& tokens]
  `(let [result# (sh ~@tokens)]
     (when-not (clob.platform.process/process? result#) result#)))

(defn defalias [name value]
  `(swap! clob.env/*clob-aliases* assoc (str (quote ~name)) ~value))

(defn defabbr [name value]
  `(do (swap! clob.env/*clob-abbreviations* assoc (str (quote ~name)) ~value)
       ;; Temporary workaround: Treat abbreviations as aliases in the JVM version until proper abbreviation expansion is implemented
       (defalias ~name ~value)))

(defn defcmd [name & body]
  (if (= 1 (count body))
    `(do (swap! clob.env/*clob-commands* assoc (quote ~name) ~(first body))
         nil)
    `(do (defn ~name ~@body)
         (swap! clob.env/*clob-commands* assoc (quote ~name) ~name)
         nil)))

(defn chain-> [x & forms]
  `(-> ~x ~@(for [form forms]
              (list form))))

(comment
  (macroexpand-1 '(chain-> x (first) (second))))
