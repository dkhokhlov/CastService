#!/bin/bash -x
set -eu
mvn clean install dependency:copy-dependencies
echo
echo @@@@@@@ SUCCESS @@@@@@@@
echo
