/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.vdb.chip;

/**
 * @author Aravind Subramanian
 */
public class ChipHelper {

    // Seperate method here to that the opt below can be done
    public static Chip createComboChip(final Chip[] chips) throws Exception {
        if (chips == null || chips.length == 0) {
            throw new IllegalArgumentException("Parameter chips cannot be null or zero length");
        } else if (chips.length == 1) {
            return chips[0];
        } else {
            return new FileInMemoryChip(chips);
        }
    }

}
