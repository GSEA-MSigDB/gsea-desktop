setlocal

start jdk\bin\javaw -showversion --module-path=modules -Xmx4g @gsea.args -Djava.util.logging.config.file=logging.properties --module=org.gsea_msigdb.gsea/xapps.gsea.GSEA  %*
