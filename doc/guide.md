
## Quick Guide

Command mode uses simple shell-like syntax

```
git commit -a
```

Forms starting with parenthesis are considered as Clojure code

```
(+ 1 2)
```

Glob patterns and environment variables are expanded
```
ls *.txt

ls $HOME

cd ~/Downloads
```

Use output from a command or function as arguments to other command

```
echo (+ 1 2)
echo (sh-str date)
```

Piping output between commands

```
ls | head
```

Piping with functions works similarly to `->>` threading macro

```
ls | (clojure.string/upper-case)

ls | #(clojure.string/replace % #"\.txt" ".md")
```

Use special `|>` pipe operator to split input into sequence of lines

```
ls |> (reverse) | (take 5)
```

Redirects - note that there must be spaces around redirection operators `>` and `>>`

```
ls > files.txt

echo hi >> file.txt

ls 2 > files.txt
```

Command status

```
echo hi && echo OK

! echo hi || echo FAILED
```

## Examples

Most of helper utilities can be replaced with functions on sequences.

```clojure
bash:  ls | head -n 5
clob: ls |> (take 5)

bash:  ls | tail -n 5
clob: ls |> (take-last 5)

bash:  ls | tail -n +5
clob: ls |> (drop 4)

; Print filenames starting with "."
bash:  ls -a | grep "^\\."
clob: ls -a |> (filter #(re-find #"^\." %))

; Print only odd numbered lines counting from 1
bash:  ls | sed -n 1~2p
clob: ls |> (keep-indexed #(when (odd? (inc %1)) %2))

; Math
bash:  echo '(1 + sqrt(5))/2' | bc -l
clob: (/ (+ 1 (Math.sqrt 5)) 2)
```
### Control flow

For loops:

```bash
for f in /sys/bus/usb/devices/*/power/wakeup; do echo $f; cat $f; done
```
```clojure
; Using doseq
(doseq [f (expand "/sys/bus/usb/devices/*/power/wakeup")] (println f) (sh cat (str f)))
```

```clojure
; Or multi pipes
ls /sys/bus/usb/devices/*/power/wakeup |> (map #(str % "\n" (sh-str cat (str %)))) | cat
```

If conditionals:

```bash
if test -f package.json; then echo file exists; else echo no file; fi
```
```clojure
echo (if (sh-ok test -f package.json) "file exists" "no file")
```

### Sequence of commands

```
bash:  ls; echo hi
clob: (sh ls) (sh echo hi)
```

## Reference

### History

History gets saved to the file `~/.clob/clob.sqlite` which is a SQLite database.

Use <kbd>up</kbd> and <kbd>down</kbd> arrows to cycle through history. First history from a current session is used, then history from all other sessions is used.

If you type some text and press <kbd>up</kbd> then the text will be used to match beginning of the command (prefix mode). Pressing <kbd>ctrl-r</kbd> will switch to matching anywhere in the command (substring mode). The search is case insensitive.

While in the history search mode you can use following controls:
- <kbd>enter</kbd> to accept the command and execute it
- <kbd>tab</kbd> to accept the command but have ability to edit it
- <kbd>esc</kbd> cancel search keeping the initial text
- <kbd>ctrl-c</kbd> cancel search and resetting the initial text

To show history you can run:
```sh
sqlite3 ~/.clob/clob.sqlite "SELECT command FROM history ORDER BY id ASC"
```

For convenience you can add the following to your `~/.clobrc` file:
```clj
(defcmd history []
  (sh sqlite3 (str (getenv "HOME") "/.clob/clob.sqlite") "SELECT command FROM history ORDER BY id ASC" | cat))
```

### Environment variables

There are some helper functions for doing common things with environment variables

**setenv**: set environment variable
```
setenv "ONE" "1"
=> ("1")
(setenv "ONE" "1")
=> ("1")
(setenv "ONE" "1" "TWO" "2")
=> ("1" "2")
```

**getenv**: get environment variable
```
getenv "ONE"
=> "1"
(getenv "ONE")
=> "1"
(getenv "ONE" "TWO")
=> {"ONE" "1", "TWO" "2"}
getenv
=> ;; returns a map of all environment variables
```

**source-shell**: run bash scripts and import the resulting environment variables into the clob environment
```
(source-shell "export ONE=42")
=> nil
getenv "ONE"
=> "42"
```
`source-shell` defaults to `bash` but you can use other shells:
```
(source-shell "zsh" "source complicated_script.zsh")
=> nil
getenv "SET_BY_COMPLICATED_SCRIPT"
=> "..."
```

To set a temporary variable while running a command, do this:
```
env VAR=1 command
```
Which is equivalent to bash:
```
VAR=1 command
```

#### Built-in environment variables

- `PWD` - a path to the current working directory

### Custom prompt

The prompt can be customized by defining `clob-prompt` function in `~/.clobrc` file.

For example you can use [powerline](https://github.com/banga/powerline-shell) prompt like this:

```clojure
(require-macros '[clob.zero.core :refer [sh-str]])

(defn clob-prompt []
  (sh-str powerline-shell --shell bare))
```

Or you can reuse existing prompt from [fish](http://fishshell.com/) shell:

```clojure
(require-macros '[clob.zero.core :refer [sh-str]])

(defn clob-prompt []
  (sh-str fish -c fish_prompt))
```

Or you can implement a custom [prompt in clojure](https://gist.github.com/jeroenvandijk/22927bd763ab786ec826a7727b43208c).

### Tab completion

Clob delegates completion to existing shells. When tab completion is triggered it tries to fetch completions first from `fish`, then `zsh` and finally `bash`. One the mentioned shells needs to be installed for completion to work.

If the completion does not work you can find out the reason by cloning the repo and trying out:

```sh
./resources/completion/completion.bash "ls "
./resources/completion/completion.zsh "ls "
./resources/completion/completion.fish "ls "
```

### Custom commands

You can define helper aliases, abbreviations, functions and commands in your `~/.clobrc` file.

#### Aliases

Aliases for defining or overriding functionality. When alias is used it does not get expanded and is saved into history as is.

Example:
```clojure
(defalias ls "ls --color=auto")
```

#### Abbreviations

Abbreviations are similar to aliases but expand to underlying definition on the command line after you type space or press enter. Therefore autocomplete can work seamlessly and also full command is saved to history. Inspired by [abbr](https://fishshell.com/docs/current/commands.html#abbr) in fish (more [details](https://github.com/fish-shell/fish-shell/issues/731)).

```clojure
(defabbr gco "git checkout")
(defabbr gaa "git add --all")
```

#### Functions

Define Clojure functions, run in command line like: `(hello "World")`.

```clojure
(defn hello [name]
  (str "Hello " name))
```

#### Commands

Similar to functions, but can be executed like commands without using parens. Run in command line like: `hello World`.

```clojure
; Define commands like with defn
(defcmd hello [name]
  (str "Hello " name))

; Promote existing function to a command
(defcmd upper clojure.string/upper-case)
```

### Quoting

Prevent some expansion is same like bash with double-quoted string:
```
echo "*"
```

Disable expansion completely with a single quote:
```clojure
bash:  echo '$HOME'
clob: echo '$HOME ; notice there is only one quote

; if the quoted string has spaces wrap in double quotes and then prepend single quote
bash:  echo '$HOME $PWD'
clob: echo '"$HOME $PWD"
```

### Signal handling

- SIGINT interrupts currently running command or code
- SIGQUIT is ignored
