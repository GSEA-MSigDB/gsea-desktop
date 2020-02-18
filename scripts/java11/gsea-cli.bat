setlocal

start jdk-11\bin\javaw -showversion --module-path=%BatchPath%\modules -Xmx4g -Djava.awt.headless=true @%BatchPath%\gsea.args --patch-module=jide.common=%BatchPath%\lib\jide-components-3.7.4.jar;%BatchPath%\lib\jide-dock-3.7.4.jar;%BatchPath%\lib\jide-grids-3.7.4.jar --module=org.gsea_msigdb.gsea/xapps.gsea.CLI  %*
