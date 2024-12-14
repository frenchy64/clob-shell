(ns clob.rebel-readline.main
  (:require
   [clob.rebel-readline.clojure.main :as main]))

(defn -main [& args]
  (apply main/-main args))
