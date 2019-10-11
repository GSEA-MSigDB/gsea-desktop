/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.objects;

import edu.mit.broad.genome.MismatchedSizeException;
import edu.mit.broad.genome.TraceUtils;
import edu.mit.broad.vdb.chip.Chip;
import edu.mit.broad.vdb.meg.Gene;

import org.apache.log4j.Logger;

import java.util.List;

/**
 * @author Aravind Subramanian
 *         <p/>
 *         A sometimes CHIP-LESS implementation
 *         Useful because it shares row names with the dataset
 */
public class FeatureAnnot extends AbstractObject {

    public static class Helper {
    
        private boolean fReportedError = false;
    
        private static final Logger klog = Logger.getLogger(Helper.class);
    
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
            return (gene != null) ? gene.getSymbol() : null;
        }
    
        public String getGeneTitle(final String featureName, final Chip chip) {
            Gene gene = _hugo(featureName, chip);
            return (gene != null) ? gene.getTitle_truncated() : null; // @note trunc
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

    private Logger log = Logger.getLogger(getClass());

    private List fFeatureNamesList;

    private List fRowDescs;

    protected Chip fChip;

    protected Helper fHelper;

    public FeatureAnnot(final String name,
            final List rowNames,
            final List rowDescs_opt) {
        this(name, rowNames, rowDescs_opt, null);
    }

    public FeatureAnnot(final String name,
            final List rowNames,
            final List rowDescs_opt,
            final Chip chip_opt) {
        if (rowNames == null) {
            throw new IllegalArgumentException("Param rowNames cannot be null");
        }

        if (rowDescs_opt != null && rowNames.size() != rowDescs_opt.size()) {
            throw new MismatchedSizeException("num rows", rowNames.size(), " row descs", rowDescs_opt.size());
        }

        super.initialize(name);
        this.fFeatureNamesList = rowNames;
        this.fRowDescs = rowDescs_opt;
        this.fChip = chip_opt;
        this.fHelper = new FeatureAnnot.Helper();
    }

    public Chip getChip() {
        return fChip;
    }

    public void setChip(final Chip chip) {
        if (chip != null) {
            Helper.checkChip(fChip, chip);
            this.fChip = chip;
        }
    }

    public String getQuickInfo() {
        return null;
    }

    public int getNumFeatures() {
        return fFeatureNamesList.size();
    }

    public boolean hasNativeDescriptions() {
        return fRowDescs != null && !fRowDescs.isEmpty();
    }

    public String getNativeDesc(final String featureName) {

        if (fRowDescs == null) {
            return null;
        }

        int index = fFeatureNamesList.indexOf(featureName);
        if (index == -1) {
            log.warn("No such such feature: >" + featureName + "< " + getName());
            return null;
        } else {
            return fRowDescs.get(index).toString();
        }
    }

    public String getGeneSymbol(final String featureName) {
        return fHelper.getGeneSymbol(featureName, fChip);
    }

    public String getGeneTitle(final String featureName) {
        return fHelper.getGeneTitle(featureName, fChip);
    }

}