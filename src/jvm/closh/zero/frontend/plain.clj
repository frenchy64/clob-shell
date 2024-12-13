(ns clob.zero.frontend.plain
  (:gen-class)
  (:require [clob.zero.platform.eval :as eval]
            [clob.zero.compiler]
            [clob.zero.parser]
            [clob.zero.pipeline]
            [clob.zero.reader]))

(defn -main [& args]
  (let [cmd (or (first args) "echo hello clojure")]
    (eval/eval
     `(-> ~(clob.zero.compiler/compile-interactive
            (clob.zero.parser/parse (clob.zero.reader/read-string cmd)))
          (clob.zero.pipeline/wait-for-pipeline)))))
