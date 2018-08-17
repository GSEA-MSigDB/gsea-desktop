#!/bin/sh

#This script is intended for launching on Macs
#It may or may not work on *nix, definitely not on windows

#-Xdock:name again for Macs, sets the name in menu bar
#-Xmx4g indicates 4 gb of memory, adjust number up or down as needed
prefix=`dirname $(readlink $0 || echo $0)`
exec java --module-path="$prefix"/modules -Xmx4g \
    @"$prefix"/gsea.args \
    -Xdock:name="GSEA" \
	--module org.broad.gsea/xapps.gsea.GSEA "$@"
