(ns clob.service.completion
  (:require [clojure.string]
            [clojure.java.io :as io]
            [clob.pipeline :as pipeline]
            [clob.builtin :refer [getenv]]
            [clob.platform.io :refer [out-stream]]
            [clob.platform.process :refer [shx]]
            [clob.macros :refer [chain->]]))

(defn escape-fish-string [s]
  (-> s
      (clojure.string/replace #"\\" "\\\\\\\\")
      (clojure.string/replace #"'" "\\\\'")
      (#(str "'" % "'"))))

(defn get-completions-spawn
  "Get completions by spawning a command."
  [shell cmd args]
  (let [proc (if (= shell "fish")
               (shx "fish" ["-c"
                            (str "complete --do-complete=" (escape-fish-string (first args)))])
               (if-let [resource (io/resource cmd)]
                 (pipeline/pipe (slurp resource)
                                (shx shell (cons "-s" args)))
                 (shx (str (getenv "CLOB_SOURCES_PATH") "/resources/" cmd) args)))
        stream (out-stream proc)]
    (chain->
     @(clob.platform.io/stream-output stream)
     (fn [stdout]
       (if (clojure.string/blank? stdout)
         []
         (clojure.string/split (clojure.string/trim stdout) #"\n"))))))

(defn complete-fish
  "Get completions from a fish shell. Spawns a process."
  [line]
  (try
    (chain-> (get-completions-spawn "fish" "completion/completion.fish" [line])
             (fn [completions] (map #(first (clojure.string/split % #"\t")) completions))) ; discard the tab-separated description
    (catch Exception _)))

(defn complete-bash
  "Get completions from bash. Spawns a process."
  [line]
  (try
    (get-completions-spawn "bash" "completion/completion.bash" [line])
    (catch Exception _)))

(defn complete-zsh
  "Get completions from zsh. Spawns a process."
  [line]
  (try
    (get-completions-spawn "zsh" "completion/completion.zsh" [line])
    (catch Exception _)))

(defn sanitize-completion [s]
  (-> s
      (clojure.string/trim)
      (clojure.string/replace #"\\ " " ")))

(defn append-completion
  "Appends completion to a line, discards the common part from in between."
  [line completion]
  (let [line-lower (clojure.string/lower-case line)
        completion-lower (clojure.string/lower-case completion)]
    (loop [i (count completion-lower)]
      (if (zero? i)
        (str line completion)
        (let [sub (subs completion-lower 0 i)]
          (if (clojure.string/ends-with? line-lower sub)
            (str (subs line 0 (- (count line) i)) completion)
            (recur (dec i))))))))

(defn process-completions
  "Processes completions for a given line, cleans up results by removing duplicates."
  [line completions]
  (->> completions
       (map #(append-completion line %))
       (filter #(not= line %))
       (distinct))) ; bash seems to return duplicates sometimes

(defn complete-shell [line]
  (chain-> (complete-fish line)
           #(if (seq %) % (complete-zsh line))
           #(if (seq %) % (complete-bash line))
           #(map sanitize-completion %)))
