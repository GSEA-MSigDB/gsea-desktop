#!/bin/sh

#This script is intended for launching on Macs
#It may or may not work on *nix, definitely not on windows

#-Xdock:name again for Macs, sets the name in menu bar
#-Xmx4g indicates 4 gb of memory, adjust number up or down as needed
prefix=`dirname $(readlink $0 || echo $0)`
exec java --module-path="$prefix"/modules -Xmx4g \
    --patch-module=jide.common="$prefix"/lib/jide-components-3.7.4.jar:"$prefix"/lib/jide-dock-3.7.4.jar:"$prefix"/lib/jide-grids-3.7.4.jar \
    --add-exports=jide.common/com.jidesoft.grid=org.gsea-msigdb.gsea --add-exports=jide.common/com.jidesoft.docking=org.gsea-msigdb.gsea --add-exports=jide.common/com.jidesoft.status=org.gsea-msigdb.gsea \
    --add-exports=java.desktop/com.sun.java.swing.plaf.windows=jide.common --add-exports=java.desktop/javax.swing.plaf.synth=jide.common --add-exports=java.desktop/sun.swing=jide.common --add-exports=java.desktop/sun.awt=jide.common --add-exports=java.desktop/sun.awt.image=jide.common --add-exports=java.desktop/sun.awt.shell=jide.common --add-exports=java.desktop/sun.awt.dnd=jide.common --add-exports=java.desktop/sun.awt.windows=jide.common --add-exports=java.base/sun.security.action=jide.common \
    -Xdock:name="GSEA" \
	--module org.gsea-msigdb.gsea/xapps.gsea.GSEA "$@"
