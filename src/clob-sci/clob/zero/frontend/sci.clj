(ns clob.zero.frontend.sci
  (:gen-class)
  (:require
   ; [clob.zero.compiler]
   ; [clob.zero.parser :as parser]
   ; [clob.zero.pipeline]
   ; [clob.zero.platform.eval :as eval]
   ; [clob.zero.platform.process :as process]
   ; [clob.zero.env :as env]
   ; [clob.zero.reader :as reader]
   [clob.zero.core :as clob.core]
   [clob.zero.utils.clojure-main-sci :refer [main]]))

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
        `(-> ~(clob.zero.compiler/compile-interactive
               (clob.zero.parser/parse
                (reader/read-string cmd)
                #_(edamame/parse-string-all cmd {:all true})))
             (clob.zero.pipeline/wait-for-pipeline))))))

(defn -main [& args]
  (if (= args '("--version"))
    (prn {:clob (clob.core/clob-version)
          :clojure (clojure-version)})
    (apply main args)))
