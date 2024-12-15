(ns clob.service.completion
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clob.pipeline :as pipeline]
            [clob.builtin :refer [getenv]]
            [clob.platform.io :refer [out-stream]]
            [clob.platform.process :refer [shx]]
            [clob.macros :refer [chain->]]))

(defn escape-fish-string [s]
  (-> s
      (str/replace #"\\" "\\\\\\\\")
      (str/replace #"'" "\\\\'")
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
       (if (str/blank? stdout)
         []
         (str/split (str/trim stdout) #"\n"))))))

(defn complete-fish
  "Get completions from a fish shell. Spawns a process."
  [line]
  (try
    (chain-> (get-completions-spawn "fish" "completion/completion.fish" [line])
             (fn [completions] (map #(first (str/split % #"\t")) completions))) ; discard the tab-separated description
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
      (str/trim)
      (str/replace #"\\ " " ")))

(defn append-completion
  "Appends completion to a line, discards the common part from in between."
  [line completion]
  (let [line-lower (str/lower-case line)
        completion-lower (str/lower-case completion)]
    (loop [i (count completion-lower)]
      (if (zero? i)
        (str line completion)
        (let [sub (subs completion-lower 0 i)]
          (if (str/ends-with? line-lower sub)
            (str (subs line 0 (- (count line) i)) completion)
            (recur (dec i))))))))

(defn process-completions
  "Processes completions for a given line, cleans up results by removing duplicates."
  [line completions]
  (->> completions
       (map #(append-completion line %))
       (remove #(= line %))
       (distinct))) ; bash seems to return duplicates sometimes

(defn complete-shell [line]
  (chain-> (complete-fish line)
           #(or (not-empty %) (complete-zsh line))
           #(or (not-empty %) (complete-bash line))
           #(map sanitize-completion %)))
