/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xtools.api.param;

import java.awt.event.ActionListener;

/**
 * Object to capture commandline params</p>
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class ModeReqdParam extends StringReqdParam implements ActionListener {

    /**
     * Class constructor
     *
     * @param name
     * @param nameEnglish
     * @param desc
     * @param def_and_hints
     */
    public ModeReqdParam(String name, String nameEnglish, String desc, String[] def_and_hints) {
        super(name, nameEnglish, desc, def_and_hints);
    }

}    // End class StringsChooserParam2
