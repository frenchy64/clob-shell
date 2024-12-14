(ns clob.frontend.plain
  (:gen-class)
  (:require [clob.platform.eval :as eval]
            [clob.compiler]
            [clob.parser]
            [clob.pipeline]
            [clob.reader]))

(defn -main [& args]
  (let [cmd (or (first args) "echo hello clojure")]
    (eval/eval
     `(-> ~(clob.compiler/compile-interactive
            (clob.parser/parse (clob.reader/read-string cmd)))
          (clob.pipeline/wait-for-pipeline)))))
