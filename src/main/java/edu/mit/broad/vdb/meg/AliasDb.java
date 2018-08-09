/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.vdb.meg;

import edu.mit.broad.vdb.chip.Probe;

/**
 * @author Aravind Subramanian
 */
public interface AliasDb {

    public Probe[] getAliasesAsProbes() throws Exception;

} // End class IAliasDb
