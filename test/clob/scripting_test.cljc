(ns clob.scripting-test
  (:require [clojure.test :refer [deftest are]]
            [clob.zero.core :refer [shx]]
            [clob.zero.pipeline :refer [process-output process-value pipe]]))

(def sci? #?(:clj (System/getenv "__CLOB_USE_SCI_EVAL__")
             :cljs false))

(def sci-complete? #?(:clj (System/getenv "__CLOB_USE_SCI_COMPLETE__")
                      :cljs false))

(defn clob [& args]
  (shx "clojure" (concat (if sci?
                           ["-M:sci" "-m" "clob.zero.frontend.sci"]
                           ["-m" "clob.zero.frontend.rebel"])
                         args)))

(deftest scripting-test

  (are [x y] (= x (process-output y))

    "a b\n"
    (clob "-e" "echo a b")

    "a b\n"
    (pipe (shx "echo" ["echo a b"])
          (clob "-"))

    "3\n"
    (pipe (shx "echo" ["echo (+ 1 2)"])
          (clob "-"))

    "bar\n"
    (clob "fixtures/script-mode-tests/bar.cljc")

    "foo\nbar\n"
    (clob "fixtures/script-mode-tests/foo.cljc")

    "Hi World\n"
    (clob "-i" "fixtures/script-mode-tests/cmd.cljc" "-e" "my-hello World")

    "Hello World\n"
    (clob "-i" "fixtures/script-mode-tests/cmd2.cljc")

    "(\"a\" \"b\")\n"
    (clob "fixtures/script-mode-tests/args.cljc" "a" "b")

    "a b\n"
    (clob "fixtures/script-mode-tests/cond.cljc")

    ;; TODO metadata reader for sci
    "true"
    (clob "-e" (if sci?
                  "(print (:dynamic (meta (with-meta {} {:dynamic true}))))"
                  "(print (:dynamic (meta ^:dynamic {})))"))))

(when (or (not sci?)
          sci-complete?)
  (deftest scripting-errors-test

    (are [result regex cmd] (= result (->> (:stderr (process-value cmd))
                                           (re-find regex)
                                           (second)))

      "5:3"
      #"/throw1\.cljc:(\d+:\d+)"
      (clob "fixtures/script-mode-tests/throw1.cljc")

      "4:2"
      #"Syntax error compiling at \(REPL:(\d+:\d+)\)"
      (pipe "\n\n\n (throw (Exception. \"my exception message\"))" (clob "-"))

      ; TODO
      ; "2:4"
      ; (if (System/getenv "__CLOB_USE_SCI_EVAL__")
      ;   #"Syntax error reading source at \(REPL:(\d+:\d+)\)"
      ;   #"Syntax error \(ExceptionInfo\) compiling at \(REPL:(\d+:\d+)\)")
      ; (pipe "\n  )" (clob "-"))

      "5:1"
      #"/throw2\.cljc:(\d+:\d+)"
      (clob "fixtures/script-mode-tests/throw2.cljc")

      "3"
      #"Execution error at .* \(REPL:(\d+)\)"
      (clob "-e" "\n\n(throw (Exception. \"my exception message\"))"))))

    ; "2"
    ; #"Execution error at .* \(REPL:(\d+)\)"
    ; (clob "-e" "\n  )")))
