@ECHO OFF
if not defined XMAGE_SERVER_GUARD set XMAGE_SERVER_GUARD=1
for /f "delims=" %%P in ('powershell -NoProfile -Command "$c=Get-NetTCPConnection -LocalPort 17171 -State Listen -ErrorAction SilentlyContinue; $c.OwningProcess"' ) do (
  echo XMage server already listening on port 17171, PID %%P
  echo Close it before starting another server.
  exit /b 2
)
IF NOT EXIST "C:\Program Files\Java\jre7\bin\" GOTO NOJAVADIR
set JAVA_HOME="C:\Program Files\Java\jre7\"
set CLASSPATH=%JAVA_HOME%/bin;%CLASSPATH%
set PATH=%JAVA_HOME%/bin;%PATH%
:NOJAVADIR
set "AI_DIAGNOSTICS=-Dmage.debug.printGameLogs=true -Dmage.debug.saveGameHistory=true"
echo [RC1] Diagnostico de IA: activado (logs de partidas e historial)
java %AI_DIAGNOSTICS% -Xmx1024m -jar ./lib/mage-server-${project.version}.jar
