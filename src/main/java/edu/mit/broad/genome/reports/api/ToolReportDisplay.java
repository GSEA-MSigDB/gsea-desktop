/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.reports.api;

import edu.mit.broad.genome.Constants;
import edu.mit.broad.genome.NamingConventions;
import edu.mit.broad.genome.XLogger;
import edu.mit.broad.genome.charts.XChart;
import edu.mit.broad.genome.parsers.DataFormat;
import edu.mit.broad.genome.swing.GPopupChecker;
import edu.mit.broad.genome.swing.GuiHelper;
import edu.mit.broad.xbench.actions.ext.*;
import edu.mit.broad.xbench.core.api.Application;

import org.apache.log4j.Logger;

import javax.swing.*;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

/**
 * Class ToolReportDisplay
 * <p/>
 * Here because of linkage
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class ToolReportDisplay {

    /**
     * IMP IMP IMP
     * <p/>
     * Careful to not use anything that causes xomics to initialize and launch!
     * <p/>
     * Not using RuntimeResources is a lost cause -- to much needed from that.
     * But try to not use actions
     * <p/>
     * Duplicating stuff is fine.
     */
    private JList jlDisplay;
    private ToolReport fReport;
    private final Logger log = XLogger.getLogger(this.getClass());
    private JFrame fFrame;
    private static final Dimension DEFAULT_DIM = new Dimension(500, 325);

    private final ToolReportDisplay fInstance = this;

    /**
     * Class constructor
     *
     * @param report
     */
    public ToolReportDisplay(ToolReport report) {

        if (report == null) {
            throw new IllegalArgumentException("Param reports cannot be null");
        }

        this.fReport = report;

        init();
    }

    /**
     * Show the reports display
     */
    public void show() {

        centerFrame(fFrame);
        fFrame.setSize(DEFAULT_DIM);
        fFrame.setVisible(true);

    }

    private void init() {

        jbInit();

        /* dont add, it makes the thing quit even when running as xomics
        fFrame.addWindowListener(new WindowAdapter() {

            public void windowClosed(WindowEvent e) {
                //log.debug("closed");
                fInstance.quit();
            }


            public void windowClosing(WindowEvent e) {
                   //log.debug("closing");
                   fInstance.quit();
            }

        });
        */
    }

    private void jbInit() {

        String desc;
        File rdir = fReport.getReportDir();

        if ((rdir != null) && (rdir.exists())) {
            desc = "Report for " + fReport.getName() + " [" + rdir.getAbsolutePath() + "]";
        } else {
            desc = "Report for " + fReport.getName() + " [ No data files ]";
        }

        this.jlDisplay = new JList();
        jlDisplay.setToolTipText(desc);
        jlDisplay.setCellRenderer(new CommonLookAndClickListRenderer(jlDisplay));
        jlDisplay.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        DefaultListModel model = new DefaultListModel();

        // only files
        // also want to always add the index HTML file first (if one was made)


        File[] files = fReport.getFilesProduced();
        File indexPageFile = fReport.getIndexPageFile();
        if (indexPageFile != null) {
            model.addElement(indexPageFile);
        }

        for (int i = 0; i < files.length; i++) {

            if (indexPageFile != null) {
                if (!indexPageFile.equals(files[i])) {
                    model.addElement(files[i]);
                }
            } else {
                model.addElement(files[i]);
            }

        }

        /*
        Chart[] charts = fReport.getCharts();

        for (int i = 0; i < charts.length; i++) {
            model.addElement(charts[i]);
        }
        */

        jlDisplay.setModel(model);
        jlDisplay.setBorder(BorderFactory.createTitledBorder("Files Produced (double click to open, right-click for options)"));

        jlDisplay.setBackground(GuiHelper.COLOR_LIGHT_ORANGE);
        
        if (model.getSize() > 0) {
            jlDisplay.setSelectedIndex(0);
        }

        this.fFrame = new JFrame(desc);

        fFrame.setVisible(false);    // dont show yet
        fFrame.setSize(DEFAULT_DIM);
        fFrame.getContentPane().setLayout(new BorderLayout());
        fFrame.getContentPane().add(createCommandPanel(), BorderLayout.NORTH);

        JScrollPane sp = new JScrollPane(jlDisplay);
        sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        fFrame.getContentPane().add(sp, BorderLayout.CENTER);

        JPanel butPanel = new JPanel();

        if ((rdir != null) && (rdir.exists())) {
            JButton bExplorer = new JButton("Browse");

            bExplorer.addActionListener(new OsExplorerAction(rdir));
            butPanel.add(bExplorer);
        }

        JButton bQuit = new JButton("Quit");
        if (fReport.getTool().getParamSet().getGuiParam().isTrue()) {
            bQuit.setText("Close");
        }

        bQuit.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                fInstance.close();
            }
        });
        butPanel.add(bQuit);
        fFrame.getContentPane().add(butPanel, BorderLayout.SOUTH);
    }

    private JComponent createCommandPanel() {

        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(DEFAULT_DIM.width, 55));

        StringBuffer buf = new StringBuffer(fReport.getProducerName()).append(' ');
        buf.append(fReport.getTool().getParamSet().getAsCommand(false, false, false).trim());

        final JTextArea ta = new JTextArea(1, 40);

        ta.setBorder(BorderFactory.createTitledBorder("The command was:"));
        ta.setText(buf.toString());
        ta.setWrapStyleWord(true);

        JButton bCopy = new JButton("Copy", GuiHelper.ICON_COPY16);

        bCopy.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {

                StringSelection stsel = new StringSelection(ta.getText());
                Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();

                clip.setContents(stsel, stsel);
            }
        });
        panel.add(new JScrollPane(ta), BorderLayout.CENTER);
        panel.add(bCopy, BorderLayout.EAST);

        return panel;
    }

    /**
     * Duplicating actions stuff for perf + avoiding dependencies
     *
     * @param file
     */
    private Icon getIcon(File file) {
        return DataFormat.getIcon(file);
    }

    private void runDefaultAction(Object obj) {

        // TODO: restructure
        Action action = null;

        if (obj instanceof File) {
            File file = (File) obj;
            String ext = NamingConventions.getExtension(file);
            if (ext.equalsIgnoreCase(Constants.HTML)) {
                // TODO: Can we use this on any non-directory file?
                // Use the FileBrowserAction here as we have a File and not a URL.  That will correctly build the URL from the File.
                action = new FileBrowserAction(file);
            } else {
                 action = new OsExplorerAction(file);
            }
        } else if (obj instanceof XChart) {
            // TODO: confirm whether this code is reachable.  I suspect not.
            action = new ReportChartAction((XChart) obj);
        }

        if (action != null) {
            action.actionPerformed(null);
        } else {
            Application.getWindowManager().showMessage("No action defined for: " + obj);
        }
    }

    private JPopupMenu createPopupMenu(Object obj) {

        JPopupMenu popup = null;

        if (obj instanceof File) {
            popup = new JPopupMenu();
            File file = (File) obj;
            String ext = NamingConventions.getExtension(file);
            if (ext.equalsIgnoreCase(Constants.HTML)) {
                // TODO: Can we use this on any non-directory file?
                // Use the FileBrowserAction here as we have a File and not a URL.  That will correctly build the URL from the File.
                popup.add(new FileBrowserAction(file));
            } else {
                popup.add(new OsExplorerAction(file));
            }
        } else if (obj instanceof XChart) {
            // TODO: confirm whether this code is reachable.  I suspect not.
            popup = new JPopupMenu();
            popup.add(new ReportChartAction((XChart) obj));
        }

        return popup;
    }


    private static final Font FONT_DEFAULT_BOLD = new Font("Helvetica", Font.BOLD, 12);
    private static final String index_html = "index.html";

    /**
     * Veri similar to the one in RendererFactory2
     * But reproducing code here to avoid xomics dependencies.
     */
    private class CommonLookAndClickListRenderer extends DefaultListCellRenderer {
        private Font normalFont;

        private CommonLookAndClickListRenderer(final JList renderedlist) {

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

                        runDefaultAction(obj);
                    }
                }
            });
        }

        public Component getListCellRendererComponent(JList list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof XChart) {
                this.setText(((XChart) value).getName());
                this.setIcon(XChart.ICON);
            } else if (value instanceof File) {

                // special highlighting for the index html page
                if (((File) value).getName().equalsIgnoreCase(index_html)) {
                    this.setText(index_html);
                    this.setFont(FONT_DEFAULT_BOLD);
                    //this.setForeground(Color.RED);
                } else {
                    this.setFont(normalFont);
                    this.setText(((File) value).getAbsolutePath());
                }

                this.setIcon(fInstance.getIcon((File) value));
                this.setToolTipText(((File) value).getAbsolutePath());
            }

            return this;
        }
    }

    /**
     * Popup displayer
     */
    private class MyPopupMouseListener extends GPopupChecker {

        protected void maybeShowPopup(MouseEvent e) {

            if (e.isPopupTrigger()) {
                Object ot = e.getSource();

                if (ot instanceof JList) {
                    Object obj = ((JList) ot).getSelectedValue();

                    if (obj == null) {
                        return;
                    }

                    JPopupMenu popup = createPopupMenu(obj);
                    if (popup != null) {
                        popup.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }
        }
    }    // End of inner class MyPopupMouseListener

    private static void centerFrame(JFrame frame) {

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension size = frame.getSize();

        frame.setLocation((screenSize.width - size.width) / 2,
                (screenSize.height - size.height) / 2);
    }

    /**
     * Dont quit if we are debugging in JUnit
     */
    private void close() {
        fFrame.dispose();
        //fFrame.hide();
        fFrame.setVisible(false);
        log.debug("Debug mode, so not quiting but hiding");
    }

    /**
     * @author Aravind Subramanian
     * @version %I%, %G%
     */
    // TODO: evaluate the ReportChartAction for removal.
    // DRE note: After removing some unused code, this class probably has no 
    // remaining useful purpose in the current Desktop code base.  It has
    // been left in place for now since its removal now would be a distraction
    // to other current clean-up currently in motion which turned it up.
    // Look into the XChart class...
    class ReportChartAction extends AbstractAction {

        final XChart fChart;

        private ReportChartAction(XChart chart) {
            this.fChart = chart;
        }

        public void actionPerformed(ActionEvent evt) {
            // Stub action handler.  This formerly contained code that was
            // determined to be unused in the Desktop.  That implies this
            // whole class is unused - see comment above.
        }
    }

}    // End ToolReportDisplay
