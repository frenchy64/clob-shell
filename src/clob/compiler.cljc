(ns clob.compiler
  (:require [clob.env :refer [*clob-commands*]]
            [clob.core :as core]
            [clob.pipeline :as pipeline]))

(def ^:no-doc pipes
  "Maps shorthand symbols of pipe functions to full name"
  {'| `pipeline/pipe
   '|> `pipeline/pipe-multi
   '|? `pipeline/pipe-filter
   '|& `pipeline/pipe-reduce
   ;; '|>> `pipeline/pipe-thread-last
   ;; '|| `pipeline/pipe-mapcat
   ;; '|! `pipeline/pipe-foreach
   })

(defn ^:no-doc process-arg
  "Transform conformed argument."
  [arg]
  (cond
    ;; clojure form - use as is
    (or (boolean? arg) (number? arg) (seq? arg) (vector? arg)) [arg]
    ;; strings do limited expansion
    (string? arg) (list `core/expand-partial arg)
    ;; otherwise coerce to string and do full expansion
    :else (list `core/expand (str arg))))

(defn ^:no-doc process-redirect
  "Transform conformed redirection specification."
  [{:keys [op fd arg]}]
  (let [arg (if ((some-fn list? number? keyword?) arg)
              arg
              (list `core/expand-redirect (str arg)))]
    (case op
      > [[:out (or fd 1) arg]]
      < [[:in (or fd 0) arg]]
      >> [[:append (or fd 1) arg]]
      &> [[:out 1 arg]
          [:set 2 1]]
      &>> [[:append 1 arg]
           [:set 2 1]]
      <> [[:rw (or fd 0) arg]]
      >& [[:set (or fd 1) arg]])))

(defn ^:no-doc process-command
  "Transform conformed command specification."
  ([cmd] (process-command cmd []))
  ([[cmd & rest] redir]
   (let [args (cond->> rest
                (vector? (ffirst rest)) (apply concat))
         is-function (and (= (first cmd) :arg)
                          (list? (second cmd))
                          (not= 'cmd (first (second cmd))))
         redirects (into [] (comp (filter #(= (first %) :redirect))
                                  (mapcat (comp process-redirect second)))
                         (concat redir args))
         parameters (into [] (comp (filter #(= (first %) :arg))
                                   (map second))
                          args)]
     (if is-function
       (if (seq parameters)
         (list* 'do (second cmd) parameters)
         (second cmd))
       (let [name (second cmd)
             name-val (if (list? name)
                        (second name) ; when using cmd helper
                        (str name))
             parameters (mapv process-arg parameters)]
         (if (@*clob-commands* name)
           (let [cmd `(@clob.env/*clob-commands* '~name)]
             (if (empty? parameters)
               `(~cmd)
               `(apply ~cmd (concat ~@parameters))))
           `(core/shx (core/expand-command ~name-val)
                      ~parameters
                      ~@(when (seq redirects) [{:redir redirects}]))))))))

(defn ^:no-doc special?
  "Predicate to detect special form so we know not to partial apply it when piping.
  CLJS does not support dynamic macro detection so we also list selected macros."
  [symb]
  (or
   (special-symbol? symb)
   (#{`core/shx 'fn} symb)))
   ;; TODO: how to dynamically resolve and check for macro?
   ;; (-> symb resolve meta :macro boolean)))

(defn ^:no-doc process-pipeline
  [{:keys [cmd cmds]} redir-begin redir-end]
  (let [pipeline (butlast cmds)
        end (last cmds)]
    (reduce (fn [result [{:keys [op cmd]} redir]]
              (let [rest (rest cmd)
                    args (cond->> rest
                           (vector? (ffirst rest))
                           (apply concat))
                    redirects (into [] (comp (filter #(= (first %) :redirect))
                                             (mapcat (comp process-redirect second)))
                                    (concat redir args))
                    cmd (process-command cmd redir)
                    fn (pipes op)
                    cmd (if-not (special? (first cmd))
                          `#(pipeline/redir (~@cmd %) ~redirects)
                          cmd)]
                (list fn result cmd)))
            (process-command cmd redir-begin)
            (cond-> (mapv (fn [cmd] [cmd []]) pipeline)
              end (conj [end redir-end])))))

(defn ^:no-doc process-pipeline-interactive
  "Transform conformed pipeline specification in interactive mode. Pipeline by default reads from stdin and writes to stdout."
  [pipeline]
  (list `pipeline/wait-for-pipeline
        (process-pipeline pipeline
                          (cond-> [[:redirect {:op '>& :fd 0 :arg :stdin}]
                                   [:redirect {:op '>& :fd 2 :arg :stderr}]]
                            (empty? (:cmds pipeline)) (into [[:redirect {:op '>& :fd 1 :arg :stdout}]]))
                          [[:redirect {:op '>& :fd 1 :arg :stdout}]
                           [:redirect {:op '>& :fd 2 :arg :stderr}]])))

(defn ^:no-doc process-pipeline-batch
  "Transform conformed pipeline specification in batch mode. "
  [pipeline]
  (list `pipeline/wait-when-process
        (process-pipeline pipeline [] [])))

(defn ^:no-doc process-pipeline-command-substitution
  "Transform pipeline for command substitution mode, do not capture stderr."
  [pipeline]
  (list `pipeline/wait-for-pipeline
        (process-pipeline pipeline [] [[:redirect {:op '>& :fd 2 :arg :stderr}]])))

(defn ^:no-doc process-command-clause
  "Transform conformed command clause specification, handle conditional execution."
  [{:keys [pipeline pipelines]} process-pipeline]
  (let [items (reverse (conj (seq pipelines) {:pipeline pipeline}))]
    (:pipeline
      (reduce (fn [{op :op child :pipeline} pipeline]
                (update pipeline :pipeline
                        (fn [pipeline]
                          ;;what does :not do?
                          (let [pred (if (cond-> (= op '&&) (:not pipeline) not) `true? `false?)]
                            `(let [tmp# (pipeline/wait-for-pipeline ~(process-pipeline pipeline))]
                               (if (~pred (pipeline/pipeline-condition tmp#))
                                 ~child
                                 tmp#))))))
              (-> items first (update :pipeline process-pipeline))
              (rest items)))))

;; TODO: handle rest of commands when job control is implemented
(defn ^:no-doc process-command-list
  "Transform conformed command list specification."
  [{:keys [cmd cmds]} process-pipeline]
  (if (empty? cmds)
    (process-command-clause cmd #(second (process-pipeline %)))
    (concat ['do (process-command-clause cmd process-pipeline)]
            (map #(process-command-clause (:cmd %) process-pipeline) (butlast cmds))
            [(process-command-clause (:cmd (last cmds)) #(second (process-pipeline %)))])))

(defn compile-interactive
  "Parse tokens in command mode into clojure form that can be evaled. First it runs spec conformer and then does the transformation of conformed result. Uses interactive pipeline mode."
  [ast]
  (process-command-list ast process-pipeline-interactive))

(defn compile-batch
  "Parse tokens in command mode into clojure form that can be evaled. First it runs spec conformer and then does the transformation of conformed result. Uses batch pipeline mode."
  [ast]
  (process-command-list ast process-pipeline-batch))
