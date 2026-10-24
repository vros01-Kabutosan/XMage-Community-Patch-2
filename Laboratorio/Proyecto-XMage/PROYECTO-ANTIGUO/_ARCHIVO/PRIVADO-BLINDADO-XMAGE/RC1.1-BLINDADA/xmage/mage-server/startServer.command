#!/bin/sh

cd "`dirname "$0"`"

java -Xmx1024m -jar ./lib/mage-server-1.4.61.jar
