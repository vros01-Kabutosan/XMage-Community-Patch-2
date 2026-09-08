@ECHO OFF
IF NOT EXIST "C:\Program Files\Java\jre7\bin\" GOTO NOJAVADIR
set JAVA_HOME="C:\Program Files\Java\jre7\"
set CLASSPATH=%JAVA_HOME%/bin;%CLASSPATH%
set PATH=%JAVA_HOME%/bin;%PATH%
:NOJAVADIR
set "AI_DIAGNOSTICS=-Dmage.debug.printGameLogs=true -Dmage.debug.saveGameHistory=true"
echo [RC1] Diagnostico de IA: activado (logs de partidas e historial)
java %AI_DIAGNOSTICS% -Xmx1024m -jar ./lib/mage-server-${project.version}.jar
