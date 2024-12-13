with import <nixpkgs> {};
stdenv.mkDerivation {
  name = "clob-env";
  buildInputs = [
    bash
    clojure
    git
    graalvm11-ce
    which
  ];
}

