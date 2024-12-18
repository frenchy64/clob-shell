
Take a look at example [config files](https://github.com/dundalek/dotfiles/tree/master/closh). Also get inspired by [community configs](https://github.com/search?q=in%3Apath+closh&type=Code).

## Autojump

To enable [Autojump](https://github.com/wting/autojump) refer to a following [configuration](https://github.com/dundalek/dotfiles/blob/master/closh/.closh_autojump.cljc).

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

[Liquid](https://github.com/mogenslund/liquid) is a text editor written in Clojure inspired by Emacs and Vim. There is a [plugin](https://github.com/mogenslund/closhapp) that integrates Clob with Liquid. One cool feature is that command output is written into a text buffer and can be later edited and manipulated within the text editor.

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
  `(binding [clob.platform.process/*cwd*
             (atom (clob.platform.process/resolve-path ~dir))]
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
(do (add-deps '{:deps {funcool/datoteka {:mvn/version "1.1.0"}}})
    (require '[datoteka.core :as f]))
```

Then you can use those functions for example like:
```clojure
(f/executable? "myapp") && ./myapp
```
