(ns clob.frontend.sci
  (:gen-class)
  (:require
   ; [clob.compiler]
   ; [clob.parser :as parser]
   ; [clob.pipeline]
   ; [clob.platform.eval :as eval]
   ; [clob.platform.process :as process]
   ; [clob.env :as env]
   ; [clob.reader :as reader]
   [clob.core :as clob.core]
   [clob.utils.clojure-main-sci :refer [main]]))

#_(defn repl-print
    [result]
    (when-not (or (nil? result)
                  (identical? result env/success)
                  (process/process? result))
      (if (or (string? result)
              (char? result))
        (print result)
        (pr result))
      (flush)))

#_(defn -main [& args]
    (reset! process/*cwd* (System/getProperty "user.dir"))
    (let [cmd (or (first args) "echo hello clojure")]
      (repl-print
       (eval/eval
        `(-> ~(clob.compiler/compile-interactive
               (clob.parser/parse
                (reader/read-string cmd)
                #_(edamame/parse-string-all cmd {:all true})))
             (clob.pipeline/wait-for-pipeline))))))

(defn -main [& args]
  (if (= args '("--version"))
    (prn {:clob (clob.core/clob-version)
          :clojure (clojure-version)})
    (apply main args)))
