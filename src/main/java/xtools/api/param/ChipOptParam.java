/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xtools.api.param;

import edu.mit.broad.vdb.chip.Chip;

/**
 * @author Aravind Subramanian
 */
public class ChipOptParam extends WChipChooserAbstractParam {

    public ChipOptParam(boolean reqd) {
        super(CHIP, CHIP_ENGLISH, CHIP_DESC, reqd, false);
    }

    public Chip getChip() throws Exception {
        return super._getChip();
    }

    public Chip getChipCombo() throws Exception {
        return super._getChip();
    }

}    // End class ChipOptParam
