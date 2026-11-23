/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.objects;

/**
 * One entry from an MSigDB GMT file catalog or CHIP catalog (the two share this same shape): a
 * single downloadable file belonging to one {@link MSigDBRelease}. Replaces the old
 * FTP-directory-listing-derived {@code FTPFile}; {@link #getPath()} now returns a directly usable
 * {@code https://} URL rather than a synthetic FTP host/dir/name string.
 *
 * @author David Eby
 */
public class MSigDBCatalogFile implements Versioned {
    private final String name;
    private final String description;
    private final String url;
    private final MSigDBVersion msigDBVersion;

    public MSigDBCatalogFile(String name, String description, String url, MSigDBVersion msigDBVersion) {
        this.name = name;
        this.description = description;
        this.url = url;
        this.msigDBVersion = msigDBVersion;
    }

    public String getName() { return name; }

    public String getDescription() { return description; }

    public String getUrl() { return url; }

    /** @return the download URL for this file, in the same role as {@code FTPFile.getPath()} previously. */
    public String getPath() { return url; }

    public MSigDBVersion getMSigDBVersion() { return msigDBVersion; }

    public void setMSigDBVersion(MSigDBVersion msigDBVersion) { throw new UnsupportedOperationException("Version cannot be changed"); }

    public String toString() { return name; }

    public boolean equals(Object obj) {
        if (!(obj instanceof MSigDBCatalogFile)) { return false; }
        return obj == this || url.equals(((MSigDBCatalogFile) obj).url);
    }

    public int hashCode() { return url.hashCode(); }
}
