#!/bin/sh

wd=$(dirname $0)

analyzer_jar=${wd}/analyzer.jar
tty=/dev/ttyACM0
nal=/usr/lib/jni

if [ ! -f "${tty}" ]
then
 cat<<EOF>&2
Error, device not found '${tty}'.
EOF
 exit 1
elif [ ! -d "${nal}" ]
then
 cat<<EOF>&2
Error, native library directory not found '${nal}'.
EOF
 exit 1
elif [ -f "${analyzer_jar}" ]
then
 java -jar ${analyzer_jar} -Dgnu.io.rxtx.SerialPorts="${tty}" -Djava.library.path="${nal}" $*

else
 cat<<EOF>&2
Error, file not found '${analyzer_jar}'
EOF
 exit 1
fi
