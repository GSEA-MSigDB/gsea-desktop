/*
 * Copyright (c) 2003-2022 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.objects;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

public class MSigDBVersion {
    private static AtomicInteger unknownTrackingCounter = new AtomicInteger(0);
    
    public static MSigDBVersion createUnknownTrackingVersion(String id) {
        String trackingString = id + "-unknown-" + unknownTrackingCounter.incrementAndGet();
        return new MSigDBVersion(MSigDBSpecies.Unknown, trackingString) {
            @Override
            public boolean isUnknownVersion() { return true; }
        };
    }
    
    private final MSigDBSpecies msigDBSpecies;
    private final String versionString;
    private final DefaultArtifactVersion artifactVersion;

    public MSigDBVersion(MSigDBSpecies msigDBSpecies, String versionString) {
        if (msigDBSpecies == null || StringUtils.isBlank(versionString)) {
            throw new IllegalArgumentException("msigDBSpecies and versionString must not be null or blank.");
        }
        this.msigDBSpecies = msigDBSpecies;
        this.versionString = versionString;
        this.artifactVersion = new DefaultArtifactVersion(versionString);
    }

    public MSigDBSpecies getMsigDBSpecies() { return msigDBSpecies; }

    public String getVersionString() { return versionString; }

    public DefaultArtifactVersion getArtifactVersion() { return artifactVersion; }

    public boolean isUnknownVersion() { return false; }
    
    @Override
    public int hashCode() {
        return Objects.hash(msigDBSpecies, versionString);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) { return true; }
        if (obj == null) { return false; }
        if (getClass() != obj.getClass()) { return false; }
        MSigDBVersion other = (MSigDBVersion) obj;
        return msigDBSpecies == other.msigDBSpecies && Objects.equals(versionString, other.versionString);
    }
}
