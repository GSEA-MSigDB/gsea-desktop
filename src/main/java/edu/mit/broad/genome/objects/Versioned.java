/*
 * Copyright (c) 2003-2022 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.objects;

public interface Versioned {
    public MSigDBVersion getMSigDBVersion();

    public void setMSigDBVersion(MSigDBVersion msigDBVersion);
}
