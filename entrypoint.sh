#!/bin/bash
set -e
libreoffice "--accept=socket,host=127.0.0.1,port=2002;urp;StarOffice.ServiceManager" --headless &
sleep 1
java -jar /app/Mail2Print-1.0-SNAPSHOT-jar-with-dependencies.jar $@