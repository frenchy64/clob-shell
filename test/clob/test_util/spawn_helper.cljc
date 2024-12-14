(ns clob.test-util.spawn-helper
  (:require [clojure.string]
            [clob.zero.parser]
            [clob.zero.compiler]
            [clob.zero.builtin]
            [clob.zero.env]
            [clob.zero.reader :as reader]
            [clob.zero.platform.eval :as eval]
            [clob.zero.platform.process :as process]))

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
