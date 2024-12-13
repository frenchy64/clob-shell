(ns clob.zero.platform.eval
  (:require [lumo.repl]
            [cljs.tools.reader]
            [clob.zero.reader]
            [clob.zero.env]
            [goog.object :as gobj]))

;; Make lumo's print a noop since we process resulting value ourselves
(gobj/set js/$$LUMO_GLOBALS "doPrint" identity)

(def handle-error-orig lumo.repl/handle-error)

(defn handle-error [error stacktrace?]
  (if (and (ex-cause error) (= (.-message (ex-cause error)) "Script execution interrupted."))
    (js/console.error " Interrupted")
    (handle-error-orig error stacktrace?)))

(defn execute-text
  "Evals a string via lumo."
  [source]
  ;; Execute does not return value but binds it to *1
  (lumo.repl/execute-text source {:expression? true})
  *1)

(defn execute-command-text
  "Evals a string in command mode via lumo."
  ([source]
   (execute-command-text source clob.zero.reader/read-sh))
  ([source reader-fn]
   ;; Execute does not return value but binds it to *1
   (with-redefs [cljs.tools.reader/read reader-fn
                 lumo.repl/handle-error handle-error]
     (lumo.repl/execute-text source {:expression? true}))
   *1))
