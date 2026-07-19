/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xtools.api.param;


/**
 * Object to capture commandline params</p>
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class ModeReqdParam extends StringReqdParam {

    public ModeReqdParam(String name, String nameEnglish, String desc, String[] def_and_hints) {
        super(name, nameEnglish, desc, def_and_hints);
    }

    public ModeReqdParam(String name, String nameEnglish, String desc, String def, String[] hints) {
        super(name, nameEnglish, desc, def, hints);
    }
}