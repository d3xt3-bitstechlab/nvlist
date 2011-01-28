#!/bin/sh
java -jar ${jvm-args} ${jvm-args-mac64} "${project-name}.jar" -id "${project-name}" ${program-args}
