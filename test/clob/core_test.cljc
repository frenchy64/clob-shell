(ns clob.core-test
  (:require [clojure.test :refer [deftest is are]]
            [clob.test-util.util :refer [null-file with-tempfile with-tempfile-content create-fake-writer get-fake-writer str-fake-writer]]
            [clojure.string :as str]
            [clob.reader :as reader]
            [clob.builtin :refer [getenv setenv]]
            [clob.env]
            [clob.compiler]
            [clob.parser]
            [clob.platform.io]
            [clob.platform.process :as process]
            [clob.pipeline :as pipeline :refer [process-output process-value]]
            [clob.core :refer [shx expand]]
            ;;[clob.macros :refer [sh sh-str defalias defabbr]]
            [clob.platform.eval :as eval]))

(def user-namespace (create-ns 'user))
(binding [*ns* user-namespace]
  (eval/eval-clob-requires))

(defn bash [cmd]
  (pipeline/process-value (shx "bash" ["-c" cmd])))

(defn clob-spawn-helper [cmd]
  (pipeline/process-value
    (cond (and (process/getenv "__CLOB_USE_SCI_NATIVE__") (process/getenv "CI_ENV"))
          (shx "./clob-sci" "-e" [cmd])

          (process/getenv "__CLOB_USE_SCI_NATIVE__")
          (shx "java" ["-jar" "target/clob-sci.jar" "-e" cmd])

          (process/getenv "__CLOB_USE_SCI_EVAL__")
          (shx "clojure" ["-M:jansi" "-m" "clob.frontend.sci" "-e" cmd])

          :else
          (shx "clojure" ["-M" "-m" "clob.frontend.rebel" "-e" cmd]))))

(defn clob-spawn-in-process [cmd]
  (let [out (create-fake-writer)
        err (create-fake-writer)]
    (binding [clob.platform.io/*stdout* (get-fake-writer out)
              clob.platform.io/*stderr* (get-fake-writer err)]
      (let [code (reader/read (reader/string-reader cmd))
            proc (eval/eval `(-> ~(clob.compiler/compile-batch (clob.parser/parse code))
                                 (clob.pipeline/wait-for-pipeline)))]
        (if (process/process? proc)
          (do
            (process/wait proc)
            {:stdout (str-fake-writer out)
             :stderr (str-fake-writer err)
             :code (process/exit-code proc)})
          (let [{:keys [stdout stderr code]} (process-value proc)]
            {:stdout (str (str-fake-writer out) stdout)
             :stderr (str (str-fake-writer err) stderr)
             :code code}))))))

(defn clob-run [cmd]
  (let [code (clob.compiler/compile-batch
               (clob.parser/parse (reader/read (reader/string-reader cmd))))]
    (binding [*ns* user-namespace]
      (clob.pipeline/process-value (eval/eval code)))))

(def clob-spawn
  ;; Run proper spawn helper on CI, for local machine use in-process runner for faster iteration
  (if (or (process/getenv "__CLOB_USE_SCI_NATIVE__") (process/getenv "CI_ENV"))
    clob-spawn-helper
    clob-spawn-in-process))

(def clob
  (if (process/getenv "__CLOB_USE_SCI_NATIVE__")
    clob-spawn
    clob-run))

(deftest run-test

  (is (= "package.json\n" (process-output (shx "ls" [(expand "package.js*")]))))

  (is (= (process-output (shx "ls"))
         (process-output (shx "ls" ["-d" (expand "*")]))))

  (is (= (process-output (shx "ls" ["scripts"]))
         (do (process/chdir "scripts")
             (let [out (process-output (shx "ls" ["-d" (expand "*")]))]
               (process/chdir "..")
               out))))

  (is (= (-> (slurp "package.json")
             (clojure.string/trimr)
             (clojure.string/split-lines)
             (seq))
         (clob.platform.io/line-seq
          (java.io.FileInputStream. "package.json"))))

  (are [x y] (= x (:stdout (clob y)))
    "3"
    "(+ 1 2)"

    "hi\n"
    "echo hi"

    "hi\n"
    "echo hi | (str)"

    "HI\n"
    "echo hi | (clojure.string/upper-case)"

    "HI\n"
    "echo hi | (str/upper-case)"

    "3\n"
    "echo (+ 1 2)"

    "x\n"
    "echo (sh-str echo x)"

    "2"
    "(sh-str echo \"a\n\b\" |> (count))"

    "3"
    "(list :a :b :c) |> (count)"

    "OK\n"
    "(identity true) && echo OK"

    "false"
    "(identity false) && echo OK"

    "OK\n"
    "(identity false) || echo OK"

    ; process to process - redirect stdout
    "ABC\n"
    "echo abc | tr \"[:lower:]\" \"[:upper:]\""

    ; process to fn - collect stdout
    "ABC\n"
    "echo abc | (clojure.string/upper-case)"

    ; process to sequence - split lines
    "(\"c\" \"b\" \"a\")"
    "printf \"a\\nb\\nc\" |> (reverse)"

    ; sequence to fn
    "1"
    "(list 1 2 3) | (first)"

    ; sequence to sequence
    "(3 2 1)"
    "(list 1 2 3) | (reverse)"

    ; sequence to process - join items
    "1\n2\n3\n"
    "(list 1 2 3) | cat -"

    "{:a 123}"
    "(identity {:a 123}) | cat -"

    "{:a 123}\n{:b 456}\n"
    "(list {:a 123} {:b 456}) | cat -"

    ; string to process
    "abc"
    "(str \"abc\") | cat -"

    ; string to sequence
    "(\"c\" \"b\" \"a\")"
    "(str \"a\\nb\\nc\") |> (reverse)"

    ; sequential
    "[1 2 3]"
    "(identity [1 2 3]) |> (identity)"

    "(1 2 3)"
    "(list 1 2 3) |> (identity)"

    ; non-seqable to seqable - wrap in list
    "(false)"
    "(identity false) |> (identity)"

    "[\"a\" \"b\"]"
    "echo a b | #(clojure.string/split % #\"\\s+\")"

    ; cmd helper to invoke command name by value
    "x\n"
    "(cmd \"echo\") x"

    "abc > /tmp/x\n"
    "echo abc '> /tmp/x"

    "{"
    "cat < package.json | (first)"

    "ok\n"
    "cd . && echo ok"

    "ok\n"
    "fixtures/echo-tester ok"

    "ok\n"
    "./fixtures/echo-tester ok"

    ;; Make sure updating PATH is reflected for executable lookup
    "ok\n"
    "setenv PATH (str (getenv \"PWD\") \"/fixtures:\" (getenv \"PATH\")) && echo-tester ok")

    ; TODO: Fix input redirection to a function
    ; "{"
    ; "(first) < package.json")

  (are [x] (= (bash x) (clob x))
    "ls"

    "git status"

    "ls -l *.json"

    "ls $HOME"

    "ls | head"

    ; TODO: fix exit code
    ; "! echo hi && echo NO"

    "echo a | egrep b || echo OK"

    "cat < package.json"

    "echo x | cat < package.json"

    "cat < package.json | cat"

    "/bin/ech? x")

  (are [x y] (= (bash x) (clob y))
    "echo \"*\""
    "echo \"*\""

    "echo '$HOME $PWD'"
    "echo '\"$HOME $PWD\""

    "echo '$HOME'"
    "echo '$HOME"

    "ls | head -n 5"
    "ls |> (take 5) | cat"

    "ls | tail -n 5"
    "ls |> (take-last 5) | cat"

    "ls | tail -n +5"
    "ls |> (drop 4) | cat"

    "ls -a | grep \"^\\.\""
    "ls -a |> (filter #(re-find #\"^\\.\" %)) | cat"

    "ls -a | grep \"^\\.\""
    "ls -a |? (re-find #\"^\\.\") | cat"

    "ls | ls | awk 'NR%2==1'"
    "ls |> (keep-indexed #(when (odd? (inc %1)) %2)) | cat"

    "ls | sort -r | head -n 5"
    "ls |> (reverse) | (take 5) | cat"

    "ls *.json | sed 's/\\.json$/.txt/'"
    "ls *.json | #(clojure.string/replace % #\"\\.json\" \".txt\")"

    "ls *.json | sed 's/\\.json$/.txt/'"
    "ls *.json |> (map #(clojure.string/replace % #\"\\.json\" \".txt\")) | cat"

    "ls *.json | sed 's/\\.json$/.txt/'"
    "ls *.json | (fn [x] (clojure.string/replace x #\"\\.json\" \".txt\")) | cat"

    "ls *.json | sed 's/\\.json$/.txt/'"
    "ls *.json | ((fn [x] (clojure.string/replace x #\"\\.json\" \".txt\"))) | cat"

    "echo $(date \"+%Y-%m-%d\")"
    "echo (sh-str date \"+%Y-%m-%d\")"

    "result=`echo '(1 + sqrt(5))/2' | bc -l`; echo -n ${result:0:10}"
    "(-> (/ (+ 1 (Math/sqrt 5)) 2) str (subs 0 10))"

    (str "cat < package.json 2>" null-file " | cat")
    (str "cat < package.json 2 > " null-file " | cat")

    "for f in test/clob/*.cljc; do echo $f; cat $f; done"
    "ls test/clob/*.cljc |> (map #(str % \"\\n\" (sh-str cat (str %)))) | cat"

    "if test -f package.json; then echo file exists; else echo no file; fi"
    "echo (if (sh-ok test -f package.json) \"file exists\" \"no file\")"

    "if test -f asdfgh.json; then echo file exists; else echo no file; fi"
    "echo (if (sh-ok test -f asdfgh.json) \"file exists\" \"no file\")"

    "ls -l `echo *.json *.md`"
    "ls -l (sh-seq echo *.json *.md)"

    "bash -c \"echo err 1>&2; echo out\""
    "bash -c \"echo err 1>&2; echo out\"")

  (are [x y] (= x (with-tempfile-content (fn [f] (clob y))))

    "x1\n"
    (str "echo x1 > " f)

    ;(str "echo x2 | (spit \"" f "\")")

    "x3\ny1\n"
    (str "(sh echo x3 > \"" f "\") (sh echo y1 >> \"" f "\")")

    ""
    (str "echo x4 2 > " f)

    "HELLO\n"
    (str "echo hello | (clojure.string/upper-case) > " f)

    "H"
    (str "echo hello | (first) | (clojure.string/upper-case) > " f)

    "X3\nY1\n"
    (str "(sh echo x3 | (clojure.string/upper-case) > \"" f "\")"
         "(sh echo y1 | (clojure.string/upper-case) >> \"" f "\")"))

  (are [x y] (= x (with-tempfile (fn [f] (clob y))))

    {:stdout "", :stderr "", :code 0}
    (str "echo hello | (clojure.string/upper-case) > " f))

  (are [x y] (= (with-tempfile-content (fn [f] (bash x)))
                (with-tempfile-content (fn [f] (clob y))))

    ; macOS does not have `tac` command but `tail -r` can be used instead
    (str "(ls | tac || ls | tail -r) > " f)
    (str "ls |> (reverse) > " f)))

(deftest run-special-cases
  (are [x y] (= (bash x) (clob-spawn y))
    "echo hi && echo OK"
    "echo hi && echo OK"

    "echo hi || echo NO"
    "echo hi || echo NO"

    "! echo hi || echo OK"
    "! echo hi || echo OK"

    "false || echo FAILED"
    "false || echo FAILED"

    "echo a && echo b && echo c"
    "echo a && echo b && echo c"

    "if test -f package.json; then echo file exists; else echo no file; fi"
    "(if (sh-ok test -f package.json) (sh echo file exists) (sh echo no file))"

    "ls; echo hi"
    "(sh ls) (sh echo hi)")

  (is (= "2\n1\ngo\n"
         (-> '(do (sh bash -c "sleep 0.2 && echo 2")
                  (sh bash -c "sleep 0.1 && echo 1")
                  (sh bash -c "echo go"))
             pr-str clob-spawn :stdout)))

  (is (= "2\n1\ngo\n"
         (-> '(sh bash -c "sleep 0.2 && echo 2" \;
                  bash -c "sleep 0.1 && echo 1" \;
                  bash -c "echo go")
             pr-str clob-spawn :stdout)))

  (is (= {:stdout "x\n" :stderr "" :code 0}
         (clob-spawn "(sh (cmd (str \"ec\" \"ho\")) x)")))

  (is (= "_asdfghj_: command not found\n"
         (:stderr (clob-spawn "_asdfghj_"))))

  (is (= {:stderr "_asdfghj_: command not found\n"
          :stdout ""}
         (-> (clob-spawn "_asdfghj_ && echo NO")
             (select-keys [:stdout :stderr]))))

  (is (= {:stderr "_asdfghj_: command not found\n"
          :stdout "YES\n"}
         (-> (clob-spawn "_asdfghj_ || echo YES")
             (select-keys [:stdout :stderr])))))

(deftest run-extra-special-cases
  (are [x y] (= (bash x) (clob-spawn-helper y))

    ; "mkdir x/y/z || echo FAILED"
    ; "mkdir x/y/z || echo FAILED"

    "for f in test/clob/*.cljc; do echo $f; cat $f; done"
    "ls test/clob/*.cljc |> #(doseq [f %] (sh echo (str f)) (sh cat (str f)))"))

(deftest test-builtin-getenv-setenv

  (is (= (pr-str (setenv "ONE" "6")) (:stdout (clob "setenv \"ONE\" \"6\""))))
  (is (= "42" (:stdout (clob "(sh setenv ONE 42) (sh getenv ONE)"))))
  (is (= "42" (:stdout (clob "(sh setenv \"ONE\" \"42\") (sh getenv \"ONE\")"))))
  (is (= (getenv "ONE") (:stdout (clob "getenv \"ONE\"")))))

(deftest test-builtin-cd
  (is (= (str/trim (:stdout (clob "pwd")))
         (str/trim (:stdout (clob "mkdir -p out && cd out && cd -")))))

  (is (str/ends-with? (let [result (str/trim (:stdout (clob "mkdir -p \"out/1\" && cd out && cd 1 && pwd")))]
                        (clob "cd ../..")
                        result)
                      "/out/1")))

(defn clob-out [cmd]
  (let [fut (future (:stdout (clob (pr-str cmd))))
        res (deref fut 1000 fut)]
    (if (identical? res fut)
      (do (future-cancel fut)
          (throw (ex-info (str "Too long: " (pr-str cmd)) {:cmd cmd})))
      res)))

(deftest commands
  (is (= "abcX" (clob-out '(do (defcmd cmd-x [s] (str s "X"))
                               (sh cmd-x "abc")))))
  (is (= "abcX" (clob-out '(do (defcmd cmd-x [s] (str s "X"))
                               (cmd-x "abc")))))
  (is (= "abcY" (clob-out '(do (defcmd cmd-y (fn [s] (str s "Y")))
                               (sh cmd-y abc)))))
  (is (= "original fn" (clob-out '(do (defn cmd-y [_] "original fn")
                                      (defcmd cmd-y (fn [s] (str s "Y")))
                                      (cmd-y "abc")))))
  (is (= "abcY" (clob-out '(do (defn cmd-y [_] "original fn")
                               (defcmd cmd-y (fn [s] (str s "Y")))
                               (sh cmd-y "abc")))))
  (is (= "abcZ" (clob-out '(do (defn fn-z [s] (str s "Z"))
                               (defcmd cmd-z fn-z)
                               (sh cmd-z abc)))))
  (is (= "ABC" (clob-out '(do (defcmd cmd-upper clojure.string/upper-case)
                              (sh echo -n abc | cmd-upper)))))
  (is (= "ABC" (clob-out '(do (defcmd cmd-upper clojure.string/upper-case)
                              (sh-str echo -n abc | cmd-upper | cat)))))
  (is (= "ABC" (clob-out '(do (defcmd cmd-upper str/upper-case)
                              (sh (str "abc") | cmd-upper)))))
  (is (= "hi" (clob-out '(do (defcmd cmd-hello [] "hi")
                             (sh cmd-hello)))))
  (is (= "HI" (clob-out '(do (defcmd cmd-hello [] "hi")
                             (sh cmd-hello | (str/upper-case))))))
  (is (= "HI" (clob-out '(do (defcmd cmd-hello [] "hi")
                             (sh-str cmd-hello | tr "[:lower:]" "[:upper:]")))))
  (is (= "ABC" (with-tempfile-content
                 (fn [f] (clob (format "(do (defcmd cmd-upper clojure.string/upper-case) (sh echo -n abc | cmd-upper > \"%s\"))"
                                       f)))))))
