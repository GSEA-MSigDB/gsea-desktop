/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.vdb.chip;

import edu.mit.broad.vdb.meg.Gene;

/**
 * A chip feature
 * <p/>
 * Many hugo might be null
 * Only guaranteed not null is getName()
 */
public interface Probe {

    public String getName();

    // yeah similarly can have multiple Hugos - but that is even more of a huh huh so completely preclude that
    public Gene getGene();

} // End interface Probe

