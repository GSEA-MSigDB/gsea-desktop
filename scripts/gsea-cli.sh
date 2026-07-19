#!/bin/sh

#This script is intended for launch on *nix machines

#-Xmx4g indicates 4 gb of memory, adjust number up or down as needed
prefix=`dirname $(readlink -f $0 || echo $0)`

# Check whether or not to use the bundled JDK
if [ -d "${prefix}/jdk" ]; then
    echo echo "Using bundled JDK."
    JAVA_HOME="${prefix}/jdk"
    PATH=$JAVA_HOME/bin:$PATH
else
    echo "Using system JDK."
    java -version
fi

MODULE_PATH="${prefix}/modules"
if [ -d "${prefix}/javafx-modules" ]; then
    MODULE_PATH="${MODULE_PATH}:${prefix}/javafx-modules"
fi

# Check if there is a user-specified Java arguments file
# For more info, see the README at 
# https://raw.githubusercontent.com/GSEA-MSigDB/gsea-desktop/master/scripts/readme.txt
if [ -e "$HOME/.gsea/java_arguments" ]; then
    exec java --module-path="${MODULE_PATH}" -Xmx4g \
        @"${prefix}/gsea.args" \
        -Djava.awt.headless=true \
        -Djava.util.logging.config.file="${prefix}/logging.properties" \
        @"$HOME/.gsea/java_arguments" \
        --module=org.gsea_msigdb.gsea/xapps.gsea.CLI "$@"
else
    exec java --module-path="${MODULE_PATH}" -Xmx4g \
        @"${prefix}/gsea.args" \
        -Djava.awt.headless=true \
        -Djava.util.logging.config.file="${prefix}/logging.properties" \
        --module=org.gsea_msigdb.gsea/xapps.gsea.CLI "$@"
fi
