/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xtools.api.param;

import edu.mit.broad.genome.objects.Dataset;
import xtools.api.AbstractTool;

import java.awt.event.ActionListener;

/**
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class DatasetReqdParam extends PobParam implements ActionListener {
    /**
     * Class constructor
     */
    public DatasetReqdParam() {
        this(RES, RES_ENGLISH, RES_DESC);
    }

    public DatasetReqdParam(final String name, final String nameEnglish, final String desc) {
        super(name, nameEnglish, Dataset.class, desc, new Dataset[]{}, true);
    }

    // Override this parameter for custom dataset impls
    public Dataset getDataset() throws Exception {
        return (Dataset) getPob();
    }

    public Dataset getDataset(final ChipOptParam chipParam) throws Exception {
        Dataset ds = getDataset();
        AbstractTool.setChip(ds, chipParam);
        return ds;
    }

}    // End class DatasetReqdParam
