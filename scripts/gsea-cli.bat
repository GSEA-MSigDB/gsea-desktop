setlocal

start jdk-17\bin\javaw -showversion --module-path=modules -Xmx4g -Djava.awt.headless=true -Djava.util.logging.config.file=logging.properties @gsea.args --patch-module=jide.common=lib\jide-components-3.7.4.jar;lib\jide-dock-3.7.4.jar;lib\jide-grids-3.7.4.jar --module=org.gsea_msigdb.gsea/xapps.gsea.CLI  %*
