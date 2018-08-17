#!/bin/sh

#This script is intended for launching on Macs
#It may or may not work on *nix, definitely not on windows

#apple.laf.useScreenMenuBar for Macs, to put menu bar at top of screen
#-Xdock:name again for Macs, sets the name in menu bar
#-Xmx4g indicates 4 gb of memory, adjust number up or down as needed
prefix=`dirname $(readlink $0 || echo $0)`
exec java -Xmx4g \
    -Xdock:name="GSEA" \
    -Dapple.laf.useScreenMenuBar=true \
    -jar "$prefix"/lib/gsea.jar "$@"
