/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package org.genepattern.uiutil;

public class FTPFile {

    private String fHost;

    private String fDir;

    private String fName;

    /**
     * Class constructor
     *
     * @param host
     * @param dir
     * @param name
     */
    public FTPFile(String host, String dir, String name) {
        this.fHost = host;
        this.fDir = dir;
        this.fName = name;
    }

    public String toString() {
        return fHost + ":/" + fDir + "/" + fName;
    }

    public String getPath() {
        return toString();
    }

    public String getName() {
        return fName;
    }

    public boolean equals(Object obj) {
        if (obj instanceof FTPFile) {
            return ((FTPFile) obj).toString().equals(toString());
        }

        return false;
    }

    public int hashCode() {
        return toString().hashCode();
    }

} // End class FTPFile
