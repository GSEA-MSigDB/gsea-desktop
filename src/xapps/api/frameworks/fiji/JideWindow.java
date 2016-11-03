/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xapps.api.frameworks.fiji;

import edu.mit.broad.xbench.core.Window;

import javax.swing.*;

/**
 * @author Aravind Subramanian
 */
// We have lots of wrappers here.  Looks like we can collapse all of this and use the underlying
// Class directly (whatever that may be).  Need to sort that out.
public class JideWindow implements Window {

    private String fTitle;

    private JComponent fComp;

    /**
     * Class constructor
     *
     * @param comp
     */
    public JideWindow(final String title, final JComponent comp) {
        this.fComp = comp;
        this.fTitle = title;
    }

}
