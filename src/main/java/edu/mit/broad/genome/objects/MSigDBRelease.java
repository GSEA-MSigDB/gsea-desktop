/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.objects;

/**
 * One entry from the MSigDB release catalog: a single MSigDB release for one species, together
 * with the URLs of its GMT file catalog and CHIP catalog.
 *
 * @author David Eby
 */
public class MSigDBRelease implements Versioned {
    private final String releaseName;
    private final String description;
    private final String releaseDate;
    private final String geneSetsCatalogUrl;
    private final String chipCatalogUrl;
    private final MSigDBVersion msigDBVersion;

    public MSigDBRelease(MSigDBSpecies species, String releaseName, String versionId, String description,
            String releaseDate, String geneSetsCatalogUrl, String chipCatalogUrl) {
        this.releaseName = releaseName;
        this.description = description;
        this.releaseDate = releaseDate;
        this.geneSetsCatalogUrl = geneSetsCatalogUrl;
        this.chipCatalogUrl = chipCatalogUrl;
        this.msigDBVersion = new MSigDBVersion(species, versionId);
    }

    public String getReleaseName() { return releaseName; }

    public String getDescription() { return description; }

    public String getReleaseDate() { return releaseDate; }

    public String getGeneSetsCatalogUrl() { return geneSetsCatalogUrl; }

    public String getChipCatalogUrl() { return chipCatalogUrl; }

    public MSigDBSpecies getSpecies() { return msigDBVersion.getMsigDBSpecies(); }

    public MSigDBVersion getMSigDBVersion() { return msigDBVersion; }

    public void setMSigDBVersion(MSigDBVersion msigDBVersion) { throw new UnsupportedOperationException("Version cannot be changed"); }

    public String toString() { return releaseName; }

    public boolean equals(Object obj) {
        if (!(obj instanceof MSigDBRelease)) { return false; }
        return obj == this || msigDBVersion.equals(((MSigDBRelease) obj).msigDBVersion);
    }

    public int hashCode() { return msigDBVersion.hashCode(); }
}
