@echo off
echo Starting Spring Boot Application with JMX...
echo.

java ^
  -Dcom.sun.management.jmxremote ^
  -Dcom.sun.management.jmxremote.port=7091 ^
  -Dcom.sun.management.jmxremote.authenticate=false ^
  -Dcom.sun.management.jmxremote.ssl=false ^
  -Djava.rmi.server.hostname=localhost ^
  -jar build/libs/*.jar

pause