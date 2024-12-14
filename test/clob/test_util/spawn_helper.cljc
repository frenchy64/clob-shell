(ns clob.test-util.spawn-helper
  (:require [clojure.string]
            [clob.parser]
            [clob.compiler]
            [clob.builtin]
            [clob.env]
            [clob.reader :as reader]
            [clob.platform.eval :as eval]
            [clob.platform.process :as process]))

(defn -main [cmd]
  (eval/eval-clob-requires)
  (let [result (eval/eval (reader/read-sh (reader/string-reader cmd)))]
    (cond
      (process/process? result)
      (process/exit (process/exit-code result))

      (and (seq? result)
           (every? #(process/process? %) result))
      (process/exit (process/exit-code (last result)))

      :else
      (print (str result)))))
