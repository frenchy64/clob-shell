# clob.rebel-readline

A fork of [rebel-readline](https://github.com/bhauman/rebel-readline).

A terminal readline library for Clojure Dialects

[![asciicast](https://asciinema.org/a/160597.png)](https://asciinema.org/a/160597)

## Why create a terminal readline library?

[Intro](doc/intro.md)

## Important note!!!

The line reader will attempt to manipulate the terminal that initiates
the JVM process. For this reason it is important to start your JVM in
a terminal.

That means you should launch your Java process using the

 * the java command
 * the Clojure `clojure` tool (without readline support)
 * lein trampoline
 * boot - would need to run in boot's worker pod

Launching the terminal readline process from another Java process will not work.

It's best to not launch this readline behind other readline tools like `rlwrap`.

## Quick try

#### Clojure tools

If you want to try this really quickly
[install the Clojure CLI tools](https://clojure.org/guides/getting_started)
and then invoke this:

```shell
clojure -Sdeps "{:deps {com.ambrosebs/clob.readline {:git/url \"https://github.com/frenchy64/clob-shell.git\" :git/root \"readline\" :git/sha \"de2ec77482261f9d7d7a194088e8f18fb3bc3c36\"}}}" -m clob.readline.main
```

That should start a Clojure REPL that takes its input from the Rebel readline editor.

Note that I am using the `clojure` command and not the `clj` command
because the latter wraps the process with another readline program (rlwrap).

Alternatively you can specify an alias in your `$HOME/.clojure/deps.edn`

```clojure
{
 ...
 :aliases {:clob.readline {:extra-deps {com.ambrosebs/clob.readline {...}}
                           :main-opts  ["-m" "clob.readline.main"]}}
}
```

And then run with a simpler:

```shell
$ clojure -M:clob.readline
```

#### Clone repo

Clone this repo and then from the `clob.readline` sub-directory
typing `clojure -M -m clob.readline.main` will get you into
a Clojure REPL with the readline editor working.

Note that `lein run -m clob.readline.main` will not work! See above.

## How do I default to vi bindings?

In `~/.clojure/clob_readline.edn` put

```
{:key-map :viins}
```

## Config

In `~/.clojure/clob_readline.edn` you can provide a map with the
following options:

```
:key-map         - either :viins or :emacs. Defaults to :emacs

:color-theme     - either :light-screen-theme or :dark-screen-theme

:highlight       - boolean, whether to syntax highlight or not. Defaults to true

:completion      - boolean, whether to complete on tab. Defaults to true

:eldoc           - boolean, whether to display function docs as you type.
                   Defaults to true

:indent          - boolean, whether to auto indent code on newline. Defaults to true

:redirect-output - boolean, rebinds root *out* during read to protect linereader
                   Defaults to true
                   
:key-bindings    - map of key-bindings that get applied after all other key 
                   bindings have been applied
```

#### Key binding config

You can configure key bindings in the config file, but your milage may vary.

Example:

```
{ 
...
:key-bindings { :emacs [["^D" :clojure-doc-at-point]] 
                :viins [["^J" :clojure-force-accept-line]] }
}
```

Serialized keybindings are tricky and the keybinding strings are translated with
`org.jline.keymap.KeyMap/translate` which is a bit peculiar in how it translates things.

If you want literal characters you can use a list of chars or ints i.e
`(\\ \d)` instead of the serialized key names. So you can use `(4 4)` inplace of `"^D^D"`.

The best way to look up the available widget names is to use the `:repl/key-bindings`
command at the REPL prompt.

Note: I have found that JLine handles control characters and
alphanumeric characters quite well but if you want to bind special
characters you shouldn't be surprised if it doesn't work.

## Quick Lay of the land

You should look at `clob.readline.clojure.main` and `clob.readline.core`
to give you top level usage information.

The core of the functionality is in
`clob.readline.clojure.line-reader` everything else is just support.

## Quick Usage

These are some quick examples demonstrating how to use the clob.readline
API.

The main way to utilize this readline editor is to replace the
`clojure.main/repl-read` behavior in `clojure.main/repl`.

The advantage of doing this is that it won't interfere with the input
stream if you are working on something that needs to read from
`*in*`. This is because the line-reader will only be engaged when the
REPL loop is reading.

Example:

```clojure
(clob.readline.core/with-line-reader
  (clob.readline.clojure.line-reader/create
    (clob.readline.clojure.service.local/create))
  (clojure.main/repl
     :prompt (fn []) ;; prompt is handled by line-reader
     :read (clob.readline.clojure.main/create-repl-read)))
```

Another option is to just wrap a call you your REPL with
`clob.readline.core/with-readline-in` this will bind `*in*` to an
input-stream that is supplied by the line reader.

```clojure
(clob.readline.core/with-readline-in
  (clob.readline.clojure.line-reader/create
    (clob.readline.clojure.service.local/create))
  (clojure.main/repl :prompt (fn[])))
```

Or with a fallback:

```clojure
(try
  (clob.readline.core/with-readline-in
    (clob.readline.clojure.line-reader/create
      (clob.readline.clojure.service.local/create))
    (clojure.main/repl :prompt (fn[])))
  (catch clojure.lang.ExceptionInfo e
    (if (-> e ex-data :type (= :clob.readline.jline-api/bad-terminal))
      (do (println (.getMessage e))
        (clojure.main/repl))
      (throw e))))
```

## Services

The line reader provides features like completion, documentation,
source, apropos, eval and more. The line reader needs a Service to
provide this functionality.

When you create a `clob.readline.clojure.line-reader`
you need to supply this service.

The more common service is the
`clob.readline.services.clojure.local` which uses the
local clojure process to provide this functionality and its a good
example of how a service works.

https://github.com/frenchy64/clob-shell/blob/main/readline/src/clob/readline/clojure/service/local.clj

In general, it's much better if the service is querying the Clojure process
where the eventual REPL eval takes place.

However, the service doesn't necessarily have to query the same
environment that the REPL is using for evaluation. All the editing
functionality that clob.readline provides works without an
environment to query. And the apropos, doc and completion functionality is
still sensible when you provide those abilities from the local clojure process.

This could be helpful when you have a Clojurey REPL process and you
don't have a Service for it. In this case you can just use a
`clojure.service.local` or a `clojure.service.simple` service. If you
do this you can expect less than optimal results but multi-line
editing, syntax highlighting, auto indenting will all work just fine.

## Key-bindings

**Bindings of interest**

* Ctrl-C => aborts editing the current line
* Ctrl-D at the start of a line => sends an end of stream message
  which in most cases should quit the REPL

* TAB => word completion or code indent if the cursor is in the whitespace at the
  start of a line
* Ctrl-X_Ctrl-D => Show documentation for word at point
* Ctrl-X_Ctrl-S => Show source for word at point
* Ctrl-X_Ctrl-A => Show apropos for word at point
* Ctrl-X_Ctrl-E => Inline eval for SEXP before the point

You can examine the key-bindings with the `:repl/key-bindings` command.

## Commands

There is a command system. If the line starts with a "repl" namespaced
keyword then the line-reader will attempt to interpret it as a command.

Type `:repl/help` or `:repl` TAB to see a list of available commands.

You can add new commands by adding methods to the
`clob.readline.commands/command` multimethod. You can add
documentation for the command by adding a method to the
`clob.readline.commands/command-doc` multimethod.

## nREPL, SocketREPL, pREPL?

Services have not been written for these REPLs yet!!

But you can use the `clob.readline.clojure.service.simple` service in the meantime.

## Contributing

Please contribute!

I'm trying to mark issues with `help wanted` for issues that I feel
are good opportunities for folks to help out. If you want to work on
one of these please mention it in the issue.

If you do contribute:

* if the change isn't small please file an issue before a PR.
* please put all PR changes into one commit
* make small grokable changes. Large changes are more likely to be
  ignored and or used as a starting issue for exploration.
* break larger solutions down into a logical series of small PRs
* mention it at the start, if you are filing a PR that is more of an
  exploration of an idea

I'm going to be more open to repairing current behavior than I will be
to increasing the scope of clob.readline.

I will have a preference for creating hooks so that additional functionality
can be layered on with libraries.

If you are wanting to contribute but don't know what to work on reach
out to me on the clojurians slack channel.

## License

Copyright © 2018 Bruce Hauman

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.