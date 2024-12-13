(ns clob.zero.platform.eval
  (:refer-clojure :exclude [eval]))

(defmacro def-eval []
  (if (System/getenv "__CLOB_USE_SCI_EVAL__")
    `(do (require 'clob.zero.utils.sci)
         (def ~'eval clob.zero.utils.sci/sci-eval))
    `(def ~'eval clojure.core/eval)))

(def-eval)

(defmacro eval-clob-requires []
  (when-not (System/getenv "__CLOB_USE_SCI_EVAL__")
    `(eval clob.zero.env/*clob-environment-requires*)))
