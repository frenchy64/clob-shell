#!/bin/bash

set -x

CLOB_CMD=./clob-zero-sci
#CLOB_CMD="java -jar target/clob-zero-sci.jar"

$CLOB_CMD --version
$CLOB_CMD -e "(+ 1 2)"
$CLOB_CMD -e "date"
$CLOB_CMD -e 'echo "hi" | (clojure.string/upper-case)'
$CLOB_CMD fixtures/script-mode-tests/cond.cljc
$CLOB_CMD fixtures/script-mode-tests/bar.cljc
echo "(+ 1 2)" | $CLOB_CMD -
echo date | $CLOB_CMD -
$CLOB_CMD -h

# expect -c "
# spawn $CLOB_CMD;
# send \"(println :hello-word)\r\";
# send \"(exit)\r\";
# expect eof;
# "
