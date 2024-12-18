(ns clob.frontend.sci-rebel
  (:gen-class)
  (:require [clob.frontend.rebel :as rebel]
            [clob.core]
            [clob.utils.clojure-main-sci :as clojure-main]
            [clojure.string :as str]))

(defn help-opt
  "Print help text for main"
  [_ _]
  (println (-> (:doc (meta (var clojure-main/main)))
               (str/replace #"java -cp clojure\.jar clojure\.main" "clob-sci"))))

(defn -main [& args]
  (if (= args '("--version"))
    (prn {:clob (clob.core/clob-version)
          :clojure (clojure-version)})
    (with-redefs [clojure-main/repl-opt rebel/repl
                  clojure-main/help-opt help-opt]
      (apply clojure-main/main args))))
