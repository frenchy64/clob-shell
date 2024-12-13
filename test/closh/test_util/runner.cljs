(ns clob.test-util.runner
  (:require [goog.object :as gobj]
            [clojure.test :refer [report run-tests]]
            [clob.compiler-test]
            [clob.process-test]
            [clob.common-test]
            [clob.pipeline-test]
            [clob.completion-test]
            [clob.core-test]
            [clob.reader-test]
            [clob.util-test]
            [clob.history-test]))

(defmethod report [:cljs.test/default :end-run-tests] [m]
  (if (cljs.test/successful? m)
    (gobj/set js/process "exitCode" 0)
    (gobj/set js/process "exitCode" 1)))

(defn -main []
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
    'clob.history-test)))
