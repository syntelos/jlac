
Sump's Logic Analyzer Client

 See http://www.seeedstudio.com/depot/preorder-open-workbench-logic-sniffer-p-612.html?cPath=75

Introduction

  http://www.sump.org/projects/analyzer/client/
  http://github.com/syntelos/jlac

  This is a modified copy of JLAC from the work of Michael Poppitz and Benjamin Vedder.

Ubuntu Linux Installation

  sudo apt-get install librxtx-java
  make
  ./analyzer.sh

  This is known to work on Gnu Linux / Ubuntu.

For other platforms

  Edit the Makefile for the location of RXTXcomm.jar, make, install
  the RXTX native library, and create a script after following...

    analyzer_jar=${HOME}/src/jlac/analyzer.jar

    java -Dgnu.io.rxtx.SerialPorts="/dev/ttyACM0" -Djava.library.path="/usr/lib/jni/" -jar ${analyzer_jar} $*

License

  Copyright (C) 2006 Michael Poppitz
  Copyright (C) 2012 John Pritchard
 
  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or (at
  your option) any later version.

  This program is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  General Public License for more details.

  You should have received a copy of the GNU General Public License along
  with this program; if not, write to the Free Software Foundation, Inc.,
  51 Franklin St, Fifth Floor, Boston, MA 02110, USA
