/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xtools.api.param;

/**
 * <p> Param to capture whether or not to run too in GUI mode</p>
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class GuiParam extends BooleanParam {

    /**
     * Class constructor
     * <p/>
     * default -> if reqd, then true, false
     * else false, true
     *
     * @param reqd
     */
    public GuiParam() {
        super(GUI, GUI_DESC, false, false); // defaults: never reqd, default is false
    }

}    // End class GuiParam
