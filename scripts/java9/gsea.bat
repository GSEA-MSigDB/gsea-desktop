::Get the current batch file's short path

for %%x in (%0) do set BatchPath=%%~dpsx

for %%x in (%BatchPath%) do set BatchPath=%%~dpsx

start java --module-path=%BatchPath%\modules -Xmx4g @%BatchPath%\gsea.args --module org.gsea_msigdb.gsea/xapps.gsea.GSEA  %*
