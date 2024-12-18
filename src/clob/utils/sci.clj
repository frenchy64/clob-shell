(ns clob.utils.sci
  (:refer-clojure :exclude [eval load-file])
  (:require [sci.core :as sci]
            [sci.impl.interpreter :as interpreter]
            [sci.impl.opts :as opts]
            [fipp.edn]
            [babashka.impl.deps :as bdeps]
            [clojure.repl.deps :as repl-deps]
            [clob.pipeline :as pipeline]
            [clob.core :as clob-core]
            [clob.compiler]
            [clob.parser]
            [clob.platform.process :as process]
            [clob.builtin :as builtin]
            [clob.env :as env]
            [clob.util :as util]
            [clob.macros-fns :as macros-fns]
            [clojure.java.io :as jio]
            [clojure.java.javadoc :as javadoc]
            [clojure.repl :as repl]
            [sci.impl.unrestrict :refer [*unrestricted*]]
            [clob.platform.clojure-compiler :as clojure-compiler]))

(set! *warn-on-reflection* true)

(comment
  (defmacro clob-requires []
    clob.env/*clob-environment-requires*)

  (defmacro clob-bindings []
    (->> clob.env/*clob-environment-requires*
         (drop 1)
         (mapcat (fn [[_ [namespace & opts]]]
                   (when (not= namespace 'clob.macros)
                     (let [{:keys [as refer]} (apply hash-map opts)]
                       (concat
                        (for [x refer]
                          [x (symbol (str namespace) (str x))])
                        (->>
                         (ns-publics namespace)
                         (keys)
                         (map (fn [k]
                                [(symbol (str namespace) (str k))
                                 (symbol (str namespace) (str k))])))
                        (when as
                          (->>
                           (ns-publics namespace)
                           (keys)
                           (map (fn [k]
                                  [(symbol (str as) (str k))
                                   (symbol (str as) (str k))])))))))))
         (map (fn [[k v]]
                [(list 'quote k) v]))
         (into {})))

  (defmacro clob-macro-bindings []
    (with-open [rdr (io/reader "src/common/clob/macros.cljc")]
      (let [prdr (PushbackReader. rdr)
            eof (Object.)
            opts {:eof eof :read-cond :allow :features #{:clj}}]
        (loop [bindings {}]
          (let [form (reader/read opts prdr)]
            (cond (= form eof) bindings

                  (= (first form) 'defmacro)
                  (let [name (second form)
                        fn-form `(with-meta
                                   (fn ~name ~@(drop-while #(not (vector? %)) form))
                                   {:sci/macro true})]
                    (recur (assoc bindings
                                  (list 'quote name) fn-form
                                  (list 'quote (symbol "clob.macros" (str name))) fn-form)))

                  :else (recur bindings)))))))

  (clob-requires)

  #_(def bindings
      (merge
       (clob-bindings)
       (clob-macro-bindings)
       {})))

(declare ctx)
(declare eval)

(defn load-file [file]
  (clojure-compiler/load-file file eval))

(def sci-env (atom {}))

(def macro-bindings
  {'sh (with-meta (fn [_ _ & args] (apply macros-fns/sh args)) {:sci/macro true})
   'sh-value (with-meta (fn [_ _ & args] (apply macros-fns/sh-value args)) {:sci/macro true})
   'sh-val (with-meta (fn [_ _ & args] (apply macros-fns/sh-val args)) {:sci/macro true})
   'sh-str (with-meta (fn [_ _ & args] (apply macros-fns/sh-str args)) {:sci/macro true})
   'sh-seq (with-meta (fn [_ _ & args] (apply macros-fns/sh-seq args)) {:sci/macro true})
   'sh-lines (with-meta (fn [_ _ & args] (apply macros-fns/sh-lines args)) {:sci/macro true})
   'sh-code (with-meta (fn [_ _ & args] (apply macros-fns/sh-code args)) {:sci/macro true})
   'sh-ok (with-meta (fn [_ _ & args] (apply macros-fns/sh-ok args)) {:sci/macro true})
   'sh-wrapper (with-meta (fn [_ _ & args] (apply macros-fns/sh-wrapper args)) {:sci/macro true})
   'defalias (with-meta (fn [_ _ & args] (apply macros-fns/defalias args)) {:sci/macro true})
   'defabbr (with-meta (fn [_ _ & args] (apply macros-fns/defabbr args)) {:sci/macro true})
   'defcmd (with-meta (fn [_ _ & args] (apply macros-fns/defcmd args)) {:sci/macro true})})

(def bindings {'deref deref
               'clojure.core/deref deref
               'slurp slurp
               'swap! swap!
               'clojure.core/swap! swap!
               'print print
               'println println
               'load-file load-file
               'add-deps bdeps/add-deps
               'Math/sqrt #(Math/sqrt %)
               'clojure.repl/set-break-handler! repl/set-break-handler!
               '*clob-commands* env/*clob-commands*
               'cd builtin/cd
               'exit builtin/exit
               'quit builtin/quit
               'getenv builtin/getenv
               'setenv builtin/setenv
               'unsetenv builtin/unsetenv
               '*args* (sci/new-dynamic-var '*args* (rest *command-line-args*))
               'source-shell util/source-shell
               '*clob-aliases* env/*clob-aliases*
               'expand clob-core/expand
               'shx clob-core/shx})

(def repl-requires {; 'source
                    ; (with-meta
                    ;   (fn source [_ _ & n]
                    ;     `(println (or (source-fn '~n) (str "Source not found"))))
                    ;   {:sci/macro true})
                    ; 'apropos clojure.repl/apropos
                    ; 'dir
                    ; (with-meta
                    ;   (fn dir [_ _ & nsname]
                    ;     `(doseq [v# (dir-fn '~nsname)]
                    ;        (println v#)))
                    ;   {:sci/macro true})
                    'javadoc javadoc/javadoc
                    ; 'source repl/source
                    'apropos repl/apropos
                    ; 'dir repl/dir
                    'pst repl/pst
                    ; 'doc repl/doc
                    'find-doc repl/find-doc
                    'pprint fipp.edn/pprint})
                    ;; TODO pp macro

(def ctx {:bindings (merge bindings repl-requires macro-bindings)
          :namespaces {'clob.macros macro-bindings
                       'clojure.core {'println println
                                      'print print
                                      'pr pr
                                      'prn prn
                                      'pr-str pr-str}
                       'clojure.java.io {'file jio/file
                                         'reader jio/reader}
                       'clob.pipeline {'pipe pipeline/pipe
                                       'redir pipeline/redir
                                       'wait-for-pipeline pipeline/wait-for-pipeline
                                       'pipeline-condition pipeline/pipeline-condition
                                       'pipe-multi pipeline/pipe-multi
                                       'process-output pipeline/process-output
                                       'pipe-filter pipeline/pipe-filter}
                       'clob.platform.process {'exit-code process/exit-code
                                               'wait process/wait
                                               'cwd process/cwd
                                               'process? process/process?}
                       'clob.core {'expand-variable clob-core/expand-variable
                                   'expand-tilde clob-core/expand-tilde
                                   'expand-filename clob-core/expand-filename
                                   'expand-redirect clob-core/expand-redirect
                                   'expand-partial clob-core/expand-partial
                                   'expand clob-core/expand
                                   'expand-command clob-core/expand-command
                                   'get-command-suggestion clob-core/get-command-suggestion
                                   'shx clob-core/shx
                                   'expand-alias clob-core/expand-alias
                                   'expand-abbreviation clob-core/expand-abbreviation
                                   '-clob-version clob-core/-clob-version
                                   'clob-version clob-core/clob-version}
                       'babashka.deps {'add-deps bdeps/add-deps}
                       'clob.util {'source-shell util/source-shell}
                       'clob.env {'*clob-aliases* env/*clob-aliases*
                                  '*clob-commands* env/*clob-commands*
                                  '*clob-abbreviations* env/*clob-commands*}
                       'clob.macros-fns macro-bindings}
          :classes {:allow :all}
          :env sci-env})

(defn sci-eval [form]
  ;; (prn "EVAL FORM" form)
  ;; (sci/eval-string (pr-str form) ctx)
  (binding [*unrestricted* true]
    (let [ctx (opts/init ctx)]
      (interpreter/eval-form ctx form))))

(comment
  (sci-eval 1)
  (sci-eval '(+ 1 2))
  (sci-eval '(add-deps
               '{:deps {org.clojure/data.xml {:mvn/version "0.0.8"}}}))
  )

(defn eval [form]
  (sci-eval
   (clob.compiler/compile-interactive
    (clob.parser/parse form))))

(defn repl-print? [val]
  (not (or (nil? val)
           (identical? val env/success)
           (process/process? val))))

(defn repl-print
  [& args]
  (when (repl-print? (first args))
    (apply prn args)))
