/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.objects;

import edu.mit.broad.genome.MismatchedSizeException;
import edu.mit.broad.genome.XLogger;
import edu.mit.broad.vdb.chip.Chip;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * @author Aravind Subramanian
 *         <p/>
 *         A sometimes CHIP-LESS implementation
 *         Useful because it shares row names with the dataset
 */
public class FeatureAnnotImpl extends AbstractObject implements FeatureAnnot {

    private Logger log = XLogger.getLogger(getClass());

    private Object fDataset_or_FeatureNamesList;

    private List fRowDescs;

    private Chip fChip;

    private Helper fHelper;

    private ColorMap.Rows fColorMap;

    /**
     * Class constructor
     *
     * @param name
     * @param rowNames
     * @param rowDescs_opt
     */
    public FeatureAnnotImpl(final String name,
                            final List rowNames,
                            final List rowDescs_opt,
                            final ColorMap.Rows cm_opt) {
        this.initHere(name, rowNames, rowDescs_opt, null, cm_opt);
    }

    public FeatureAnnotImpl(final String name,
                            final List rowNames,
                            final List rowDescs_opt) {
        this.initHere(name, rowNames, rowDescs_opt, null, null);
    }

    public FeatureAnnotImpl(final String name,
                            final List rowNames,
                            final List rowDescs_opt,
                            final Chip chip) {
        this.initHere(name, rowNames, rowDescs_opt, chip, null);
    }

    private void initHere(final String name,
                          final Object ds_or_rowNamesList,
                          final List rowDescs_opt,
                          final Chip chipOpt,
                          final ColorMap.Rows cm_opt) {
        if (ds_or_rowNamesList == null) {
            throw new IllegalArgumentException("Param ds cannot be null");
        }

        if (ds_or_rowNamesList instanceof Dataset) {
            Dataset ds = (Dataset) ds_or_rowNamesList;
            if (rowDescs_opt != null && ds.getNumRow() != rowDescs_opt.size()) {
                throw new MismatchedSizeException("dataset num rows", ds.getNumRow(), " row descs", rowDescs_opt.size());
            }

        } else {
            List rowNames = (List) ds_or_rowNamesList;
            if (rowDescs_opt != null && rowNames.size() != rowDescs_opt.size()) {
                throw new MismatchedSizeException("num rows", rowNames.size(), " row descs", rowDescs_opt.size());
            }
        }

        super.initialize(name);
        this.fDataset_or_FeatureNamesList = ds_or_rowNamesList;
        this.fRowDescs = rowDescs_opt;
        this.fChip = chipOpt;
        this.fColorMap = cm_opt;
        this.fHelper = new Helper();
    }

    public Chip getChip() {
        return fChip;
    }

    public ColorMap.Rows getColorMap() {
        return fColorMap;
    }

    public void setChip(final Chip chip, final ColorMap.Rows cmr) {
        if (chip != null) {
            Helper.checkChip(fChip, chip);
            this.fChip = chip;
            this.fColorMap = cmr;
        }
    }

    public String getQuickInfo() {
        if (fDataset_or_FeatureNamesList instanceof Dataset) {
            return ((Dataset) fDataset_or_FeatureNamesList).getQuickInfo();
        } else {
            return null;
        }
    }

    public int getNumFeatures() {
        if (fDataset_or_FeatureNamesList instanceof Dataset) {
            return ((Dataset) fDataset_or_FeatureNamesList).getNumRow();
        } else {
            List list = (List) fDataset_or_FeatureNamesList;
            return list.size();
        }
    }

    public boolean hasNativeDescriptions() {
        return fRowDescs != null && !fRowDescs.isEmpty();
    }

    public String getNativeDesc(final String featureName) {

        if (fRowDescs == null) {
            return null;
        }

        int index;

        if (fDataset_or_FeatureNamesList instanceof Dataset) {
            index = ((Dataset) fDataset_or_FeatureNamesList).getRowIndex(featureName);
        } else {
            List list = (List) fDataset_or_FeatureNamesList;
            index = list.indexOf(featureName);
        }

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

} // End class FeatureAnnotationDatasetImpl

