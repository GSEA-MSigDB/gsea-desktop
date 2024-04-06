setlocal

start jdk\bin\javaw -showversion --module-path=modules -Xmx4g -Djava.awt.headless=true -Djava.util.logging.config.file=logging.properties @gsea.args --module=org.gsea_msigdb.gsea/xapps.gsea.CLI  %*
