(ns prep
  (:refer-clojure :exclude [compile])
  (:require [clojure.tools.build.api :as b]))

(def class-dir "target/classes")
(def basis (delay (b/create-basis {:project "deps.edn"})))

(defn clean [_]
  (b/delete {:path "target"}))

(defn compile [_]
  (b/compile-clj {:ns-compile ['clob.rebel-readline.line-reader-class]
                  :basis @basis
                  :class-dir class-dir}))
