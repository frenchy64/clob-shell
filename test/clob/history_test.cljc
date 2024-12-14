(ns clob.history-test
  (:require [clojure.test :refer [deftest is testing]]
            [clob.test-util.util :refer [with-tempfile]]
            [clob.macros :refer [chain->]]
            [clob.frontend.jline-history :as jhistory]))

(defn iter->seq [iter]
  (loop [coll []]
    (if (.hasPrevious iter)
      (recur (conj coll (.previous iter)))
      coll)))

(defn history->seq [h]
  (->>
    (iter->seq (.iterator h (.index h)))
    (map #(.line %))
    (into [])))

(defn create-history [db-file]
  (jhistory/sqlite-history db-file))

(defn add-history [h command]
  (.add h command))

(defn assert-history [expected h]
  (is (= expected (history->seq h))))

(defmacro with-async [form] form)

(deftest history-multi-sessions
  (testing "First get history from current session, then from other sessions"
    (with-tempfile
      (fn [db-file]
        (let [s1 (create-history db-file)
              s2 (create-history db-file)]
          (with-async
            (chain->
             (add-history s1 "a")
             (fn [_] (add-history s2 "b"))
             (fn [_] (add-history s1 "c"))
             (fn [_] (.moveToEnd s2))
             (fn [_] (assert-history ["c" "a" "b"] s1))
             (fn [_] (assert-history ["b" "c" "a"] s2)))))))))

(deftest history-leading-whitespace
  (testing "Do do not add when line is starting with whitespace"
    (with-tempfile
      (fn [db-file]
        (let [s1 (create-history db-file)]
          (with-async
            (chain->
             (add-history s1 "a")
             (fn [_] (add-history s1 " b"))
             (fn [_] (assert-history ["a"] s1)))))))))

(deftest history-dont-add-empty
  (testing "Do do not add empty lines"
    (with-tempfile
      (fn [db-file]
        (let [s1 (create-history db-file)]
          (with-async
            (chain->
             (add-history s1 "a")
             (fn [_] (add-history s1 "  "))
             (fn [_] (add-history s1 ""))
             (fn [_] (assert-history ["a"] s1)))))))))

(deftest history-trim-whitespace
  (testing "Trim whitespace"
    (with-tempfile
      (fn [db-file]
        (let [s1 (create-history db-file)]
          (with-async
            (chain->
             (add-history s1 "a b \n")
             (fn [_] (assert-history ["a b"] s1)))))))))

(deftest history-no-duplicates
  (testing "No duplicates are returned"
    (with-tempfile
      (fn [db-file]
        (let [s1 (create-history db-file)]
          (with-async
            (chain->
             (add-history s1 "a")
             (fn [_] (add-history s1 "b"))
             (fn [_] (add-history s1 "a"))
             (fn [_] (assert-history ["a" "b"] s1)))))))))
