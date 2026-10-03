@ECHO OFF
powershell -NoProfile -Command "$x=Get-CimInstance Win32_Process; $x | Where-Object { $_.Name -eq 'java.exe' -and $_.CommandLine -like '*mage-client-*.jar*' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }"
echo Previous XMage client instances closed.
timeout /t 3 /nobreak >nul
IF NOT EXIST "C:\Program Files\Java\jre7\bin\" GOTO NOJAVADIR
set JAVA_HOME="C:\Program Files\Java\jre7\"
set CLASSPATH=%JAVA_HOME%/bin;%CLASSPATH%
set PATH=%JAVA_HOME%/bin;%PATH%
:NOJAVADIR
set "CLIENT_JAR="
for %%J in (".\lib\mage-client-*.jar") do set "CLIENT_JAR=%%~fJ"
if not defined CLIENT_JAR (
  echo No se encontro el JAR del cliente en .\lib
  exit /b 3
)
java -Xmx2000m -jar "%CLIENT_JAR%"