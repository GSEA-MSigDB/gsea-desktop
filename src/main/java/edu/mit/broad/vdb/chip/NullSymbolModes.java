/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.vdb.chip;

import edu.mit.broad.genome.NamingConventions;
import edu.mit.broad.vdb.meg.Gene;

/**
 * @author Aravind Subramanian
 * @author eby
 */
public class NullSymbolModes {
    private static final String getTitle(String probeId, Gene gene) {
        return (_isNull(gene)) ? probeId : gene.getTitle();
    }

    private static final String getSymbol(String probeId, Gene gene) {
        return (_isNull(gene)) ? probeId : gene.getSymbol();
    }
    
    private static final boolean _isNull(Gene gene) {
        return (gene == null || NamingConventions.isNull(gene.getSymbol()));
    }

    public static final NullSymbolMode OmitNulls = new NullSymbolMode() {
        public final String getTitle(String probeId, Gene gene) {
            return NullSymbolModes.getTitle("", gene);
        }

        public final String getSymbol(String probeId, Gene gene) {
            return NullSymbolModes.getSymbol("", gene);
        }
    };

    public static final NullSymbolMode ReplaceWithProbeId = new NullSymbolMode() {
        public final String getTitle(String probeId, Gene gene) {
            return NullSymbolModes.getTitle(probeId, gene);
        }

        public final String getSymbol(String probeId, Gene gene) {
            return NullSymbolModes.getSymbol(probeId, gene);
        }
    };
}