/*
 * Copyright (c) 2003-2022 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package org.genepattern.uiutil;

import edu.mit.broad.genome.objects.MSigDBVersion;
import edu.mit.broad.genome.objects.Versioned;

public class FTPFile implements Versioned {
    private final String fHost;
    private final String fDir;
    private final String fName;
    private final String toString;
    private final MSigDBVersion msigDBVersion;

    public FTPFile(String host, String dir, String name, MSigDBVersion msigDBVersion) {
        this.fHost = host;
        this.fDir = dir;
        this.fName = name;
        this.toString = fHost + ":/" + fDir + "/" + fName;
        this.msigDBVersion = msigDBVersion;
    }

    public String toString() { return toString; }

    public String getPath() { return toString; }

    public String getName() { return fName; }

    public MSigDBVersion getMSigDBVersion() { return msigDBVersion; }

    public void setMSigDBVersion(MSigDBVersion msigDBVersion) { throw new UnsupportedOperationException("Version cannot be changed"); }
    
    public boolean equals(Object obj) {
        if (obj instanceof FTPFile) {
            return obj == this || toString.equals(obj.toString());
        }
        return false;
    }

    public int hashCode() { return toString.hashCode(); }
}
