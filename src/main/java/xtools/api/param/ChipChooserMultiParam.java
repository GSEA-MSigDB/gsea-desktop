/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xtools.api.param;

import edu.mit.broad.vdb.chip.Chip;
import edu.mit.broad.vdb.chip.ChipHelper;

/**
 * choose 1 or more string
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 * @note diff from StringsInputParam -> the chpices are presented in a JListWindow and not
 * in a text area
 */
// Note: this has been modified to never allow multiple CHIP file selection from the UI.
// TODO: clean up back-end code to only handle a single CHIP.
public class ChipChooserMultiParam extends WChipChooserAbstractParam implements Param {

    /**
     * @param name
     * @param desc
     * @param reqd
     */
    public ChipChooserMultiParam(String name, String nameEnglish, String desc, boolean reqd) {
        super(name, nameEnglish, desc, reqd, false);
    }

    public ChipChooserMultiParam(boolean reqd) {
        this(CHIP, CHIP_ENGLISH, CHIP_DESC, reqd);
    }

    public Chip[] getChips() throws Exception {
        return super._getChips();
    }

    public Chip getChipCombo() throws Exception {
        Chip[] chips = getChips();

        // not sure why the chips.lemtgh == 0 check is needed - should this be generically caught?
        // see Probes2Symbol for an example of how this method fails (on no chip specified) if not checked in this manner
        if (isReqd() && chips.length == 0) {
            throw new IllegalArgumentException("Chip must be specified");
        }

        return ChipHelper.createComboChip(chips);
    }

}    // End class ChipsChooserParam

