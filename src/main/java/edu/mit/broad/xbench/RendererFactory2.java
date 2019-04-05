/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.xbench;

import edu.mit.broad.genome.JarResources;
import edu.mit.broad.genome.charts.XChart;
import edu.mit.broad.genome.objects.PersistentObject;
import edu.mit.broad.genome.parsers.DataFormat;
import edu.mit.broad.genome.parsers.ParserFactory;
import edu.mit.broad.genome.swing.GPopupChecker;
import edu.mit.broad.xbench.core.api.Application;
import org.genepattern.uiutil.FTPFile;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

/**
 * Collection of renderers for components bearing (all or some) objects
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class RendererFactory2 {

    public static final Icon FTP_FILE_ICON = JarResources.getIcon("FTPFile.gif");

    /**
     * For rendering the lists/combo boxes that have nodes properly
     * <p/>
     * In adddition to working on presentable objects also works on
     * persistenobjects by looking up their node delegate - this mechanism uses the
     * sco system along with additional caching so fairly perf efficient.
     */
    public static class CommonLookAndDoubleClickListRenderer extends CommonLookListRenderer {

        /**
         * Class Constructor.
         * This form of the constructor enables a popup menu for the jlist
         * apart from the usual rendering stuff.
         */
        public CommonLookAndDoubleClickListRenderer(final JList renderedlist) {

            // notice adding listener to the list and not to the renderer
            renderedlist.addMouseListener(new MyPopupMouseListener());

            // IMP to NOT place this piece of code in the popupmenu checker - that causes
            // the widget to launch twice
            renderedlist.addMouseListener(new MouseAdapter() {

                public void mouseClicked(MouseEvent e) {

                    if (e.getClickCount() == 2) {

                        //log.debug("Doing double click");
                        Object obj = renderedlist.getSelectedValue();

                        if (obj == null) {
                            return;
                        }

                        Application.getWindowManager().runDefaultAction(obj);
                    }
                }
            });
        }

    }


    /**
     * For rendering the lists/combo boxes containing known Files and
     * objects.
     * <p/>
     * No events
     *
     * @see CommonLookAndDoubleClickListRenderer
     */
    public static class CommonLookListRenderer extends DefaultListCellRenderer {
        private boolean ifFileOnlyShowName;

        public CommonLookListRenderer(final boolean ifFileOnlyShowName) {
            this.ifFileOnlyShowName = ifFileOnlyShowName;
        }

        public CommonLookListRenderer() {
            this(false); // default is to show the full path
        }

        public Component getListCellRendererComponent(JList list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {

            // doesnt work properly unless called
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            /*
            klog.debug(">>>>>>>>>>>>>>>>>value " + value);
            if (value != null) {
                klog.debug(" class: " + value.getClass());
            }
            */

            // order is important
            if (value instanceof PersistentObject) {
                PersistentObject pob = (PersistentObject) value;

                if (pob.getQuickInfo() != null) {
                    StringBuffer buf = new StringBuffer("<html><body>").append(pob.getName());
                    buf.append("<font color=#666666> [").append(pob.getQuickInfo()).append(']').append("</font></html></body>");
                    this.setText(buf.toString());
                } else {
                    this.setText(pob.getName());
                }

                File f = null;

                if (ParserFactory.getCache().isCached(pob)) {
                    f = ParserFactory.getCache().getSourceFile(pob);
                }

                if (f != null) {
                    this.setToolTipText(f.getAbsolutePath());
                } else {
                    this.setToolTipText("Unknown origins of file: " + f);
                }
            } else if (value instanceof File) {
                if (ifFileOnlyShowName) {
                    this.setText(((File) value).getName());
                } else {
                    this.setText(((File) value).getAbsolutePath());
                }
                this.setIcon(DataFormat.getIcon(value));
                this.setToolTipText(((File) value).getAbsolutePath());
            } else if (value instanceof XChart) {
                this.setText(((XChart) value).getName());
                this.setIcon(XChart.ICON);
            } else if (value instanceof FTPFile) {

                String s = ((FTPFile) value).getPath();
                String slc = s.toLowerCase();
                if (slc.indexOf("c1.") != -1) {
                    s = s + " [Positional]";
                } else if (slc.indexOf("c2.") != -1) {
                    s = s + " [Curated]";
                } else if (slc.indexOf("c3.") != -1) {
                    s = s + " [Motif]";
                } else if (slc.indexOf("c4.") != -1) {
                    s = s + " [Computational]";
                }

                this.setText(s);
                this.setIcon(FTP_FILE_ICON);
            }

            return this;
        }
    }    // End CommonLookListRenderer

    /**
     * Popup displayer
     */
    static class MyPopupMouseListener extends GPopupChecker {

        /**
         * Let the node do the actual handling of popup
         * events.
         * Here, just fetch the menu from the DataObject
         * and display in the appropriate location
         */
        protected void maybeShowPopup(MouseEvent e) {

            if (e.isPopupTrigger()) {
                Object ot = e.getSource();

                //log.debug("Source is: " + ot);

                if (ot instanceof JTree) {
                    Object obj = ((JTree) ot).getSelectionPath().getLastPathComponent();

                    //log.debug("Trying to show popup for: " + obj);
                    JPopupMenu popup = Application.getWindowManager().createPopupMenu(obj);

                    popup.show(e.getComponent(), e.getX(), e.getY());
                } else if (ot instanceof JList) {
                    Object obj = ((JList) ot).getSelectedValue();

                    //log.debug("Trying to show popup for: " + obj);

                    if (obj == null) {
                        return;
                    }

                    JPopupMenu popup = Application.getWindowManager().createPopupMenu(obj);

                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        }
    }    // End of inner class MyPopupMouseListener

}        // End RendererFactory2
