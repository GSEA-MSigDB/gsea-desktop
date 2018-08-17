::Get the current batch file's short path
for %%x in (%0) do set BatchPath=%%~dpsx
for %%x in (%BatchPath%) do set BatchPath=%%~dpsx
start javaw -Xmx4g -jar %BatchPath%\lib\gsea.jar  %*
