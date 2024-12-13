(ns clob.test-util.runner
  (:require [clojure.test :refer [run-tests]]
            [clob.compiler-test]
            [clob.process-test]
            [clob.common-test]
            [clob.pipeline-test]
            [clob.completion-test]
            [clob.core-test]
            [clob.reader-test]
            [clob.util-test]
            [clob.history-test]
            [clob.scripting-test]))

(def report-orig clojure.test/report)

(defn report-custom [& args]
  ; (clojure.test/with-test-out (println args))
  (apply report-orig args))

(defn -main []
  (binding [clojure.test/report report-custom]
    (time
     (run-tests
      'clob.reader-test
      'clob.compiler-test
      'clob.process-test
      'clob.common-test
      'clob.pipeline-test
      'clob.completion-test
      'clob.core-test
      'clob.util-test
      'clob.history-test
      'clob.scripting-test))))
