/*
 * Copyright (c) 2003-2024 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xapps.gsea;

import edu.mit.broad.genome.Constants;
import edu.mit.broad.genome.NamingConventions;
import edu.mit.broad.genome.objects.*;
import edu.mit.broad.genome.parsers.ParserFactory;
import edu.mit.broad.genome.parsers.ParserWorker;
import edu.mit.broad.genome.reports.api.Report;
import edu.mit.broad.genome.viewers.*;
import edu.mit.broad.vdb.chip.Chip;
import edu.mit.broad.xbench.actions.ChipViewerAction;
import edu.mit.broad.xbench.actions.ExtAction;
import edu.mit.broad.xbench.actions.FileAction;
import edu.mit.broad.xbench.actions.FileObjectAction;
import edu.mit.broad.xbench.actions.FilesAction;
import edu.mit.broad.xbench.actions.ObjectAction;
import edu.mit.broad.xbench.actions.PobActions;
import edu.mit.broad.xbench.actions.ProxyFileAction;
import edu.mit.broad.xbench.actions.ProxyFileObjectAction;
import edu.mit.broad.xbench.actions.ProxyObjectAction;
import edu.mit.broad.xbench.actions.XAction;
import edu.mit.broad.xbench.actions.ext.FileBrowserAction;
import edu.mit.broad.xbench.actions.ext.OsExplorerAction;
import edu.mit.broad.xbench.actions.misc_actions.GeneSetMatrix2GeneSetAction;
import edu.mit.broad.xbench.actions.misc_actions.GeneSetMatrix2GeneSetsAction;
import edu.mit.broad.xbench.actions.misc_actions.GeneSetRemoveDuplicatesAction;
import edu.mit.broad.xbench.actions.misc_actions.LoadAction;
import edu.mit.broad.xbench.core.api.Application;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

/**
 * The grand registry for gsea actions.
 * Single place where actions are associated with objects, file types and classes.
 * Prefer to keep in code rather than in a xml table, as the compile time integrity checks
 * are nice and easier to maintain than updating a text file.
 *
 * @author Aravind Subramanian
 */
public class GseaActionRegistry {

    private final Map<String, PobActions> fExtActionsMap = new HashMap<String, PobActions>();
    private static final Logger klog = LoggerFactory.getLogger(GseaActionRegistry.class);

    /**
     * @maint IMP Conventions:
     * 1) First action listed is the "default" (i.e for double clicks etc) action. for OBJECTS
     * 2) THIRD action (if possible) is the "default" action for File (typically viewers)
     * 3) null actions signify a separator
     * 4) these are "viewer" style actions -- tools and data makers are generally
     * better placed in the menu/tool bar
     */

    // declare this first
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

    public static GseaActionRegistry getInstance() {
        if (kSingleton == null) {
            kSingleton = new GseaActionRegistry();
        }

        return kSingleton;
    }

    /**
     * List of file extensions that are parsable (for example gif is a
     * recognized file ext but isnt parsable
     */

    private GseaActionRegistry() {

        fExtActionsMap.put(Constants.RES, GEX_ACTIONS);
        fExtActionsMap.put(Constants.TXT, GEX_ACTIONS);
        fExtActionsMap.put(Constants.PCL, GEX_ACTIONS);
        fExtActionsMap.put(Constants.GCT, GEX_ACTIONS);

        fExtActionsMap.put(Constants.CLS, CLS_ACTIONS);
        fExtActionsMap.put(Constants.GRP, GRP_ACTIONS);

        fExtActionsMap.put(Constants.GMX, GMX_ACTIONS);
        fExtActionsMap.put(Constants.GMT, GMT_ACTIONS);

        fExtActionsMap.put(Constants.DEF, COMMON_ACTIONS);

        fExtActionsMap.put(Constants.RPT, RPT_ACTIONS);

        fExtActionsMap.put(Constants.CHIP, CHIP_ACTIONS);
        fExtActionsMap.put(Constants.RNK, RNK_ACTIONS);
    }

    // dont use directly -- for instantiating only
    // Make this snappy as its called whenever a right click is done etc
    private static PobActions _addCommon(final XAction[] customs) {

        List<XAction> all = new ArrayList<XAction>();
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

        return new PobActions(all.toArray(new XAction[all.size()]));
    }

    private PobActions lookupActions(final Object obj) {

        String name = "";

        if (obj instanceof Dataset || name.endsWith(Constants.RES) || name.endsWith(Constants.GCT) || name.endsWith(Constants.PCL) || name.endsWith(Constants.TXT)) {
            return GEX_ACTIONS;
        } else if (obj instanceof GeneSet || name.endsWith(Constants.GRP)) {
            return GRP_ACTIONS;
        } else if (obj instanceof GeneSetMatrix || name.endsWith(Constants.GMX) || name.endsWith(Constants.GMT)) {
            return GMX_ACTIONS;
        } else if (obj instanceof Report || name.endsWith(Constants.RPT)) {
            return RPT_ACTIONS;
        } else if (obj instanceof Chip || name.endsWith(Constants.CHIP)) {
            return CHIP_ACTIONS;
        } else if (obj instanceof Template || name.endsWith(Constants.CLS)) {
            return CLS_ACTIONS;
        } else if (obj instanceof RankedList) {
            return RNK_ACTIONS;
        } else {
            return new PobActions();
        }
    }

    public boolean runDefaultAction(final Object obj) {

        if (obj instanceof File || obj instanceof File[]) {

            File[] files;
            if (obj instanceof File) {
                files = new File[]{(File) obj};
            } else {
                files = (File[]) obj;
            }

            // if a single file with extension "html" or "tsv", then launch in browser
            if (files.length == 1) {
                String filename = files[0].getName();                
                if (StringUtils.endsWithIgnoreCase(filename, Constants.HTML) || StringUtils.endsWithIgnoreCase(filename, Constants.TSV)) {
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

    /**
     * @param file
     * @return
     * @maint file association - popup menu options are set here.
     */
    public JPopupMenu createPopup(final File file) {
    
        String ext = NamingConventions.getExtension(file);
        Object obj = fExtActionsMap.get(ext);
        
        if (obj == null) {
    
            JPopupMenu p = new JPopupMenu();
    
            // We can at least try to open unknown files in Desktop.browser
            if (!file.isDirectory()) {
                p.add(new FileBrowserAction(file));
            }
    
            p.add(new OsExplorerAction(file));
    
            return p;
        } else {
            PobActions da = (PobActions) obj;
            return _createPopup(da.allActions, file);
        }
    }

    /**
     * @param obj
     * @return
     * @maint Object <-> actions mapping mainatined here
     */
    public JPopupMenu createPopup(final Object obj) {
        if (obj instanceof File) {
            return createPopup((File) obj);
        } else {
    
            final PobActions pa = lookupActions(obj);
            final XAction[] actions = pa.allActions;
    
            if (actions != null && actions.length > 0) {
                return createPopupForObject(actions, obj);
            } else {
                return new JPopupMenu();
            }
        }
    }

    public JPopupMenu createPopupForObject(final XAction[] actions, final Object obj) {
    
        JPopupMenu menu = new JPopupMenu();
        List<JComponent> list = new ArrayList<JComponent>();
    
        for (int i = 0; i < actions.length; i++) {
            if (actions[i] == null) {
                list.add(new JSeparator());
            } else {
                try {
                    list.add(new JMenuItem(createAction(actions[i], obj)));
                } catch (Throwable e) {
                    klog.error("Error making popup", e);
                    list.add(new JMenuItem("Error making popup: " + e));
                }
            }
        }
    
        for (int i = 0; i < list.size(); i++) {
            menu.add(list.get(i));
        }
    
        return menu;
    }

    private JPopupMenu _createPopup(XAction[] actions, File file) {
    
        JPopupMenu menu = new JPopupMenu();
        if (actions == null) {
            return menu;
        }
    
        List<JComponent> list = new ArrayList<JComponent>();
        for (int i = 0; i < actions.length; i++) {
            if (actions[i] == null) {
                list.add(new JSeparator());
            } else {
                try {
                    JMenuItem item = new JMenuItem(createAction(actions[i], file));
                    list.add(item);
                } catch (Throwable e) {
                    klog.error(e.getMessage(), e);
                    list.add(new JMenuItem("Error making popup: " + e));
                }
            }
        }
    
        for (int i = 0; i < list.size(); i++) {
            menu.add(list.get(i));
        }
    
        return menu;
    }

    /**
     * data can be a File, an Object
     *
     * @param action
     * @param data
     * @return
     */
    public XAction createAction(final XAction action, final Object data) throws Exception {
        if (action instanceof FileObjectAction) {
            return (new ProxyFileObjectAction((FileObjectAction) action, data));
        } else if (action instanceof ObjectAction) {
            return (new ProxyObjectAction((ObjectAction) action, data));
        } else if ((action instanceof FileAction) && (data instanceof File)) { // @note file action and a file type
            return (new ProxyFileAction((FileAction) action, (File) data));
        } else if ((action instanceof FileAction) && data instanceof PersistentObject &&
                (ParserFactory.getCache().isCached((PersistentObject) data))) { // @note file action and a file type
            return (new ProxyFileAction((FileAction) action, ParserFactory.getCache().getSourceFile(data)));
        } else if ((action instanceof FilesAction) && (data instanceof File[])) {
            return (new ProxyFileAction((FilesAction) action, (File[]) data));
        } else if ((action instanceof ExtAction) && (data instanceof File)) {
            ExtAction real = (ExtAction) action.getClass().getDeclaredConstructor().newInstance();
            real.setPath(((File) data));
            return real;
        } else if ((action instanceof ExtAction) && (data instanceof PersistentObject)) {
            ExtAction real = (ExtAction) action.getClass().getDeclaredConstructor().newInstance();
            real.setPath(ParserFactory.getCache().getSourceFile(data));
            return real;
        } else if (action instanceof XAction) {    // simple, data-less widget opening
            return (XAction) action.getClass().getDeclaredConstructor().newInstance();
        } else {
            throw new Exception("Unknown action type: " + action + " and object combo: " + data);
        }
    }
}
