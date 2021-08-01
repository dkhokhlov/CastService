#!/bin/bash
scala -classpath ./target/classes castservice.ScalaClient $*
echo $?
