@ECHO OFF
powershell -NoProfile -Command "$x=Get-CimInstance Win32_Process; $x | Where-Object { $_.Name -eq 'java.exe' -and $_.CommandLine -like '*mage-client-1.4.61.jar*' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }"
echo Previous XMage client instances closed.
IF NOT EXIST "C:\Program Files\Java\jre7\bin\" GOTO NOJAVADIR
set JAVA_HOME="C:\Program Files\Java\jre7\"
set CLASSPATH=%JAVA_HOME%/bin;%CLASSPATH%
set PATH=%JAVA_HOME%/bin;%PATH%
:NOJAVADIR
java -Xmx2000m -jar .\lib\mage-client-${project.version}.jar