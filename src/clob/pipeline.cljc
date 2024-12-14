(ns clob.pipeline
  (:refer-clojure :exclude [line-seq])
  (:require [clojure.string :as str]
            [clob.platform.process :as process :refer [process?]]
            [clob.platform.io :refer [out-stream in-stream err-stream stream-output pipe-stream line-seq
                                      stream-write output-stream input-stream output-stream? input-stream? *stdout* *stderr*]]))

(set! *warn-on-reflection* true)

(defn wait-when-process [proc]
  (cond-> proc
    (process? proc) process/wait))

(defn wait-for-pipeline
  "Wait for a pipeline to complete. Standard outputs of a process are piped to stdout and stderr."
  [proc]
  (if (process? proc)
    (do (when-let [stdout (out-stream proc)]
          (pipe-stream stdout *stdout*))
        (when-let [stderr (err-stream proc)]
          (pipe-stream stderr *stderr*))
        (process/wait proc))
    proc))

(defn pipeline-condition
  "Get status of a finished pipeline. Returns true if a process exited with non code or a value is truthy."
  [proc]
  (if (process? proc)
    (zero? (process/exit-code proc))
    (boolean proc)))

(defn pipeline-value
  "Waits for a pipeline to finish and returns its output."
  [proc]
  (if (process? proc)
    (let [out (stream-output (out-stream proc))]
      (process/wait proc)
      @out)
    proc))

(defn process-output
  "Returns for a process to finish and returns output to be printed out."
  [proc]
  (if (seq? proc)
    (str (str/join "\n" proc) "\n")
    (pipeline-value proc)))

(defn process-value
  "Returns for a process to finish and returns map of exit code, stdout and stderr."
  [proc]
  (if (process? proc)
    (let [stdout (stream-output (out-stream proc))
          stderr (stream-output (err-stream proc))]
      (process/wait proc)
      {:stdout @stdout
       :stderr @stderr
       :code (process/exit-code proc)})
    {:stdout (if (instance? clojure.lang.LazySeq proc)
               (pr-str proc)
               (str proc))
     :stderr ""
     :code 0}))

; TODO: refactor dispatch to protocols or multimethods
(defn- pipe-internal
  "Pipes process or value to another process or function."
  [from to]
  (cond
    (input-stream? from) (cond
                           (process? to) (do (when-let [in (in-stream to)]
                                               (pipe-stream from in))
                                             to)
                           (output-stream? to) (pipe-stream from to)
                           :else (to (stream-output from)))
    (process? from) (cond
                      (process? to) (do (when-let [in (in-stream to)]
                                          (let [out (out-stream from)]
                                            (pipe-stream out in)))
                                        to)
                      (output-stream? to) (pipe-stream (out-stream from) to)
                      :else (to (process-output from)))
    (seq? from) (cond
                  (process? to) (do (pipe-internal from (in-stream to))
                                    to)
                  (output-stream? to) (let [val (str (str/join "\n" from) "\n")]
                                        (stream-write to val)
                                        to)
                  :else (to from))
    :else (cond
            (process? to) (do (pipe-internal from (in-stream to))
                              to)
            (output-stream? to) (do (stream-write to (str from))
                                    to)
            :else (to from))))

(defn pipe
  [x & xs]
  (reduce pipe-internal x xs))

(defn pipe-multi
  "Piping in multi mode. It splits streams and strings into seqs of strings by newline. Single value is wrapped in list. Then it is passed to `pipe`."
  [x f]
  (let [val (cond
              (process? x) (if-let [stream (out-stream x)]
                             (line-seq stream)
                             (list))
              (sequential? x) x
              (string? x) (str/split x #"\n")
              :else (list x))]
    (pipe val f)))

(defn pipe-map
  "Pipe by mapping a function."
  [proc f]
  (pipe-multi proc (partial map f)))

(defn pipe-filter
  "Pipe by filtering based on a function."
  [proc f]
  (pipe-multi proc (partial filter f)))

(defn redir [val redirects]
  (let [{stdout 1} (reduce (fn [redirects [op fd target]]
                             (case op
                               :rw (throw (Exception. "Read/Write redirection is not supported"))
                               (let [redirect (case op
                                                :in (input-stream target)
                                                :out (output-stream target)
                                                :append (output-stream target :append true)
                                                :set (cond-> target
                                                       (#{:stdin :stdout :stderr} target) redirects))]
                                 (cond-> redirects
                                   redirect (assoc fd redirect)))))
                           {0 :stdin 1 :stdout 2 :stderr}
                           redirects)]
    (case stdout
      :stdout val
      (do (pipe val stdout)
          nil))))
