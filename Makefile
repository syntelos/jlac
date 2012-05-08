
TARGET_JAR = analyzer.jar

MANIFEST_MF = Manifest.mf

MAIN_CLASS = org.sump.analyzer.Loader

RXTX_JAR = /usr/share/java/RXTXcomm.jar

CLASS_PATH = $(RXTX_JAR)

SOURCES = $(wildcard src/org/sump/analyzer/*.java) $(wildcard src/org/sump/analyzer/tools/*.java) $(wildcard src/org/sump/analyzer/devices/*.java) $(wildcard src/org/sump/util/*.java)

OBJECTS := $(SOURCES:.java=.class)

JFLAGS = -cp $(CLASS_PATH) -g

$(TARGET_JAR): $(SOURCES) src/$(MANIFEST_MF)
	javac $(JFLAGS) $(SOURCES)
	cd src; jar cfm ../$(TARGET_JAR) $(MANIFEST_MF) org

src/$(MANIFEST_MF): Makefile
	java -cp etc Manifest $(MAIN_CLASS) $(CLASS_PATH)

clean:
	$(RM) $(OBJECTS)

again:
	$(RM) $(TARGET_JAR)
	$(MAKE)
