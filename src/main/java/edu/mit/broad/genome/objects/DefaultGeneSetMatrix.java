/*
 * Copyright (c) 2003-2022 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.objects;

import edu.mit.broad.genome.parsers.AuxUtils;

import java.util.*;

/**
 * Essentially a collection of (probably related) GeneSet
 * In addition has colors and icons and names
 * 
 * Lightweight container for a bunch of gsets -- gset data is not duplicated
 *
 * @author Aravind Subramanian, David Eby
 */
public class DefaultGeneSetMatrix extends AbstractGeneSetMatrix {
    public DefaultGeneSetMatrix(final String name, final GeneSet[] gsets) {
        initMatrix(name, gsets);
    }

    public DefaultGeneSetMatrix(final String name, final GeneSet[] gsets, final MSigDBVersion msigDBVersion, final boolean removeAuxStuff) {
        setMSigDBVersion(msigDBVersion);
        if (removeAuxStuff) {
            GeneSet[] cgsets = new GeneSet[gsets.length];
            for (int i = 0; i < gsets.length; i++) {
                cgsets[i] = gsets[i].cloneShallow(AuxUtils.getAuxNameOnlyNoHash(gsets[i].getName()));
            }
            initMatrix(name, cgsets);
        } else {
            initMatrix(name, gsets);
        }
    }

    public DefaultGeneSetMatrix(final String name, final List gsets, final MSigDBVersion msigDBVersion) {
        setMSigDBVersion(msigDBVersion);
        initMatrix(name, (GeneSet[]) gsets.toArray(new GeneSet[gsets.size()]));
    }
}
