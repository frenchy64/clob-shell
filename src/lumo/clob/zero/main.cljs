(ns clob.zero.main
  (:require [lumo.repl]
            [clob.zero.parser]
            [clob.zero.compiler]
            [clob.zero.pipeline]
            [clob.zero.platform.io]
            [clob.zero.platform.util]
            [clob.zero.platform.process :as process]
            [clob.zero.env]
            [clob.zero.builtin]
            [clob.zero.util]
            [clob.zero.platform.eval :refer [execute-text]]
            [clob.zero.macros :refer-macros [sh sh-str sh-code sh-ok sh-seq sh-lines sh-value defalias defabbr defcmd]]
            [clob.zero.frontend.node-readline]
            [fs]
            [os]
            [path]))

(enable-console-print!)

(defn load-init-file
  "Loads init file."
  [init-path]
  (when (try (-> (fs/statSync init-path)
                 (.isFile))
             (catch :default _))
    (try (lumo.repl/execute-path init-path {})
         (catch :default e
           (js/console.error "Error while loading " init-path ":\n" e)))))

(defn -main
  "Starts clob REPL with prompt and readline."
  []
  (doto js/process
    ; ignore SIGQUIT like Bash
    (.on "SIGQUIT" (fn []))
    ; ignore SIGINT when not running a command (when running a command it already interupts execution with exception)
    (.on "SIGINT" (fn [])))
  (clob.zero.platform.eval/execute-text
   (str (pr-str clob.zero.env/*clob-environment-requires*)
        (pr-str clob.zero.env/*clob-environment-init*)))
  (load-init-file (path/join (os/homedir) ".clobrc"))
  (clob.zero.frontend.node-readline/-main))
