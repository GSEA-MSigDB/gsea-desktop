setlocal
::Get the current batch file's short path
for %%x in (%0) do set BatchPath=%%~dpsx
for %%x in (%BatchPath%) do set BatchPath=%%~dpsx

if exist %BatchPath%\jdk (
  echo "Using bundled JDK."
  set JAVA_HOME=%BatchPath%\jdk
  set JAVA_CMD=%BatchPath%\jdk\bin\javaw
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
::OpenJFX: run "gradlew syncJavaFxToModules" for local javafx-modules/, or use a dist that merges them into modules/.
if exist "%USERPROFILE%\.gsea\java_arguments" (
    start %JAVA_CMD% -showversion --module-path=%MODULE_PATH% --add-opens=javafx.graphics/com.sun.javafx.stage=org.gsea_msigdb.gsea -Xmx4g -Djava.awt.headless=false -Dgsea.ui=javafx @gsea.args -Djava.util.logging.config.file=logging.properties @"%USERPROFILE%\.gsea\java_arguments" --module=org.gsea_msigdb.gsea/xapps.gsea.GSEA  %*
) else (
    start %JAVA_CMD% -showversion --module-path=%MODULE_PATH% --add-opens=javafx.graphics/com.sun.javafx.stage=org.gsea_msigdb.gsea -Xmx4g -Djava.awt.headless=false -Dgsea.ui=javafx @gsea.args -Djava.util.logging.config.file=logging.properties --module=org.gsea_msigdb.gsea/xapps.gsea.GSEA  %*
)
