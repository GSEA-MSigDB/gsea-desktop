/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xapps.api;

import edu.mit.broad.xbench.actions.WidgetAction;
import edu.mit.broad.xbench.core.Widget;

/**
 * @author Aravind Subramanian
 */
public class AppDataLoaderAction extends WidgetAction {

    private AppDataLoaderWidget fApl;

    public AppDataLoaderAction() {
      super("AppDataLoaderAction", "AppDataLoaderWidget.TITLE", "Import data from file(s) into the application", 
              AppDataLoaderWidget.ICON);
    }

    public Widget getWidget() {

        if (fApl == null) {
            fApl = new AppDataLoaderWidget();
        }

        fApl.revalidate();
        fApl.repaint();
        return fApl;
    }
}    // End AppFileBrowserAction