#!/bin/bash -x
set -eu
scalac -classpath ./target/classes -d ./target/classes src/main/scala/castservice/Client.scala
echo
echo @@@@@@@ SUCCESS @@@@@@@@
echo
