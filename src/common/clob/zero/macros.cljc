(ns clob.zero.macros
  (:require [clob.zero.parser]
            [clob.zero.compiler]))

(defmacro sh
  "Expands tokens in command mode to executable code."
  [& tokens]
  `(-> ~(clob.zero.compiler/compile-interactive (clob.zero.parser/parse tokens))
       (clob.zero.pipeline/wait-for-pipeline)))

(defmacro sh-value
  "Expands tokens in command mode to executable code."
  [& tokens]
  `(-> ~(clob.zero.compiler/compile-batch (clob.zero.parser/parse tokens))
       (clob.zero.pipeline/process-value)))

(defmacro sh-val
  "Expands command mode returning process output as output value."
  [& tokens]
  `(-> ~(clob.zero.compiler/compile-batch (clob.zero.parser/parse tokens))
       (clob.zero.pipeline/process-output)))

(defmacro sh-str
  "Expands command mode returning process output as string."
  [& tokens]
  `(-> ~(clob.zero.compiler/process-command-list
         (clob.zero.parser/parse tokens)
         clob.zero.compiler/process-pipeline-command-substitution)
       (clob.zero.pipeline/process-output)
       (str)
       (clojure.string/trim)))

(defmacro sh-seq
  "Expands command mode collecting process output returning it as a sequence of strings split by whitespace."
  [& tokens]
  `(-> ~(clob.zero.compiler/process-command-list
         (clob.zero.parser/parse tokens)
         clob.zero.compiler/process-pipeline-command-substitution)
       (clob.zero.pipeline/process-output)
       (clojure.string/trim)
       (clojure.string/split #"\s+")))

(defmacro sh-lines
  "Expands command mode collecting process output returning it as a sequence of lines."
  [& tokens]
  `(-> ~(clob.zero.compiler/process-command-list
         (clob.zero.parser/parse tokens)
         clob.zero.compiler/process-pipeline-command-substitution)
       (clob.zero.pipeline/process-output)
       (clojure.string/trim)
       (clojure.string/split #"\n")))

(defmacro sh-code
  "Expands command mode returning process exit code."
  [& tokens]
  `(-> ~(clob.zero.compiler/compile-interactive (clob.zero.parser/parse tokens))
       (clob.zero.platform.process/wait)
       (clob.zero.platform.process/exit-code)))

(defmacro sh-ok
  "Expands command mode returning true if process completed with non-zero exit code."
  [& tokens]
  `(-> ~(clob.zero.compiler/compile-interactive (clob.zero.parser/parse tokens))
       (clob.zero.platform.process/wait)
       (clob.zero.platform.process/exit-code)
       (zero?)))

(defmacro sh-wrapper
  "Like sh macro but if the result is a process then returns nil. This is useful for eval mode so that process objects are not printed out."
  [& tokens]
  `(let [result# (sh ~@tokens)]
     (when-not (clob.zero.platform.process/process? result#) result#)))

(defmacro defalias [name value]
  `(swap! clob.zero.env/*clob-aliases* assoc (str (quote ~name)) ~value))

(defmacro defabbr [name value]
  `(do (swap! clob.zero.env/*clob-abbreviations* assoc (str (quote ~name)) ~value)
       ;; Temporary workaround: Treat abbreviations as aliases in the JVM version until proper abbreviation expansion is implemented
       #?(:clj (defalias ~name ~value))))

(defmacro defcmd [name & body]
  (if (= 1 (count body))
    `(do (swap! clob.zero.env/*clob-commands* assoc (quote ~name) ~(first body))
         nil)
    `(do (defn ~name ~@body)
         (swap! clob.zero.env/*clob-commands* assoc (quote ~name) ~name)
         nil)))

(defmacro chain-> [x & forms]
  `(-> ~x ~@(for [form forms]
              #?(:clj (list form)
                 :cljs (list '.then form)))))

(comment
  (macroexpand-1 '(chain-> x (first) (second))))
