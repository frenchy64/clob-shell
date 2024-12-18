(ns clob.util
  (:require [clojure.data :refer [diff]]
            [clob.platform.process :refer [shx setenv getenv unsetenv]]
            [clob.pipeline :refer [process-value]]
            [clojure.data.json :as json]))

(def ignore-env-vars #{"_" "OLDPWD" "PWD" "SHELLOPTS" "SHLVL"})

(defn with-tempfile [cb]
  (let [file (java.io.File/createTempFile "clob-tmp-" ".txt")
        f (.getAbsolutePath file)
        result (cb f)]
    (.delete file)
    result))

(defn spawn-shell
  [shell exp]
  (process-value (shx shell ["-c" exp])))

(defn setenv-diff
  [before after]
  (let [var-diff (diff before after)
        removed (remove #(ignore-env-vars (first %)) (first var-diff))
        changed (remove #(ignore-env-vars (first %)) (second var-diff))]
    (doseq [[k _] removed] (unsetenv k))
    (doseq [[k v] changed] (setenv k v))))

(defn source-shell
  "Spawns a shell interpreter and executes `exp`. If it executes successfully,
  any exported variables are then saved into the clob environment"
  ([exp] (source-shell "bash" exp))
  ([shell exp]
   (with-tempfile
     (fn [temp-file]
       (let [before (getenv)
             result (spawn-shell shell (str exp "&& (node -p 'JSON.stringify(process.env)') >" temp-file))]
         (if (= (:code result) 0)
           (let [after (json/read-str (slurp temp-file))
                 stdout (:stdout result)]
             (setenv-diff before after)
             (when-not (= stdout "") stdout))
           (println "Error while executing" shell "command:" exp "\n" (:stderr result))))))))
