(ns clob.completion-test
  (:require [clojure.test :refer [deftest is are]]
            [clob.service.completion :refer [append-completion]]))

(deftest test-append-commpletion
  (is (= "ls abc" (append-completion "ls " "abc")))
  (is (= "ls abc" (append-completion "ls abc" "abc")))
  (is (= "ls " (append-completion "ls " "")))
  (is (= "ls Abc" (append-completion "ls ab" "Abc")))
  (is (= "ls abc" (append-completion "ls A" "abc"))))
