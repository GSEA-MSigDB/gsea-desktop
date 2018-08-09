/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xtools.api.param;

import edu.mit.broad.genome.objects.RankedList;

import java.awt.event.ActionListener;

/**
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class RankedListReqdParam extends PobParam implements ActionListener {
    /**
     * Class constructor
     */
    public RankedListReqdParam() {
        this(RNK, RNK_ENGLISH, RNK_DESC);
    }

    public RankedListReqdParam(final String name, final String nameEnglish, final String desc) {
        super(name, nameEnglish, RankedList.class, desc, new RankedList[]{}, true);
    }

    // Override this parameter for custom ranked list impls
    public RankedList getRankedList() throws Exception {
        return (RankedList) getPob();
    }

}    // End class RankedListReqdParam
