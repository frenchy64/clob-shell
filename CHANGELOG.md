# Changelog

## [master](https://github.com/dundalek/clob/compare/v0.5.0...master) (unreleased)

### New features
### Fixes
### Other changes

## [0.5.0](https://github.com/dundalek/clob/compare/v0.4.1...v0.5.0) (2020-06-01)


### New features

- JVM version: Improved history storage in (stores it in sqlite db same as the lumo version)
- JVM version: Implemented alias expansion by [@djblue](https://github.com/djblue) ([#150](https://github.com/dundalek/clob/pull/150))
- JVM version: Make abbreviations work (by treating them as same as aliases for now)
- Add support for `cd -` go to previous directory by [@kirillsalykin](https://github.com/kirillsalykin) ([#167](https://github.com/dundalek/clob/pull/167))

### Fixes

- Fix tab completions when launching clob via clojure CLI by [@djblue](https://github.com/djblue) ([#148](https://github.com/dundalek/clob/pull/148))
- Fix typo in expand-abbreviation by [@djblue](https://github.com/djblue) ([#149](https://github.com/dundalek/clob/pull/149))
- Fix when argument to cd is a number ([#153](https://github.com/dundalek/clob/issues/153))
- Fix ignoring updates to PATH when executing binaries (JVM)
- Fix shell completion on NixOS by [@johannesloetzsch](https://github.com/johannesloetzsch) ([#163](https://github.com/dundalek/clob/pull/163))
- Fix globbing implementation by [@kirillsalykin](https://github.com/kirillsalykin) ([#164](https://github.com/dundalek/clob/pull/164))

### Other changes

- Improved reader customization. It no longer depends on a fork of `tools.reader` but it uses upstream version.
- Some progress on GraalVM port using SCI (thanks to [@borkdude](https://github.com/borkdude) and [@jeroenvandijk](https://github.com/jeroenvandijk)). But does it is not fully supported yet.

## [0.4.1](https://github.com/dundalek/clob/compare/v0.4.0...v0.4.1) (2019-08-10)

This is a bugfix release containing fixes mostly around current working directory as well as fixing compatibility with Java 11.

### New features

- Add unsetenv builtin ([#140](https://github.com/dundalek/clob/issues/140))

### Fixes

- Fix cd which broke with Java 11 ([#144](https://github.com/dundalek/clob/issues/144))
- Fix cd does not return success value ([#70](https://github.com/dundalek/clob/issues/70))
- Fix slurp to respect cwd ([#126](https://github.com/dundalek/clob/issues/126))
- Fix metadata handling that affected dynamic vars ([#146](https://github.com/dundalek/clob/issues/146))
- Fix expansion for commands ([#130](https://github.com/dundalek/clob/issues/130))

## [0.4.0](https://github.com/dundalek/clob/compare/v0.3.3...v0.4.0) (2019-04-07)

### New features

- Add support for script mode based on clojure.main

### Other changes

- Update Clojure to 1.10
- Use fully qualified names for emitted commands
- Fix bugs in sequential execution

## [0.3.3](https://github.com/dundalek/clob/compare/v0.3.2...v0.3.3) (2019-01-02)

- Fix displaying completions provided by zsh ([#115](https://github.com/dundalek/clob/issues/115))
- Fix redirection issues from functions in pipeline ([#62](https://github.com/dundalek/clob/issues/62))

## [0.3.2](https://github.com/dundalek/clob/compare/v0.3.1...v0.3.2) (2018-12-11)

- Fix crash when command line args are passed

## [0.3.1](https://github.com/dundalek/clob/compare/v0.3.0...v0.3.1) (2018-12-06)

- Make jar executable
- Update npm dependencies to prevent installation issues

## [0.3.0](https://github.com/dundalek/clob/compare/v0.2.2...v0.3.0) (2018-12-06)

### New features

- Add JVM support ([#66](https://github.com/dundalek/clob/issues/66))
- Integrate rebel-readline

### Other changes

- Renamed `clob` namespace to `clob.zero`
  *Same reasoning like `clojure.spec.alpha`, but it feels strange to have `alpha` stick for many years hence `zero`.*

## [0.2.2](https://github.com/dundalek/clob/compare/v0.2.1...v0.2.2) (2018-07-30)

- Bump up deasync dependency
- Upgrade lumo to 1.9.0-alpha which should fix installation issues

## [0.2.1](https://github.com/dundalek/clob/compare/v0.2.0...v0.2.1) (2018-07-04)

- Add node version check to the install script to prevent people from running into issues
- Add a logo
- Internal code cleanups

## [0.2.0](https://github.com/dundalek/clob/compare/v0.1.0...v0.2.0) (2018-04-16)

### New features

- Environment variable integration ([#16](https://github.com/dundalek/clob/issues/16))
- Persistent history ([#23](https://github.com/dundalek/clob/pull/23))
- Tab-completion ([#6](https://github.com/dundalek/clob/issues/6))
- Improved reader ([#39](https://github.com/dundalek/clob/issues/39))
- Signal control ([#30](https://github.com/dundalek/clob/issues/30))
- Aliases ([#12](https://github.com/dundalek/clob/issues/12))

### Bugfixes

- Many

### Changes

- Upgrade Lumo to 1.8.0 and install it as local dependency

## 0.1.0 (2017-10-31)

Initial version with following features:

- Command execution
- Pipes
- IO Redirects
- Interactive mode REPL
- Dynamic prompt
- Load `~/.clobrc` on startup
