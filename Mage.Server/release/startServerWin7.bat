@ECHO OFF
IF NOT EXIST "C:\Program Files (x86)\Java\jre7\bin\" GOTO NOJAVADIR
set JAVA_HOME="C:\Program Files (x86)\Java\jre7\"
set CLASSPATH=%JAVA_HOME%/bin;%CLASSPATH%
set PATH=%JAVA_HOME%/bin;%PATH%
:NOJAVADIR
set "SERVER_JAR="
for %%J in (".\lib\mage-server-*.jar") do set "SERVER_JAR=%%~fJ"
if not defined SERVER_JAR (
  echo No se encontro el JAR del servidor en .\lib
  exit /b 3
)
java -Xmx1024m -jar "%SERVER_JAR%"