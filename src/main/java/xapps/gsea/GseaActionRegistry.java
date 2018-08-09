/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xapps.gsea;

import edu.mit.broad.genome.Constants;
import edu.mit.broad.genome.XLogger;
import edu.mit.broad.genome.objects.*;
import edu.mit.broad.genome.parsers.ParserWorker;
import edu.mit.broad.genome.reports.api.Report;
import edu.mit.broad.genome.viewers.*;
import edu.mit.broad.vdb.chip.Chip;
import edu.mit.broad.xbench.actions.ChipViewerAction;
import edu.mit.broad.xbench.actions.PobActions;
import edu.mit.broad.xbench.actions.XAction;
import edu.mit.broad.xbench.actions.ext.OsExplorerAction;
import edu.mit.broad.xbench.actions.misc_actions.GeneSetMatrix2GeneSetAction;
import edu.mit.broad.xbench.actions.misc_actions.GeneSetMatrix2GeneSetsAction;
import edu.mit.broad.xbench.actions.misc_actions.GeneSetRemoveDuplicatesAction;
import edu.mit.broad.xbench.actions.misc_actions.LoadAction;
import edu.mit.broad.xbench.core.api.Application;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import xapps.api.frameworks.AbstractActionLookup;

import java.awt.*;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * The grand registry for gsea actions.
 * Single place where actions are associated with objects, file types and classes.
 * Prefer to keep in code rather than in a xml table, as the compile time integrity checks
 * are nice and easier to maintain than updating a text file.
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
class GseaActionRegistry extends AbstractActionLookup implements Constants {

    static final Logger klog = XLogger.getLogger(GseaActionRegistry.class);

    static {
        klog.debug("Initializing GseaActionRegistry");
        //TraceUtils.showTrace();
    }

    /**
     * @maint IMP Conventions:
     * 1) First action listed is the "default" (i.e for double clicks etc) action. for OBJECTS
     * 2) THIRD action (if possible) is the "default" action for File (typically viewers)
     * 3) null actions signify a separator
     * 4) these are "viewer" style actions -- tools and data makers are generally
     * better placed in the menu/tool bar
     */

    // declre this first
    private static PobActions COMMON_ACTIONS = new PobActions(new XAction[]{
            new LoadAction(),
            null,
            new OsExplorerAction()
    });

    private static PobActions GEX_ACTIONS = _addCommon(new XAction[]{
            new DatasetViewerAction()
    });

    private static PobActions CHIP_ACTIONS = _addCommon(new XAction[]{
            new ChipViewerAction()});

    private static PobActions GRP_ACTIONS = _addCommon(new XAction[]{
            new GeneSetViewerAction(),
            null,
            //new GeneSet2RankedListAction(),
            new GeneSetRemoveDuplicatesAction()
    });

    private static PobActions CLS_ACTIONS = _addCommon(new XAction[]{
            new PhenotypeViewerAction()
    });

    private static PobActions RNK_ACTIONS = _addCommon(new XAction[]{
            new RankedListViewerAction()
    });

    private static PobActions GMX_ACTIONS = _addCommon(new XAction[]{
            new GeneSetMatrixViewerAction(),
            null,
            new GeneSetMatrix2GeneSetsAction(),
            new GeneSetMatrix2GeneSetAction()
    });

    private static PobActions GMT_ACTIONS = GMX_ACTIONS;

    private static PobActions RPT_ACTIONS = _addCommon(new XAction[]{
            new ReportViewerAction(),
    });

    private static GseaActionRegistry kSingleton;

    protected static GseaActionRegistry getInstance() {
        if (kSingleton == null) {
            kSingleton = new GseaActionRegistry();
        }

        return kSingleton;
    }

    /**
     * /**
     * List of file extensions that are parsable (for example gif is a
     * recognized file ext but isnt parsable
     */

    private GseaActionRegistry() {

        fExtActionsMap.put(RES, GEX_ACTIONS);
        fExtActionsMap.put(TXT, GEX_ACTIONS);
        fExtActionsMap.put(PCL, GEX_ACTIONS);
        fExtActionsMap.put(GCT, GEX_ACTIONS);

        fExtActionsMap.put(CLS, CLS_ACTIONS);
        fExtActionsMap.put(GRP, GRP_ACTIONS);

        fExtActionsMap.put(GMX, GMX_ACTIONS);
        fExtActionsMap.put(GMT, GMT_ACTIONS);

        fExtActionsMap.put(DEF, COMMON_ACTIONS);

        fExtActionsMap.put(RPT, RPT_ACTIONS);

        fExtActionsMap.put(CHIP, CHIP_ACTIONS);

        fExtActionsMap.put(RNK, RNK_ACTIONS);
    }

    // dont use directly -- for instantiating only
    // Make this snappy as its called whenever a right click is done etc
    private static PobActions _addCommon(final XAction[] customs) {

        List all = new ArrayList();
        int cnt = 0;
        for (int i = 0; i < customs.length; i++, cnt++) {
            all.add(customs[i]);
        }

        if (cnt != 0) {
            all.add(null);
        }

        for (int i = 0; i < COMMON_ACTIONS.allActions.length; i++) {
            all.add(COMMON_ACTIONS.allActions[i]);
        }

        // klog.debug("Loked up actions for: " + obj + " got: " + all.length);

        return new PobActions((XAction[]) all.toArray(new XAction[all.size()]));
    }

    protected PobActions lookupActions(final Object obj) {

        String name = "";

        if (obj instanceof Dataset || name.endsWith(RES) || name.endsWith(GCT) || name.endsWith(PCL) || name.endsWith(TXT)) {
            return GEX_ACTIONS;
        } else if (obj instanceof GeneSet || name.endsWith(".grp")) {
            return GRP_ACTIONS;
        } else if (obj instanceof GeneSetMatrix || name.endsWith(GMX) || name.endsWith(GMT)) {
            return GMX_ACTIONS;
        } else if (obj instanceof Report || name.endsWith(RPT)) {
            return RPT_ACTIONS;
        } else if (obj instanceof Chip || name.endsWith(CHIP)) {
            return CHIP_ACTIONS;
        } else if (obj instanceof Template || name.endsWith(CLS)) {
            return CLS_ACTIONS;
        } else if (obj instanceof RankedList) {
            return RNK_ACTIONS;
        } else {
            return new PobActions();
        }
    }

    // simply import
    public boolean runDefaultAction(final Object obj) {

        if (obj instanceof File || obj instanceof File[]) {

            File[] files;
            if (obj instanceof File) {
                files = new File[]{(File) obj};
            } else {
                files = (File[]) obj;
            }

            // if a single file with extension "html" or "xls", then launch in browser
            if (files.length == 1) {
                String filename = files[0].getName();                
                if (StringUtils.endsWithIgnoreCase(filename,"html") || StringUtils.endsWithIgnoreCase(filename,"xls")) {
                    URI fileURI = files[0].toURI();
                    try {
                        // we need to add an (empty) authority designator for compatibility with all platforms
                        // (mac requires an authority field in file URIs, windows does not)
                        // the resulting URI will have the form "file://<absolute path>"
                        fileURI = new URI(fileURI.getScheme(), "", fileURI.getPath(), null, null);
                        Desktop.getDesktop().browse(fileURI);
                    } catch (Exception e) {
                        Application.getWindowManager().showError(e.toString());
                    }
                    return true;
                }
            }

            new ParserWorker(files).execute();

            return true;
        } else {
            return false; // @todo
        }

    }

}    // End GseaActionsRegistry

