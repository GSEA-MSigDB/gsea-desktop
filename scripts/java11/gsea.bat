setlocal
::Get the current batch file's short path
for %%x in (%0) do set BatchPath=%%~dpsx
for %%x in (%BatchPath%) do set BatchPath=%%~dpsx

if exist %BatchPath%\jdk-11 (
  echo "Using bundled JDK."
  set JAVA_HOME=%BatchPath%\jdk-11
  set JAVA_COM=%BatchPath%\jdk-11\bin\java
) else (
  echo "Bundled JDK not found.  Using system JDK."
  set JAVA_COM=java
)

start %JAVA_CMD% --module-path=%BatchPath%\modules -Xmx4g %BatchPath%\gsea.args --patch-module=jide.common=%BatchPath%\lib\jide-components-3.7.4.jar:%BatchPath%\lib\jide-dock-3.7.4.jar:%BatchPath%\lib\jide-grids-3.7.4.jar --module=org.gsea_msigdb.gsea/xapps.gsea.GSEA  %*
