/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xtools.api;

import edu.mit.broad.genome.NotImplementedException;
import edu.mit.broad.xbench.core.api.*;
import xapps.api.frameworks.fiji.StatusBarAppender;

import java.awt.*;

public class XToolsApplication implements Application.Handler {

    private static final VdbManager fVdbmanager = new VdbManagerImpl("foo");

    /**
     * Class constructor
     */
    public XToolsApplication() {
    }

    public ToolManager getToolManager() {
        throw new NotImplementedException();
    }

    public FileManager getFileManager() {
        throw new NotImplementedException();
    }

    public VdbManager getVdbManager() {
        return fVdbmanager;
    }

    public WindowManager getWindowManager() throws HeadlessException {
        throw new NotImplementedException();
    }

    public StatusBarAppender getStatusBarAppender() {
        return null;
    }

} // End class XToolsApplication
