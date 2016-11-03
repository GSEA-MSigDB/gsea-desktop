/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.heatmap;

import edu.mit.broad.genome.objects.Dataset;
import edu.mit.broad.genome.objects.Template;

import org.genepattern.heatmap.image.HeatMap;

/**
 * Factory implementation so that can switch b/w my table and gp's heat maps easily
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */

// not statics so that things are auto-thread safe
public interface GramImager {

    public HeatMap createBpogHeatMap(final Dataset ds);

    public HeatMap createBpogHeatMap(final Dataset ds, final Template t);

} // End GramImager
