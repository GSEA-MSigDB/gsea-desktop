/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.explorer.filemgr;

import edu.mit.broad.genome.JarResources;
import edu.mit.broad.genome.XLogger;
import edu.mit.broad.genome.io.FileTransferable;
import edu.mit.broad.genome.parsers.DataFormat;
import edu.mit.broad.genome.parsers.ParserWorker;
import edu.mit.broad.genome.swing.GPopupChecker;
import edu.mit.broad.genome.swing.GseaSimpleInternalFrame;
import edu.mit.broad.genome.swing.GuiHelper;
import edu.mit.broad.genome.swing.dnd.DndSource;
import edu.mit.broad.genome.swing.dnd.DragSourceDecorator;
import edu.mit.broad.genome.utils.FileUtils;
import edu.mit.broad.xbench.actions.misc_actions.CopyFilesAction;
import edu.mit.broad.xbench.actions.misc_actions.FilesSelectable;
import edu.mit.broad.xbench.core.api.Application;

import org.apache.log4j.Logger;

import javax.swing.*;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

/**
 * Class JRecentFilesList
 * With a list of files opened in the "recent" past.
 * <p/>
 * DND
 * This is a drag source -> Files can be dragged out into appropriate receivers
 * This is NOT a drag receiver
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class JRecentFilesList extends JList implements DndSource, FilesSelectable {

    private final Logger log = XLogger.getLogger(JRecentFilesList.class);
    private JRecentFilesList fInstance;
    public static final String DEFAULT_TITLE = "Recent files";
    public static final Icon ICON = JarResources.getIcon("History24.gif");
    private static final Font kFont = new Font("Arial", Font.PLAIN, 12);
    private CopyFilePathAction fCopyAction;
    private PurgeAllFilesPathAction fPurgeAllAction;

    /**
     * @param title
     * @return
     */
    public static JComponent createComponent(final String title) {
        JRecentFilesList recentFiles = new JRecentFilesList();
        JScrollPane sp = new JScrollPane(recentFiles);
        GseaSimpleInternalFrame sif = new GseaSimpleInternalFrame(title);
        sif.add(sp);
        return sif;
    }

    /**
     * Class constructor
     * Creates a explorer like jlist with popups et al
     */
    public JRecentFilesList() {

        fInstance = this;

        this.setModel(Application.getFileManager().getRecentFilesStore());

        this.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        this.fCopyAction = new CopyFilePathAction();
        this.fPurgeAllAction = new PurgeAllFilesPathAction();
        this.setCellRenderer(new ListCellRenderer());
        this.setBackground(GuiHelper.COLOR_VERY_LIGHT_GRAY);

        // enable Dnd
        new DragSourceDecorator(this);

        CopyFilesAction cfa = new CopyFilesAction(this);
        this.addKeyListener(cfa.createCtrlCKeyListener());
    }

    /**
     * FilesSelectable impl
     *
     * @return
     */
    public FileTransferable getSelectedFiles() {
        return (FileTransferable) getTransferable();

    }

    /**
     * FilesSelectable impl
     *
     * @return
     */
    public void refresh() {
    }

    public Transferable getTransferable() {

        Object[] objs = fInstance.getSelectedValues();

        if ((objs == null) || (objs.length == 0)) {
            return new FileTransferable(new File[]{});
        }

        File[] files = new File[objs.length];

        for (int i = 0; i < objs.length; i++) {
            files[i] = new File(objs[i].toString());
        }

        return new FileTransferable(files);
    }

    public Component getDraggableComponent() {
        return this;
    }

    /**
     * Class ListCellRenderer
     *
     * @author Aravind Subramanian
     * @version %I%, %G%
     */
    class ListCellRenderer extends DefaultListCellRenderer {

        public ListCellRenderer() {

            //log.debug(">>>> " + fInstance);

            // IMP to NOT place this piece of code in the popupmenu checker - that causes
            // the widget to launch twice
            fInstance.addMouseListener(new MouseAdapter() {

                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {

                        if (fInstance.getSelectedValue() == null) {
                            return;
                        }

                        File file = new File(fInstance.getSelectedValue().toString());
                        log.info("Loading ... " + file.getPath());
                        Application.getWindowManager().runDefaultAction(file);

                        /*
                        if (file.exists()) {
                            boolean success = Application.getWindowManager().runDefaultAction(file);
                            if (success) {
                                Application.getWindowManager().showMessage("Successfully loaded: " + file.getName());
                            }
                        }
                        */
                    }
                }
            });

            // notice adding listener to the list and not to the renderer
            fInstance.addMouseListener(new MyPopupMouseListener());
        }

        /**
         * showing the full path makes it kinda clutterred
         * instead go ../parent_dir/file_name
         * having curr dir displayed makes uniqueness visible.
         */
        public Component getListCellRendererComponent(JList list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {

            // doesnt work properly unless called
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            //log.debug(value + " class: " + value.getClass());
            if (value instanceof String) {

                File file = new File(value.toString());
                this.setText(FileUtils.shortenedPathRepresentation(file));
                this.setIcon(DataFormat.getIcon(file));
                this.setToolTipText(value.toString());
                this.setFont(kFont);

                if (!file.exists()) {
                    this.setForeground(Color.red);
                }
            }

            return this;
        }
    }    // End ListCellRenderer

    /**
     * Popup displayer
     */
    class MyPopupMouseListener extends GPopupChecker {

        protected void maybeShowPopup(MouseEvent e) {

            if (e.isPopupTrigger()) {

                //log.debug("Launching popup trigger");
                Object[] sel = fInstance.getSelectedValues();

                if (sel == null) {
                    return;
                }

                JPopupMenu popup = null;

                log.debug(">> sel = " + sel.length);
                if (sel.length > 1) {
                    popup = new JPopupMenu();
                    File[] files = new File[sel.length];
                    for (int i = 0; i < sel.length; i++) {
                        //log.debug(sel[i].getClass());
                        files[i] = new File(sel[i].toString());
                    }

                    // old style that brings up a widget window
                    //popup.add(new ParserAction(files));
                    // new way simply loads
                    popup.add(new ImportFilesAction(files));

                } else if (sel.length == 1) {
                    popup = Application.getWindowManager().createPopupMenu(new File(sel[0].toString()));
                }

                if (popup != null) {
                    popup.add(new JSeparator());
                    popup.add(fCopyAction);
                    popup.add(new JSeparator());
                    popup.add(fPurgeAllAction);
                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        }
    }    // End of inner class MyPopupMouseListener

    /**
     * Class CopyFilePathAction
     *
     * @author Aravind Subramanian
     * @version %I%, %G%
     */
    class CopyFilePathAction extends AbstractAction {

        CopyFilePathAction() {

            this.putValue(Action.NAME, "Copy File(s)");
            this.putValue(Action.SMALL_ICON, JarResources.getIcon("Copy16.gif"));
            this.putValue(Action.SHORT_DESCRIPTION, "Copy File(s)");
        }

        public void actionPerformed(ActionEvent evt) {
            Transferable t = getTransferable();
            Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
            clip.setContents(t, (ClipboardOwner) t);    // we know that FileTransferable is whats returned and that it is also a cpowner
        }
    }

    class PurgeAllFilesPathAction extends AbstractAction {

        PurgeAllFilesPathAction() {

            this.putValue(Action.NAME, "Purge All Files");
            // Couldn't find a good icon...
            this.putValue(Action.SMALL_ICON, null);
            this.putValue(Action.SHORT_DESCRIPTION, "Copy File(s)");
        }

        public void actionPerformed(ActionEvent evt) {
            boolean proceed = Application.getWindowManager().showConfirm("Delete file history?", "<html><body><b>" +
                    "This will remove recently used files from this list (but NOT delete the files themselves)</b>" +
                    "</body></html>"
            );

            if (proceed) {
                Application.getFileManager().getRecentFilesStore().clearAll();
                fInstance.refresh();
            }
        }
    }


    static class ImportFilesAction extends AbstractAction {

        File[] files;

        ImportFilesAction(File[] files) {
            this.files = files;
            this.putValue(Action.NAME, "Import data from selected " + files.length + " files");
            this.putValue(Action.SHORT_DESCRIPTION, "Import data from selected files");
        }

        public void actionPerformed(ActionEvent evt) {
            new ParserWorker(files).execute();
        }
    }
}    // End JRecentFilesList

/*--- Formatted in Sun Java Convention Style on Mon, Oct 21, '02 ---*/