setlocal
::Get the current batch file's short path
for %%x in (%0) do set BatchPath=%%~dpsx
for %%x in (%BatchPath%) do set BatchPath=%%~dpsx

if exist %BatchPath%\jdk (
  echo "Using bundled JDK."
  set JAVA_HOME=%BatchPath%\jdk
  set JAVA_CMD=%BatchPath%\jdk\bin\java
) else (
  echo "Using system JDK."
  set JAVA_CMD=java
)

set MODULE_PATH=modules
if exist javafx-modules (
  set MODULE_PATH=modules;javafx-modules
)

::-Xmx4g indicates 4 gb of memory.
::To adjust this (or other Java options), edit the "%USERPROFILE%\.gsea\java_arguments" 
::file.  For more info, see the README at 
::https://raw.githubusercontent.com/GSEA-MSigDB/gsea-desktop/master/scripts/readme.txt 
::Foreground console java so CLI stdout/stderr and exit codes work.
if exist "%USERPROFILE%\.gsea\java_arguments" (
    "%JAVA_CMD%" --module-path=%MODULE_PATH% -Xmx4g -Djava.awt.headless=true -Djava.util.logging.config.file=logging.properties @gsea.args @"%USERPROFILE%\.gsea\java_arguments" --module=org.gsea_msigdb.gsea/xapps.gsea.CLI  %*
) else (
    "%JAVA_CMD%" --module-path=%MODULE_PATH% -Xmx4g -Djava.awt.headless=true -Djava.util.logging.config.file=logging.properties @gsea.args --module=org.gsea_msigdb.gsea/xapps.gsea.CLI  %*
)
exit /b %ERRORLEVEL%
