(ns clob.reader-test
  (:require [clojure.test :refer [deftest is are]]
            [clob.reader :as reader]))

(deftest test-reader
  (is (= (list 'ping (symbol "8.8.8.8"))
         (reader/read-string "ping 8.8.8.8")))

  (is (= (list 'ls (symbol "*.{cljc,clj}"))
         (reader/read-string "ls *.{cljc,clj}")))

  (is (= (list 'vim (symbol "~/.clobrc"))
         (reader/read-string "vim ~/.clobrc")))

  (is (= (list 'git 'clone (symbol "git@github.com:frenchy64/clob-shell.git"))
         (reader/read-string "git clone git@github.com:frenchy64/clob-shell.git")))

  (is (= '(echo $USER/$DISPLAY)
         (reader/read-string "echo $USER/$DISPLAY")))

  (is (= '(echo (+ 2 3))
         (reader/read-string "echo (+ 2 3)")))

  (is (= '(echo hi | cat)
         (reader/read-string "echo hi | cat")))

  (is (= '(echo 2 > tmp)
         (reader/read-string "echo 2 > tmp")))

  (is (= '(echo "a\nb")
         (reader/read-string "echo \"a\nb\"")))

  (is (= '(echo 3)
         (reader/read-string "echo 3")))

  ; (list 'echo false)
  ; "echo false"
  ;
  ; (list 'echo nil)
  ; "echo nil"

  (is (= '((+ 1 2))
         (reader/read-string "(+ 1 2)")))

  (is (= '((ls)
           (echo x)
           ((+ 1 2)))
         (reader/read-string-all "ls\necho x\n(+ 1 2)")))

  (is (= '((echo a)
           (echo b))
         (reader/read-string-all "echo a\\;echo b")))

  (is (= '((echo a)
           (echo b))
         (reader/read-string-all "echo a \\;echo b")))

  (is (= '((echo a)
           (echo b))
         (reader/read-string-all "echo a\\; echo b")))

  (is (= '((echo a)
           (echo b))
         (reader/read-string-all "echo a \\; echo b")))

  (is (= '((echo a))
         (reader/read-string-all "echo a ; echo b")))

  (is (= '((echo a))
         (reader/read-string-all "echo a ;; echo b")))

  (is (= '((echo a) (echo b))
         (reader/read-string-all "echo a \\;;;\n echo b")))

  (is (= '((echo a)
           (echo b))
         (reader/read-string-all "\\;echo a\\;echo b\\;")))

  (is (= '((echo a)
           (b))
         (reader/read-string-all "echo a \nb")))

  (is (= '((echo a b))
         (reader/read-string-all "echo a \\\nb")))

  (is (= '((echo a | (clojure.string/upper-case)))
         (reader/read-string-all "echo a \\\n | (clojure.string/upper-case)")))

  (is (= '((echo a)
           (echo b))
         (reader/read-string-all "\n\necho a\n\n\necho b\n\n")))

  ;; (list (list 'ls (symbol "A Filename With Spaces")))
  ;; "ls A\\ Filename\\ With\\ Spaces"

  ;; Maybe allow trailing pipe without backslash escape?
  ;; '((echo a | (clojure.string/upper-case)))
  ;; "echo a |\n (clojure.string/upper-case)"

  (are [x] (thrown? Exception (reader/read-string x))

       "echo (str 8.8.8)"

       "echo \""

       "echo (+ 1"))

(deftest test-reader-forms
  (is (= '[(clob.macros/sh (+ 1 2))
           (clob.macros/sh (* 3 4))]
         (let [in (reader/string-reader "(+ 1 2)\n(* 3 4)")]
           [(reader/read-sh in)
            (reader/read-sh in)])))

  (is (= '[(clob.macros/sh echo a b)
           (clob.macros/sh ls)]
         (let [in (reader/string-reader "echo a b\nls")]
           [(reader/read-sh in)
            (reader/read-sh in)])))

  (is (= '[(clob.macros/sh (+ 1 2))]
         (let [in (reader/string-reader "(+ 1 2)\n")]
           [(reader/read-sh in)])))

  (is (= '[(clob.macros/sh (+ 1 2))]
         (let [in (reader/string-reader "(+ 1 2)")]
           [(reader/read-sh in)]))))
