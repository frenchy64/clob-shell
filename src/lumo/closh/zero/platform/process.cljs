(ns clob.zero.platform.process
  (:require [clob.zero.platform.util :refer [wait-for-event jsx->clj]]
            [clob.zero.platform.io :refer [open-io-streams]]
            [goog.object :as gobj]
            [child_process]))

(defn process? [proc]
  (instance? child_process/ChildProcess proc))

(defn exit-code [proc]
  (.-exitCode proc))

(defn wait
  "Wait untils process exits and all of its stdio streams are closed."
  [proc]
  (when (and (process? proc)
             (nil? (exit-code proc)))
    (wait-for-event proc "close"))
  proc)

(defn exit [code]
  (js/process.exit code))

(defn cwd []
  (js/process.cwd))

(defn chdir [dir]
  (js/process.chdir dir))

(defn shx
  "Executes a command as child process."
  ([cmd] (shx cmd []))
  ([cmd args] (shx cmd args {}))
  ([cmd args opts]
   (child_process/spawn
    cmd
    (apply array (flatten args))
    #js{:stdio (open-io-streams (:redir opts))})))

(defn setenv [k v]
  (do (gobj/set js/process.env k v)
      v))

(defn unsetenv [k]
  (js-delete js/process.env k))

(defn getenv
  ([] (jsx->clj js/process.env))
  ([k] (gobj/get js/process.env k)))
