/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.objects;

import edu.mit.broad.genome.parsers.AuxUtils;

import java.util.*;

/**
 * Essentially a collection of (probably related) GeneSet
 * In additin has colors and icons and names
 * <p/>
 * Lightweigth container for a bucng of gsets -- gset data is not duplicated
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class DefaultGeneSetMatrix extends AbstractGeneSetMatrix {

    /**
     * Class Constructor.
     * gsets specified are used directly without cloning any data
     * <p/>
     * gset names must be unique
     */
    public DefaultGeneSetMatrix(final String name, final GeneSet[] gsets) {
        initMatrix(name, gsets);
    }

    /**
     * Class constructor
     *
     * @param name
     * @param gsets
     * @param removeAuxStuff
     */
    public DefaultGeneSetMatrix(final String name, final GeneSet[] gsets, final boolean removeAuxStuff) {
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

    /**
     * Class constructor
     *
     * @param name
     * @param gsets
     */
    public DefaultGeneSetMatrix(final String name, final List gsets) {
        initMatrix(name, (GeneSet[]) gsets.toArray(new GeneSet[gsets.size()]));
    }

}    // End DefaultGeneSetMatrix
