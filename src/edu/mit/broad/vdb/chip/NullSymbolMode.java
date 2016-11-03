/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.vdb.chip;

import edu.mit.broad.vdb.meg.Gene;

/**
 * @author Aravind Subramanian
 */
public interface NullSymbolMode {

    public String getSymbol(String probeId, Gene gene);

    public String getTitle(String probeId, Gene gene);

}
