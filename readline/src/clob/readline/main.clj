(ns clob.readline.main
  (:require
   [clob.readline.clojure.main :as main]))

(defn -main [& args]
  (apply main/-main args))
