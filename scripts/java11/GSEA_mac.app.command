#!/bin/sh

#This script is intended for launching on Macs
#It is intended to be compiled be 'shc' on Mac in order to have the
#launcher be a binary executable rather than a script.  You can obtain
#'sch' through MacPorts or Homebrew.  Then, the steps to compile look
#like this:
#  $ export CFLAGS=-mmacosx-version-min=10.10   # Pass-thru to 'cc' for backward compatibility.
#  $ shc -r -f GSEA_mac.app.command  # The '-r' means "relaxed security" and is more portable.
#  $ mv GSEA_mac.app.sh GSEA   # Rename the executable to the expected name.

#-Xdock:name again for Macs, sets the name in menu bar
#-Xmx4g indicates 4 gb of memory, adjust number up or down as needed
prefix=`dirname $(readlink -f $0 || echo $0)`

# Check whether or not to use the bundled JDK
if [ -d "${prefix}/../jdk-11" ]; then
    echo echo "Using bundled JDK."
    JAVA_HOME="${prefix}/../jdk-11"
    PATH=$JAVA_HOME/bin:$PATH
else
    echo "Using system JDK."
    java -version
fi

exec java -showversion --module-path="${prefix}/../Java/modules" -Xmx4g \
    @"${prefix}/../Java/gsea.args" \
    --patch-module="jide.common=${prefix}/../Java/lib/jide-components-3.7.4.jar:${prefix}/../Java/lib/jide-dock-3.7.4.jar:${prefix}/../Java/lib/jide-grids-3.7.4.jar" \
    -Xdock:name="GSEA" \
    -Xdock:icon="${prefix}/../Resources/icon_64x64.png" \
    -Dapple.laf.useScreenMenuBar=true \
	--module=org.gsea_msigdb.gsea/xapps.gsea.GSEA -NSRequiresAquaSystemAppearance true
