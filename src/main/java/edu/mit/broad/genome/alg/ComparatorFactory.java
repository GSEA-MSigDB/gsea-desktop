/*
 * Copyright (c) 2003-2022 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.alg;

import java.util.Comparator;

import org.apache.commons.io.FilenameUtils;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.genepattern.uiutil.FTPFile;

import edu.mit.broad.genome.math.Order;
import edu.mit.broad.genome.objects.MSigDBVersion;
import edu.mit.broad.genome.objects.PersistentObject;
import edu.mit.broad.genome.objects.ScoredDataset;
import edu.mit.broad.genome.objects.esmatrix.db.EnrichmentResult;

/**
 * Collection of useful comparators
 *
 * @author Aravind Subramanian, David Eby
 */
public class ComparatorFactory {
    private ComparatorFactory() { }

    public static class EnrichmentResultByNESComparator implements Comparator<EnrichmentResult> {
        Order fOrder;
        final int byOrderLess;
        final int byOrderMore;

        public EnrichmentResultByNESComparator(final Order order) {
            this.fOrder = order;
            byOrderLess = fOrder.isAscending() ? -1 : +1;
            byOrderMore = fOrder.isAscending() ? +1 : -1;
        }

        public int compare(final EnrichmentResult result1, final EnrichmentResult result2) {
            if ((result1 == null) && (result2 == null)) {
                return 0;
            } else if (result1 == null) {
                return byOrderLess;    // null is always least
            } else if (result2 == null) {
                return byOrderMore;    // null is always least
            }

            final float nes1 = result1.getScore().getNES();
            final float nes2 = result2.getScore().getNES();

            // TODO: Here is the location for the key change to dealing with FP NaN issues.
            // NaN will natively sort as the *greatest* value among the floats, but we don't
            // want it to land at the top of our report summary list.  Instead, we will sort
            // it as the *least* value.
            // Thus, the sign of the comparison results should be reversed here.
            // Also, if we don't flag these elsewhere they should definitely be flagged here.
            // TODO: Also handle Infinity in the same way.
            // That gives us three different types of special case FP numbers to handle (NaN,
            // +Infinity, -Infinity).  We don't really care about the order of these in the list
            // as they will all be flagged and handled in the same way, but for the sake of
            // a stable / repeatable ordering we will use:
            //     NaN > +Infinity > -Infinity
            final boolean isNaN1 = Float.isNaN(nes1);
            final boolean isNaN2 = Float.isNaN(nes2);
            if (isNaN1 && isNaN2) {
                return 0;
            } else if (isNaN1) {
                return byOrderLess;
            } else if (isNaN2) {
                return byOrderMore;
            }

            if (nes1 == nes2) {
                return 0;
            } else if (nes1 < nes2) {
                return byOrderLess;
            }
            return byOrderMore;
        }

        public boolean equals(Object o2) {
            return false;
        }
    }

    public static class PobComparator implements Comparator<PersistentObject> {
        public int compare(PersistentObject pob1, PersistentObject pob2) {
            return pob1.getName().compareTo(pob2.getName());
        }

        public boolean equals(Object o2) {
            return false;
        }
    }

    public static class FileExtComparator implements Comparator<String> {
        public int compare(String pn1, String pn2) {
            String ext1 = FilenameUtils.getExtension(pn1);
            String ext2 = FilenameUtils.getExtension(pn2);

            return ext1.compareTo(ext2);
        }

        public boolean equals(Object o2) {
            return false;
        }
    }

    public static class ScoredDatasetScoreComparator implements Comparator<String> {
        final ScoredDataset fSds;

        public ScoredDatasetScoreComparator(ScoredDataset sds) {
            this.fSds = sds;
        }

        public int compare(String pn1, String pn2) {

            int rank1 = fSds.getRank(pn1);
            int rank2 = fSds.getRank(pn2);

            if (rank1 == -1 || rank2 == -1) {
                throw new IllegalArgumentException("Specified label not in the sds: " + pn1 + " " + pn2);
            }

            return rank1 - rank2;
        }

        public boolean equals(Object o2) {
            return false;
        }
    }

    public static Comparator<PersistentObject> PERSISTENT_OBJECT_BY_NAME = new Comparator<PersistentObject>() {
        @Override
        public int compare(PersistentObject o1, PersistentObject o2) {
            if (o1 == null) return -1;
            if (o2 == null) return 1;
            return o1.getName().compareTo(o2.getName());
        }
    };
    
    public static class FTPFileByVersionComparator implements Comparator<FTPFile> {
        // Used to track highest version seen by this instance.  Use a fake lowest-possible version
        // for the initial comparison.
        private String highestVersionId;
        private DefaultArtifactVersion highestVersion = new DefaultArtifactVersion("v0.0");
        private final String preferredPrefix;

        public String getHighestVersionId() { return highestVersionId; }

        public FTPFileByVersionComparator() { this.preferredPrefix = null; }

        public FTPFileByVersionComparator(String preferredPrefix) { this.preferredPrefix = preferredPrefix; }
        
        /**
         * Return -1 if o1 is less than o2, 0 if they're equal, +1 if o1 is greater than o2.
         */
        public int compare(FTPFile ftpFile1, FTPFile ftpFile2) {
            MSigDBVersion msigDBVersion1 = ftpFile1.getMSigDBVersion();
            MSigDBVersion msigDBVersion2 = ftpFile2.getMSigDBVersion();
            if (msigDBVersion1 == null) { return (msigDBVersion2 == null) ? 0 : -1; }
            if (msigDBVersion2 == null) { return 1; }
            
            DefaultArtifactVersion version1 = msigDBVersion1.getArtifactVersion();
            DefaultArtifactVersion version2 = msigDBVersion2.getArtifactVersion();

            if (!version1.equals(version2)) {
                int compareTo = version2.compareTo(version1);
                if (compareTo < 0) {
                    if (highestVersion.compareTo(version1) < 0) {
                        highestVersion = version1;
                        highestVersionId = msigDBVersion1.getVersionString();
                    }
                } else {
                    if (highestVersion.compareTo(version2) < 0) {
                        highestVersion = version2;
                        highestVersionId = msigDBVersion2.getVersionString();
                    }
                }
                return compareTo;
            }

            if (highestVersion.compareTo(version1) < 0) {
                // Doesn't matter which we use since they are equal
                highestVersion = version1;
                highestVersionId = msigDBVersion1.getVersionString();
            }
            
            // Optional preferredPrefix check.  Items starting with the preferredPrefix
            // get sorted above the others.
            String s1 = ftpFile1.getName();
            String s2 = ftpFile2.getName();
            if (preferredPrefix != null) {
                final boolean s1HasPP = s1.startsWith(preferredPrefix);
                final boolean s2HasPP = s2.startsWith(preferredPrefix);
                if (s1HasPP && !s2HasPP) { return -1; }
                if (!s1HasPP && s2HasPP) { return 1; }
            }

            // now just string comparison
            return s1.compareTo(s2);
        }

        public boolean equals(Object o2) { return false; }
    }
}
