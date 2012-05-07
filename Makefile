
TARGET_JAR = analyzer.jar

RXTX_JAR = /usr/share/java/RXTXcomm.jar

SOURCES = $(wildcard src/org/sump/analyzer/*.java) $(wildcard src/org/sump/analyzer/tools/*.java) $(wildcard src/org/sump/analyzer/devices/*.java) $(wildcard src/org/sump/util/*.java)

JFLAGS = -cp $(RXTX_JAR)

jar:
	javac $(JFLAGS) $(SOURCES)
	cd src; jar cfm ../$(TARGET_JAR) Manifest.mf org
