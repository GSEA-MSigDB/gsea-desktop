/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package org.genepattern.gsea;

import edu.mit.broad.xbench.core.api.Application;
import org.genepattern.uiutil.UIUtil;

import java.awt.*;

/**
 * @author Joshua Gould
 */
public class XToolsMessageHandler implements UIUtil.MessageHandler {

    public void showMessageDialog(Component parent, String message) {
        Application.getWindowManager().showMessage(message);
    }

    public void showErrorDialog(Component parent, String message) {
        Application.getWindowManager().showError(message);
    }

}
