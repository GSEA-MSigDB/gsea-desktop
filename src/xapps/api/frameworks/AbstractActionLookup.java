/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xapps.api.frameworks;

import edu.mit.broad.genome.NamingConventions;
import edu.mit.broad.genome.XLogger;
import edu.mit.broad.genome.objects.PersistentObject;
import edu.mit.broad.genome.parsers.ParserFactory;
import edu.mit.broad.xbench.actions.*;
import edu.mit.broad.xbench.actions.ext.FileBrowserAction;
import edu.mit.broad.xbench.actions.ext.OsExplorerAction;

import org.apache.log4j.Logger;

import javax.swing.*;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Aravind Subramanian
 */
public abstract class AbstractActionLookup {

    private static final Logger klog = XLogger.getLogger(AbstractActionLookup.class);

    /**
     * key -> ext, value -> Actions that are possible on the ext
     */
    protected final Map fExtActionsMap = new HashMap();

    protected AbstractActionLookup() {

    }

    /**
     * Note sub classes must implement this method
     *
     * @param obj
     * @return
     */
    protected abstract PobActions lookupActions(final Object obj);

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

        //klog.debug("createPopup Object: " + obj);

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

    // ditto as file -> except no parser included as object already parsed!
    public JPopupMenu createPopupForObject(final XAction[] actions, final Object obj) {

        JPopupMenu menu = new JPopupMenu();
        List list = new ArrayList();

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
            menu.add((JComponent) list.get(i));
        }

        return menu;
    }

    private JPopupMenu _createPopup(XAction[] actions, File file) {

        JPopupMenu menu = new JPopupMenu();
        if (actions == null) {
            return menu;
        }

        List list = new ArrayList();

        for (int i = 0; i < actions.length; i++) {
            if (actions[i] == null) {
                list.add(new JSeparator());
            } else {
                try {
                    JMenuItem item = new JMenuItem(createAction(actions[i], file));

                    list.add(item);
                } catch (Throwable e) {
                    klog.error(e);
                    list.add(new JMenuItem("Error making popup: " + e));
                }
            }
        }

        for (int i = 0; i < list.size(); i++) {
            menu.add((JComponent) list.get(i));
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
            Class cl = action.getClass();
            ExtAction real = (ExtAction) cl.newInstance();
            real.setPath(((File) data));
            return real;
        } else if ((action instanceof ExtAction) && (data instanceof PersistentObject)) {
            Class cl = action.getClass();
            ExtAction real = (ExtAction) cl.newInstance();
            real.setPath(ParserFactory.getCache().getSourceFile(data));
            return real;
        } else if (action instanceof XAction) {    // simple, data-less widget opening
            Class cl = action.getClass();
            return (XAction) cl.newInstance();
        } else {
            throw new Exception("Unknown action type: " + action + " and object combo: " + data);
        }
    }
}
