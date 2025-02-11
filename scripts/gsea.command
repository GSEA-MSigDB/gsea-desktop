#!/bin/sh

#This script is intended for launching on Macs
#It may or may not work on *nix, definitely not on windows

#-Xdock:name again for Macs, sets the name in menu bar
#-Xmx4g indicates 4 gb of memory, adjust number up or down as needed
prefix=`dirname $(readlink -f $0 || echo $0)`

# Check whether or not to use the bundled JDK
if [ -d "${prefix}/jdk" ]; then
    echo echo "Using bundled JDK."
    JAVA_HOME="${prefix}/jdk/Contents/Home"
    PATH=$JAVA_HOME/bin:$PATH
else
    echo "Bundled JDK not found.  Using system JDK."
    java -version
fi

# Check if there is a user-specified Java arguments file
# For more info, see the README at 
# https://raw.githubusercontent.com/GSEA-MSigDB/gsea-desktop/master/scripts/readme.txt
if [ -e "$HOME/.gsea/java_arguments" ]; then
    java -showversion --module-path="${prefix}/modules" -Xmx4g \
        @"${prefix}/gsea.args" \
        -Xdock:name="GSEA" \
        -Xdock:icon="${prefix}/icon_64x64.png" \
        -Djava.util.logging.config.file="${prefix}/logging.properties" \
        -Dapple.laf.useScreenMenuBar=true \
        @"$HOME/.gsea/java_arguments" \
        --module=org.gsea_msigdb.gsea/xapps.gsea.GSEA "$@"
else
    java -showversion --module-path="${prefix}/modules" -Xmx4g \
        @"${prefix}/gsea.args" \
        -Xdock:name="GSEA" \
        -Xdock:icon="${prefix}/icon_64x64.png" \
        -Djava.util.logging.config.file="${prefix}/logging.properties" \
        -Dapple.laf.useScreenMenuBar=true \
        --module=org.gsea_msigdb.gsea/xapps.gsea.GSEA "$@"
fi