#!/bin/sh

CLIENT_JAR=$(find ./lib -maxdepth 1 -type f -name 'mage-client-*.jar' | head -n 1)
if [ -z "$CLIENT_JAR" ]; then
  echo "No se encontro el JAR del cliente en ./lib" >&2
  exit 3
fi
java -Xmx2000m -jar "$CLIENT_JAR" &