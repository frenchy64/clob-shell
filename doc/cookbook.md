
Take a look at example [config files](https://github.com/dundalek/dotfiles/tree/master/clob). Also get inspired by [community configs](https://github.com/search?q=in%3Apath+clobrc&type=Code).

## Running nREPL server

[Pomegranate](https://github.com/cemerick/pomegranate) is included on the classpath so you can dynamically load other libraries. Using pomegranate nREPL server can be included and started. For example you can put following into your `~/.clobrc`:

```clojure
(defn start-nrepl
  ([] (start-nrepl 7888))
  ([port]
   (eval
    `(do
       (require '[cemerick.pomegranate])
       (cemerick.pomegranate/add-dependencies
         :coordinates '[[org.clojure/tools.nrepl "0.2.13"]]
         :repositories (merge cemerick.pomegranate.aether/maven-central
                              {"clojars" "https://clojars.org/repo"}))
       (require '[clojure.tools.nrepl.server])
       (println "Starting nrepl at" ~port)
       (defonce server (clojure.tools.nrepl.server/start-server :port ~port))))))
```

Then start the nREPL server with:
```clojure
(start-nrepl)
```

Connect to it from other client, for example:
```sh
lein repl :connect 7888
```

The current nREPL support is limited, for example the custom reader is not included. It can probably be added via middleware. If you have some experience creating nREPL middleware please leave a note in [#88](https://github.com/dundalek/clob/issues/88). So shelling out via nREPL at the momemnt needs to be done with `sh` macros:

```clojure
$ (sh-str echo hello nrepl)
$ (sh-value ls *.txt)
```

## Autojump

To enable [Autojump](https://github.com/wting/autojump) refer to a following [configuration](https://github.com/dundalek/dotfiles/blob/master/clob/.clob_autojump.cljc).

## Direnv

If you are using [direnv](https://github.com/direnv/direnv) to switch an environment based on a working directory you can augment the `clob-prompt` definition in the `~/.clobrc` like this:

```clojure
(defn clob-prompt []
  (source-shell "bash" "eval \"$(direnv export bash)\"")
  ; your prompt logic here
  )
```

## NVM integration

To use [nvm](https://github.com/creationix/nvm) put the following into your `~/.clobrc`:
```clojure
(source-shell "export NVM_DIR=\"$HOME/.nvm\"; [ -s \"$NVM_DIR/nvm.sh\" ] && . \"$NVM_DIR/nvm.sh\"")

(defn args->str [args]
  (->> args
    (map #(str "'" (clojure.string/replace % #"'" "'\"'\"'") "'"))
    (clojure.string/join " ")))

(defcmd nvm [& args]
  (print (source-shell (str ". \"$NVM_DIR/nvm.sh\"; nvm " (args->str args)))))
```

## AWS CLI

[cljaws](https://github.com/timotheosh/cljaws) is a project that integrates with clob and allows you to run AWS API commands with pure Clojure from the command line.

## Conda integration

To use [conda](https://anaconda.org/) put the following into your `~/.clobrc`:

```clojure
(source-shell ". ~/anaconda/etc/profile.d/conda.sh; conda activate")
```

## Integration with text editors

[Liquid](https://github.com/mogenslund/liquid) is a text editor written in Clojure inspired by Emacs and Vim. There is a [plugin](https://github.com/mogenslund/clobapp) that integrates Clob with Liquid. One cool feature is that command output is written into a text buffer and can be later edited and manipulated within the text editor.

## Using Google Closure Library

Closure library is built in, so you can use it like so:

```clojure
(require 'goog.string.format)
(goog.string.format "%03d" 7)
; => "007"
```

## Manipulating multiple files

Often there is a need to do some work with multiple files. An example might be to convert all text files in a directory to PDFs for printing. A single file can be converted with `unoconv abc.txt abc.pdf`.

To convert all txt files in a directory with `bash` you can do:
```bash
for f in *.txt; do unoconv "$f" `echo "$f" | sed 's/\.txt$/.pdf/'`; done
```

Here is an example how you could do that with `clob`:
```clojure
(doseq [f (expand "*.txt")] (sh unoconv (str f) (str/replace f #"\.txt$" ".pdf")))
```

## Temporarily change Current Working Directory

In bash it is usually done using subshell or directory stack:
```bash
# Using subshell
(cd SOME_PATH && exec_some_command)

# Using directory stack
pushd SOME_PATH
exec_some_command
popd
```

Possible solution in clob with a macro:
```clojure
(defmacro with-cwd [dir & body]
  `(binding [clob.zero.platform.process/*cwd*
             (atom (clob.zero.platform.process/resolve-path ~dir))]
     (sh ~@body)))
```

Then it can be used as:
```clojure
(with-cwd "src"
  pwd \;
  ls -l)
```

## Get absolute location of a script

Useful to reference files relatively to a location of running script. In bash usually done with a following idiom:

```bash
__dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
```

It can be done in clob with:

```clojure
(def dir (-> *file* clojure.java.io/as-file .getParentFile .getCanonicalPath))
```

## Helpers for file tests

You can use [datoteka](https://funcool.github.io/datoteka/latest/#reference) library which provides helper functions for tests on files like `path?`, `file?`, `absolute?`, `relative?`, `executable?`, `exists?`, `directory?`, `regular-file?`, `link?`, `hidden?`, `readable?`, `writable?`.

Add following into your `~/.clobrc`.
```clojure
#?(:clj
   (do
     (require '[cemerick.pomegranate])
     (cemerick.pomegranate/add-dependencies
       :coordinates '[[funcool/datoteka "1.1.0"]]
       :repositories (merge cemerick.pomegranate.aether/maven-central
                            {"clojars" "https://clojars.org/repo"}))
     (require '[datoteka.core :as f])))

```

Then you can use those functions for example like:
```clojure
(f/executable? "myapp") && ./myapp
```
