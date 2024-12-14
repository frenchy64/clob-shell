(ns clob.platform.eval
  (:refer-clojure :exclude [eval]))

(defmacro def-eval []
  (if (System/getenv "__CLOB_USE_SCI_EVAL__")
    `(do (require 'clob.utils.sci)
         (def ~'eval clob.utils.sci/sci-eval))
    `(def ~'eval clojure.core/eval)))

(def-eval)

(defmacro eval-clob-requires []
  (when-not (System/getenv "__CLOB_USE_SCI_EVAL__")
    `(eval clob.env/*clob-environment-requires*)))
