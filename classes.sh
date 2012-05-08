#!/bin/bash

wd=$(dirname $(readlink -f $0))

find ${wd}/src -type f -name '*.class'
