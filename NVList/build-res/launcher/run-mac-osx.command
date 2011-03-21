#!/bin/sh
cd "`dirname "$0"`"
java -jar ${jvm-args} ${jvm-args-mac64} "${project-name}.jar" @ARGS@
