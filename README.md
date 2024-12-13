
<img src="doc/img/logo/verticalversion.png" align="right" alt="clob" height="150px" style="border: none; float: right;">

# clob - Bash-like shell based on Clojure

[![Chat about Clob at Zulip](https://img.shields.io/badge/zulip-join_chat-brightgreen.svg)](https://clojurians.zulipchat.com/#narrow/stream/195273-clob) [![Join the chat at https://gitter.im/clob/Lobby](https://badges.gitter.im/clob/Lobby.svg)](https://gitter.im/clob/Lobby?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge) [![Build status on CircleCI](https://circleci.com/gh/dundalek/clob.svg?style=shield)](https://circleci.com/gh/dundalek/clob) [![GitHub release](https://img.shields.io/github/tag/dundalek/clob.svg?label=release&colorB=blue)](https://github.com/dundalek/clob/releases)

>  **Status update 2022**: Clob is now on hiatus.  
> Although the initial proof of concept turned out promising, it would take much more work to make it a robust tool that could be relied upon by a wider audience.  
> For now I recommend using [fish](https://fishshell.com/) as an interactive shell and [babashka](https://babashka.org/) for scripts.

Clob combines the best of traditional unix shells with the power of [Clojure](https://clojure.org/). It aims to be a modern alternative to bash.

Demo showing how to execute commands and using Clojure to manipulate outputs in shell:

![clob demo screencast](./doc/img/demo.gif)

Why try to reinvent bash?
- Bash has obscure syntax for non-trivial operations and lots of WTF moments.
- It treats everything as text while we mostly need to manipulate structured information.
- It is a large codebase which makes it difficult to hack on it and try innovative ideas. Which is one of the reasons why the shell did not improve much in recent decades.
- Traditional shells are limited in terms of presentation and discoverability, what if we could bring back richer environment as imagined by lisp machines?

Why shell based on Clojure(Script)?
- Clojure's has a simple syntax and well-thought design which makes it pleasurable to work with.
- Its extensive collection of powerful functions for data manipulation is suitable to provide solutions for daily tasks.
- Write shell scripts in a language you use daily for development so you don't have to google arcane shell constructs every time you need to do anything but simplest tasks.
- Less amount and more composable code allows to experiment with new features and ideas.

**Warning:** *Clob is still in a early stage and under a heavy development, has many rough edges and is subject to change a lot. Clob is tested on Linux, should run on macOS too.*

## Community and Contribution

If you have feedback about a specific feature or found a bug please open an [issue](https://github.com/dundalek/clob/issues).  
Use [reddit](https://reddit.com/r/clob) for general discussion and to share scripts and workflows.
Chat room is on [zulip](https://clojurians.zulipchat.com/#narrow/stream/195273-clob) or [gitter](https://gitter.im/clob/Lobby).

If you would like to contribute take look at [open issues](https://github.com/dundalek/clob/issues). Leave a comment if you find anything interesting  and we can improve the project together.

## Install

**Windows** proper is currently **NOT supported**, but it should run under WSL 2. If you know your way around with Windows, we need your help (see [#54](https://github.com/dundalek/clob/issues/54)).

**[Try clob online](https://repl.it/@dundalek/clob-playground)** in the browser without installing anything.

### Clojure/JVM version

Download the jar file from the [releases page](https://github.com/dundalek/clob/releases) and run it with:
```sh
java -jar clob-zero.jar
```

The jar file also contains a special header, so once you make it executable you can run it directly:
```sh
chmod +x clob-zero.jar
./clob-zero.jar
```

It can also run with `clojure` CLI:
```sh
clojure -Sdeps '{:deps {clob {:git/url "https://github.com/dundalek/clob.git" :tag "v0.5.0" :sha "6a7c0aa293616e2d28f7f735e915a301e44d2121"}}}' -m clob.zero.frontend.rebel
```

### ClojureScript/Lumo version

Install clob (requires [Node.js](https://nodejs.org/) version 9.x, support for version 10 is in progress, see [#113](https://github.com/dundalek/clob/issues/113)):
```
npm install -g clob
```

If you get a [permission error](https://github.com/dundalek/clob/issues/85) then try:
```
npm install -g clob --unsafe-perm
```

To install development version from master branch:
```
npm i -g dundalek/clob
```

## Quick Start

Start the shell:
```sh
clob
```

Run simple commands like you are used to:

```clojure
$ echo hi

$ git status

$ ls -l *.json
```

Commands starting with a parenthesis are evaluated as Clojure code:

```clojure
$ (+ 1 2)
; => 3
```

The power comes from combining shell commands and Clojure:

```clojure
$ echo hi | (clojure.string/upper-case)
; => HI

$ ls *.json |> (reverse)

; Count number of files grouped by first letter sorted by highest count first
$ ls |> (group-by first) | (map #(update % 1 count)) | (sort-by second) | (reverse)
```

If you like clob you can set it as your default shell.

Be careful and first test clob from other shell to make sure it works on your machine so you don't get locked out of shell (after `chsh` you need to log out and log back in for changes to take effect):
```sh
which clob | sudo tee -a /etc/shells
chsh -s $(which clob)
```

For the JVM version you can make it the default shell similarly like:
```sh
clob=/path/to/clob-zero.jar
chmod +x $clob
echo $clob | sudo tee -a /etc/shells
chsh -s $clob
```

## Documentation

- [Guide and Reference](./doc/guide.md) - Introduction to clob and basic configuration
- [Shell scripting](./doc/scripting.md) - Guide how to use Clob to write shell scripts
- [Cookbook](./doc/cookbook.md) - Recipes for integration of other tools like NVM, Autojump, etc.
- [Design Principles](./doc/principles.md) - Learn about the philosophy and what guides design decisions
- [Tech notes](./doc/tech.md) - Read about internals and architecture
- [Notes on Existing Shells](./doc/notes.md)
- [Changelog](./CHANGELOG.md)

## Roadmap

#### Terminal UI improvements and exploration

Explore innovate UI ideas, explore what a shell could become and all possibilities within an ASCII terminal. The goal is to reimagine what people think a command line interface is without having to lose its core power.

- [ ] Try to integrate [Liquid](https://github.com/mogenslund/liquid) as the editor interface, which would enable us:
  - [ ] Better and more flexible readline experience
  - [ ] Customizable key bindings
- [ ] Try  to explore [Trikl](https://github.com/lambdaisland/trikl) for building [interactive command-line interfaces](http://dundalek.com/entropic/combining-cli-and-gui/)
- [ ] Data helpers that automatically parse command output into data structures
- [ ] Automatic abbreviation suggestion
- [ ] Explore launcher functionality similar to Alfred, Lacona and others

#### More UI exploration

Explore if we could take shell power and functionality and lift it from the boundaries set by ASCII terminals.

- [ ] Structured graphical output ala [TermKit](https://github.com/unconed/TermKit) or [lisp machines](https://youtu.be/o4-YnLpLgtk?t=3m12s)
- [ ] Explore possibilty of a web interface

#### Stabilization and performance

I hope that new UI ideas above will get people excited and interested. After that we should work on stabilization and adding all the remaining features people are used to from traditional shells.

- [ ] Implement a low-level native pipeline library to improve performance
- [ ] Make it more robust and better error handling
- [ ] Job control

## Limitations

### JVM version (CLJ)

- [Abbreviations do not work](https://github.com/dundalek/clob/issues/151)
- Cannot redirect STDIO >= 3 (Java ProcessBuilder limitation)

### Lumo version (CLJS)

- No script mode
- No syntax highlighting
- [Prompt quirks](https://github.com/dundalek/clob/issues/71)
- Synchronous execution hacks (via deasync library)

## Development

Clone the repo and install dependencies

```
git clone git@github.com:dundalek/clob.git
cd clob
npm install
```

Run the cljs app
```
npm start
```

Run the clj app
```
clojure -m clob.zero.frontend.rebel
```

Run tests once
```
npm run test
```

Re-run tests on change
```
npm run test-auto
```

### Manual Java builds

Run `npm run pkg-java`. The resulting binary will be in `target/clob-zero.jar`.

## Sponsors

Thank you for the support:

- [AdGoji](https://www.adgoji.com/)

## Mentions

- [Hacker News](https://news.ycombinator.com/item?id=15600928)
- [root.cz](https://www.root.cz/clanky/softwarova-sklizen-19-12-2018/)

## Copyright & License

Copyright (c) Jakub Dundalek and contributors

Distributed under the Eclipse Public License 1.0 (same as Clojure).

Logo created by [@batarian71](https://github.com/batarian71) under [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/).
