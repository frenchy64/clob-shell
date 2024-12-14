(ns prep
  (:refer-clojure :exclude [compile])
  (:require [clojure.tools.build.api :as b]))

(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))

(defn compile [_]
  (b/compile-clj {:src-dirs ["src"]
                  :class-dir class-dir
                  :basis basis}))
