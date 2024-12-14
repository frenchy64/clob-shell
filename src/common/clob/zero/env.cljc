(ns clob.zero.env)

(def success (Object.))
(def failure (Object.))

(def ^:dynamic *clob-aliases* (atom {}))
(def ^:dynamic *clob-abbreviations* (atom {}))
(def ^:dynamic *clob-commands* (atom {}))

(def ^:dynamic *clob-environment-requires*
  '(require '[clob.zero.platform.process]
            '[clob.zero.reader]
            '[clob.zero.compiler]
            '[clob.zero.parser]
            '[clob.zero.core :refer [shx expand #?@(:clj [*clob-version* clob-version])]]
            '[clob.zero.builtin :refer [cd exit quit getenv setenv unsetenv]]
            '[clob.zero.platform.process]
            '[clob.zero.pipeline]
            '[clojure.string :as str]
            '[clob.zero.macros :refer [sh sh-str sh-code sh-ok sh-seq sh-lines sh-value defalias defabbr defcmd]]
            '[clob.zero.util :refer [source-shell]]))

(def ^:dynamic *clob-environment-init*
  '(do
     (def ^:dynamic clob-prompt (fn [] "$ "))

     (def ^:dynamic clob-title (fn [] (str "clob " (clob.zero.platform.process/cwd))))

     nil))
