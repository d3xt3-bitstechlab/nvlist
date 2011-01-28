#!/bin/sh
java -jar ${jvm-args} ${jvm-args-linux64} "${project-name}.jar" -id "${project-name}" ${program-args}
