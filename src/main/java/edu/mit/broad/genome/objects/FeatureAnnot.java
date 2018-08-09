/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.objects;

import edu.mit.broad.genome.TraceUtils;
import edu.mit.broad.genome.XLogger;
import edu.mit.broad.vdb.chip.Chip;
import edu.mit.broad.vdb.meg.Gene;
import org.apache.log4j.Logger;

/**
 * @author Aravind Subramanian
 */
public interface FeatureAnnot extends PersistentObject {

    public int getNumFeatures();

    // @note IMP any and all of these fields can be null
    // remember that the dataset need not even be gene based, so really everything can be null!!

    /**
     * The original desc for instance the desc in the gex dataset
     *
     * @return
     */
    public String getNativeDesc(final String featureName);

    public boolean hasNativeDescriptions();

    public String getGeneSymbol(final String featureName);

    public String getGeneTitle(final String featureName);

    public Chip getChip();

    public ColorMap.Rows getColorMap();

    public void setChip(final Chip chip, ColorMap.Rows cmr);

    static class Helper {

        private boolean fReportedError = false;

        private static final Logger klog = XLogger.getLogger(Helper.class);

        public Helper() {

        }

        public static void checkChip(final Chip current, final Chip newChip) {
            if (current != null && newChip != null) {
                if (!current.getName().equalsIgnoreCase(newChip.getName())) {
                    if ((newChip.getName().indexOf(current.getName()) == -1) && (current.getName().indexOf(newChip.getName()) == -1)) {
                        klog.warn("New chip: " + newChip.getName() + " does not match current: " + current.getName());
                        TraceUtils.showTrace();
                    }
                }
            }
        }

        public String getGeneSymbol(final String featureName, final Chip chip) {
            Gene gene = _hugo(featureName, chip);
            if (gene != null) {
                return gene.getSymbol();
            } else {
                return null;
            }
        }

        public String getGeneTitle(final String featureName, final Chip chip) {
            Gene gene = _hugo(featureName, chip);
            if (gene != null) {
                return gene.getTitle_truncated(); // @note trunc
            } else {
                return null;
            }
        }

        //Set errors = new HashSet();
        private Gene _hugo(final String featureName, final Chip chip) {
            if (chip == null) {
                return null;
            }

            // Keep checking - error or not but dont REPORT every time - just the one time
            try {
                return chip.getHugo(featureName);
            } catch (Throwable t) {
                //errors.add(featureName);
                //System.out.println("-----\n" + errors + "\n\n");
                if (!fReportedError) {
                    //TraceUtils.showTrace();
                    //t.printStackTrace();
                    klog.error(t.getMessage());
                    klog.error("Turning off subsequent error notifications");
                }
                fReportedError = true;
            }

            return null;
        }

    }

} // End interface FeatureAnnotation
