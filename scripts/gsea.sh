#!/bin/sh

#This script is intended for launch on *nix machines

#-Xmx4g indicates 4 gb of memory, adjust number up or down as needed
prefix=`dirname $(readlink $0 || echo $0)`
exec java -Xmx4g \
    -Dapple.laf.useScreenMenuBar=true \
    -jar "$prefix"/lib/gsea.jar "$@"
