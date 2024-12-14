# clob - Bash-like shell based on Clojure

Clob is a fork of [closh](https://github.com/dundalek/closh).

Clob is experimental and has no releases.

## Install

### Clojure/JVM version

```sh
npm run pkg-java
cd target
java -jar clob-zero.jar
```

The jar file also contains a special header, so once you make it executable you can run it directly:
```sh
chmod +x clob-zero.jar
./clob-zero.jar
```

It can also run with `clojure` CLI:
```sh
clojure -Sdeps '{:deps {com.ambrosebs/clob.shell {:git/url "https://github.com/frenchy64/clob-shell.git" :sha "cd1579f31dcd2ed5b655a149b177f8cd47aecb5d"}}}' -X:deps prep
clojure -Sdeps '{:deps {com.ambrosebs/clob.shell {:git/url "https://github.com/frenchy64/clob-shell.git" :sha "cd1579f31dcd2ed5b655a149b177f8cd47aecb5d"}}}' -M -m clob.zero.frontend.rebel
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

## Documentation

- [Guide and Reference](./doc/guide.md) - Introduction to clob and basic configuration
- [Shell scripting](./doc/scripting.md) - Guide how to use Clob to write shell scripts
- [Cookbook](./doc/cookbook.md) - Recipes for integration of other tools like NVM, Autojump, etc.
- [Design Principles](./doc/principles.md) - Learn about the philosophy and what guides design decisions
- [Tech notes](./doc/tech.md) - Read about internals and architecture
- [Notes on Existing Shells](./doc/notes.md)
- [Changelog](./CHANGELOG.md)

## Limitations

### JVM version (CLJ)

- [Abbreviations do not work](https://github.com/dundalek/closh/issues/151)
- Cannot redirect STDIO >= 3 (Java ProcessBuilder limitation)

## Development

Clone the repo and install dependencies

```
git clone git@github.com:frenchy64/clob-shell.git
cd clob
npm install
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

## Copyright & License

### Squarepeg

This software was written by Eric Normand and is released under the
[Eclipse Public License]. You can [find it on github][github].

[github]: http://github.com/ericnormand/squarepeg

[Eclipse Public License]: http://opensource.org/licenses/eclipse-1.0.php

### Rebel Readline

https://github.com/bhauman/rebel-readline

License
Copyright © 2018 Bruce Hauman

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.

### Closh

Copyright (c) Jakub Dundalek and contributors (2017-2022, accf238dbe099811c64e02e95a61f21e35238a0d -> 2b506c97c7756a5aa877feeee477efb6a6161956)

Distributed under the Eclipse Public License 1.0 (same as Clojure).

### Clob

Copyright (c) Ambrose Bonnaire-Sergeant (2024, 891df28c01db709fe0b4c0bc6fbef4cd351000df ->)
