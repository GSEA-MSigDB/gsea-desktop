/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xapps.api.vtools.viewers;

import edu.mit.broad.xbench.heatmap.DisplayState;
import edu.mit.broad.xbench.heatmap.GramImager;
import edu.mit.broad.xbench.heatmap.GramImagerImpl;
import org.genepattern.heatmap.ColorScheme;

/**
 * @author Aravind Subramanian
 */
public class VizFactory {

    public static GramImager createGramImager(ColorScheme cs) {
        return new GramImagerImpl(new DisplayState(cs));
    }

    public static GramImager createGramImager() {
        return new GramImagerImpl();
    }

} // End class VizFactory