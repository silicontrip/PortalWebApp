#!/bin/sh

javadoc -d /Users/mark/Developer/ingress-j2ee/portalApi/WebContent -classpath /Users/mark/Developer/ingress-j2ee/glassfish4/glassfish/lib/javaee.jar:../WEB-INF/lib/s2-geometry-java.jar:../WEB-INF/lib/json-java.jar:../build/classes/ *.java
