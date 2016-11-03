/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.vdb.chip;

import edu.mit.broad.genome.NamingConventions;
import edu.mit.broad.vdb.meg.Gene;

/**
 * @author Aravind Subramanian
 */
public class NullSymbolModes {

    public static class OmitNullSymbolMode implements NullSymbolMode {

        public OmitNullSymbolMode() {
        }

        public String getTitle(String probeId, Gene gene) {
            if (_isNull(gene)) {
                return null;
            } else {
                return gene.getTitle();
            }
        }

        public String getSymbol(String probeId, Gene gene) {
            if (_isNull(gene)) {
                return null;
            } else {
                return gene.getSymbol();
            }
        }

    }

    public static class ReplaceWithProbeIdMode implements NullSymbolMode {
        ReplaceWithProbeIdMode() {
        }

        public String getSymbol(String probeId, Gene gene) {
            if (_isNull(gene)) {
                return probeId;
            } else {
                return gene.getSymbol();
            }
        }

        public String getTitle(String probeId, Gene gene) {
            if (_isNull(gene)) {
                return probeId;
            } else {
                return gene.getTitle();
            }
        }
    }


    private static boolean _isNull(Gene gene) {
        if (gene == null || NamingConventions.isNull(gene.getSymbol())) {
            return true;
        } else {
            return false;
        }
    }

} // End class NullSymbolModes

