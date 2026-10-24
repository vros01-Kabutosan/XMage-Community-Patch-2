@ECHO OFF
call "%~dp0server\startServer.bat" %*
exit /b %ERRORLEVEL%
