(ns clob.zero.service.history-common
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clob.zero.platform.process :as process])
  #?(:clj (:import (java.io File))))

(def ^:no-doc table-history
  "CREATE TABLE IF NOT EXISTS history (
 id INTEGER PRIMARY KEY,
 session_id INTEGER NOT NULL,
 time INTEGER NOT NULL,
 command TEXT NOT NULL,
 cwd TEXT NOT NULL
);")

(def ^:no-doc table-session
  "CREATE TABLE IF NOT EXISTS session (
 id INTEGER PRIMARY KEY,
 time INTEGER NOT NULL
);")

(defn get-db-filename
  "Return path to the db file, defaults to ~/.clob/clob.sqlite"
  []
  (let [parts [(process/getenv "HOME") ".clob" "clob.sqlite"]
        f ^File (apply io/file parts)]
    (.getCanonicalPath f)))

(defn check-history-line [s]
  (when (and (not (str/blank? s))
             (not (re-find #"^\s+" s)))
    (str/trim s)))
