(ns clob.util-test
  (:require [clojure.test :refer [deftest is]]
            [clob.zero.platform.process :refer [getenv setenv unsetenv shx]]
            [clob.zero.pipeline :refer [pipeline-value]]
            [clob.zero.util :refer [source-shell]]))

(deftest test-source-shell

  (is (= nil (source-shell "export A=42")))
  (is (= "42" (getenv "A")))

  (is (= "84" (do (source-shell "A=84")
                  (getenv "A"))))

  (is (= nil (do (source-shell "unset A")
                 (getenv "A"))))

  (is (= "forty two" (do (source-shell "export A='forty two'")
                         (getenv "A"))))

  (is (= "forty\ntwo" (do (source-shell "export A='forty\ntwo'")
                          (getenv "A"))))

  (is (= "hi" (do (setenv "B" "hi")
                  (getenv "B"))))

  (is (= "hello\n" (do (setenv "B" "hello")
                       (pipeline-value (shx "bash" ["-c" "echo $B"])))))

  (is (= nil (do (setenv "B" "hi")
                 (unsetenv "B")
                 (getenv "B")))))
