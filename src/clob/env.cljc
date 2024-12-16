(ns clob.env)

(def success (Object.))
(def failure (Object.))

(def ^:dynamic *clob-aliases* (atom {}))
(def ^:dynamic *clob-abbreviations* (atom {}))
(def ^:dynamic *clob-commands* (atom {}))

(def ^:dynamic *clob-environment-requires*
  '(require '[clob.platform.process]
            '[clob.reader]
            '[clob.compiler]
            '[clob.parser]
            '[clob.core :refer [shx expand -clob-version clob-version]]
            '[clob.builtin :refer [cd exit quit getenv setenv unsetenv]]
            '[clob.platform.process]
            '[clob.pipeline]
            '[clojure.string :as str]
            '[clob.macros :refer [sh sh-str sh-code sh-ok sh-seq sh-lines sh-value defalias defabbr defcmd]]
            '[clob.util :refer [source-shell]]
            '[babashka.deps :refer [add-deps]]))

(def ^:dynamic *clob-environment-init*
  '(do
     (def ^:dynamic clob-prompt (fn [] "$ "))

     (def ^:dynamic clob-title (fn [] (str "clob " (clob.platform.process/cwd))))

     nil))
