(ns clob.test-util.spawn-helper
  (:require [clojure.string]
            [clob.zero.parser]
            [clob.zero.compiler]
            [clob.zero.builtin]
            [clob.zero.env]
            #?(:clj [clob.zero.reader :as reader])
            #?(:clj [clob.zero.platform.eval :as eval])
            #?(:cljs [clob.zero.platform.eval :refer [execute-command-text]])
            #?(:cljs [clob.zero.core])
            [clob.zero.platform.process :as process]))

(defn -main [cmd]
  #?(:cljs (clob.zero.platform.eval/execute-text
            (str (pr-str clob.zero.env/*clob-environment-requires*)))
     :clj (eval/eval-clob-requires))
  (let [result #?(:cljs (execute-command-text cmd)
                  :clj (eval/eval (reader/read-sh (reader/string-reader cmd))))]
    (cond
      (process/process? result)
      (process/exit (process/exit-code result))

      (and (seq? result)
           (every? #(process/process? %) result))
      (process/exit (process/exit-code (last result)))

      :else
      (print (str result)))))
