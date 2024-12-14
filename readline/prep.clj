(ns prep
  (:refer-clojure :exclude [compile])
  (:require [clojure.tools.build.api :as b]))

(def class-dir "classes")
(def basis (delay (b/create-basis {:project "deps.edn"})))

(defn compile [_]
  (b/compile-clj {:src-dirs ["src"]
                  :ns-compile '[clob.readline.line-reader-class]
                  :class-dir class-dir
                  :basis @basis}))
