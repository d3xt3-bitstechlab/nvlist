#!/bin/sh
java -jar ${jvm-args} ${jvm-args-linux32} "${project-name}.jar" -id "${project-name}" ${program-args}
