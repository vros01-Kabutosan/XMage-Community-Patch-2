@ECHO OFF
call "%~dp0client\startClient.bat" %*
exit /b %ERRORLEVEL%
