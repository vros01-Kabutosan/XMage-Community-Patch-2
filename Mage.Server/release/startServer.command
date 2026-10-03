#!/bin/sh

cd "`dirname "$0"`"

SERVER_JAR=$(find ./lib -maxdepth 1 -type f -name 'mage-server-*.jar' | head -n 1)
if [ -z "$SERVER_JAR" ]; then
  echo "No se encontro el JAR del servidor en ./lib" >&2
  exit 3
fi
java -Xmx1024m -jar "$SERVER_JAR"
