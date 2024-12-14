(ns clob.core
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clob.platform.io :refer [glob *stderr*]]
            [clob.platform.process :as process]
            [clob.pipeline :refer [process-value]]
            [clob.env :refer [*clob-aliases* *clob-abbreviations*]]))

(set! *warn-on-reflection* true)

(def command-not-found-bin "/usr/lib/command-not-found")

(defn expand-variable
  "Expands env variable, it does not look inside string."
  [s]
  (if (re-find #"^\$" s)
    (process/getenv (subs s 1))
    s))

(defn expand-tilde
  "Expands tilde character to a path to user's home directory."
  [s]
  (str/replace-first s #"^~" (process/getenv "HOME")))

(defn expand-filename
  "Expands filename based on globbing patterns"
  [s]
  (glob s (process/cwd)))

(defn expand-redirect
  "Expand redirect targets. It does tilde and variable expansion."
  [s]
  (-> s expand-tilde expand-variable))

; Bash: Partial quote (allows variable and command expansion)
(defn expand-partial
  "Partially expands parameter which is used when parameter is quoted as string. It only does variable expansion."
  [s]
  (if-let [result (expand-variable s)]
    (list result)
    (list)))

; Bash: The order of expansions is: brace expansion; tilde expansion, parameter and variable expansion, arithmetic expansion, and command substitution (done in a left-to-right fashion); word splitting; and filename expansion.
(defn expand
  "Expands command-line parameter.

  The order of expansions is variable expansion, tilde expansion and filename expansion."
  [s]
  (if-let [x (expand-variable s)]
    (-> x expand-tilde expand-filename)
    (list)))

(defn expand-command
  "Expands first command token."
  [s]
  (first (expand s)))

(defn get-command-suggestion
  "Get suggestion for a missing command using command-not-found utility."
  [cmdname]
  (try (-> (process/shx command-not-found-bin ["--no-failure-msg" cmdname])
           process-value
           :stderr)
       (catch Exception _)))

(defn shx
  "Executes a command as child process."
  ([cmd] (shx cmd []))
  ([cmd args] (shx cmd args {}))
  ([cmd args opts]
   (try (process/shx cmd args opts)
        (catch java.io.IOException _e
          (let [suggestion (get-command-suggestion cmd)]
            (when-not (str/blank? suggestion)
              (.print ^java.io.PrintStream *stderr* suggestion))
            (.println ^java.io.PrintStream *stderr* (str cmd ": command not found"))
            #_(println "STACKTRACE:")
            #_(.printStackTrace e)))
        (catch Exception ex
          (.println ^java.io.PrintStream *stderr* (str "Unexpected error:\n" ex))))))

(defn expand-alias
  ([input] (expand-alias @*clob-aliases* input))
  ([aliases input]
   (let [token (re-find #"[^\s]+" input)
         alias (get aliases token)]
     (cond-> input 
       alias (str/replace-first #"[^\s]+" alias)))))

(defn expand-abbreviation
  ([input] (expand-abbreviation @*clob-abbreviations* input))
  ([aliases input]
   (let [token (re-find #"[^\s]+" input)
         alias (get aliases token)]
     (cond-> input
       (and alias (= (str/trim input) token))
       (str/replace-first #"[^\s]+" alias)))))

;; Based on code from clojure.core
(def -clob-version
  (delay
    (let [version-string (str/trim (slurp (io/resource "CLOB_VERSION")))
          [_ major minor incremental qualifier snapshot]
          (re-matches
            #"(\d+)\.(\d+)\.(\d+)(?:-([a-zA-Z0-9_]+))?(?:-(SNAPSHOT))?"
            version-string)
          version {:major       (Integer/valueOf ^String major)
                   :minor       (Integer/valueOf ^String minor)
                   :incremental (Integer/valueOf ^String incremental)
                   :qualifier   (when (not= qualifier "SNAPSHOT") qualifier)}]
      (cond-> version
        (.contains version-string "SNAPSHOT")
        (assoc :interim true)))))

;; Based on clojure.core/clojure-version
(defn clob-version
  "Returns clob version as a printable string."
  {:added "1.0"}
  []
  (let [{:keys [major minor incremental qualifier interim]} @-clob-version]
    (str major "." minor
         (some->> incremental (str "."))
         (when (pos? (count qualifier)) (str "-" qualifier))
         (when interim "-SNAPSHOT"))))
