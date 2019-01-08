package xapps.gsea;
/*******************************************************************************
 * Copyright (c) 2003-2018 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/


import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jidesoft.swing.*;
import edu.mit.broad.genome.JarResources;
import edu.mit.broad.genome.objects.esmatrix.db.EnrichmentDb;
import edu.mit.broad.genome.parsers.ParserFactory;
import edu.mit.broad.genome.swing.GuiHelper;
import edu.mit.broad.genome.swing.fields.GDirFieldPlusChooser;
import edu.mit.broad.genome.swing.fields.GFieldPlusChooser;
import edu.mit.broad.genome.viewers.AbstractViewer;
import edu.mit.broad.xbench.core.api.Application;
import org.genepattern.gsea.LeadingEdgeWidget;

import xapps.api.vtools.ParamSetFormForAFew;
import xtools.api.param.DirParam;
import xtools.api.param.Param;
import xtools.api.param.ReportCacheChooserParam;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

/**
 * @author Aravind Subramanian
 */
public class LeadingEdgeReportViewer extends AbstractViewer {

    public static final String NAME = "LeadingEdgeReportWidget";

    public static final Icon ICON = JarResources.getIcon("Lev16_b.gif");

    private ReportCacheChooserParam fReportParam;

    private DirParam fDirParam;

    private File curr_gseaResultDir;

    private JComponent fFiller;

    private LeadingEdgeReportViewer fInstance = this;

    /**
     * Builds a Viewer on specified FSet object
     *
     * @param gset
     */
    public LeadingEdgeReportViewer() {
        super(NAME, ICON, "Leading edge analysis");
        jbInit();
    }


    private JideTabbedPane sharedTabbedPane;

    private void jbInit() {

        JButton bBuild = new JButton("Load GSEA Results");

        bBuild.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                if (fReportParam.isSpecified() && fDirParam.isSpecified()) {
                    Application.getWindowManager().showMessage("Both cache and a brows'ed directory were specified. Only 1 can be specified. Delete one and try again");
                } else if (!fReportParam.isSpecified() && !fDirParam.isSpecified()) {
                    Application.getWindowManager().showMessage("No GSEA result folder was specified. Specify one and try again");
                } else {
                    SwingWorker<Object, Void> worker = new SwingWorker<Object, Void>() {
                        @Override
                        protected Object doInBackground() throws Exception {
                            try {
                                Application.getWindowManager().getRootFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                                if (fReportParam.isSpecified()) {
                                    curr_gseaResultDir = fReportParam.getReportDir();
                                } else {
                                    curr_gseaResultDir = fDirParam.getDir();
                                }

                                EnrichmentDb edb = ParserFactory.readEdb(curr_gseaResultDir, true);
                                log.debug("edb: " + edb.getQuickInfo());

                                boolean first = false;
                                if (sharedTabbedPane == null) {
                                    first = true;
                                    sharedTabbedPane = new JideTabbedPane();
                                    sharedTabbedPane.setHideOneTab(true);
                                    sharedTabbedPane.setTabEditingAllowed(true);
                                    sharedTabbedPane.setShowCloseButtonOnTab(true);

                                }

                                LeadingEdgeWidget josh_widget = new LeadingEdgeWidget(sharedTabbedPane, edb);
                                sharedTabbedPane.setSelectedComponent(josh_widget.getViewAndSearchComponent());

                                if (first) {
                                    fInstance.remove(fFiller);
                                    fInstance.add(sharedTabbedPane, BorderLayout.CENTER);
                                    //sharedTabbedPane.setTabClosableAt(0, false);
                                }

                                fInstance.revalidate();

                            } catch (Throwable t) {
                                Application.getWindowManager().showError("Trouble loading enrichment database", t);
                            } finally {
                                Application.getWindowManager().getRootFrame().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                            }
                            return null;
                        }
                    };
                    worker.execute();
                }
            }
        });

        this.fReportParam = new ReportCacheChooserParam("Select a GSEA result from the application cache", false);
        this.fDirParam = new DirParam("dir", "[ OR ] Locate a GSEA result folder from the file system", "[ OR ] Locate a GSEA report folder from the file system", false);
        final Param[] params = new Param[]{fReportParam, fDirParam};

        // -------------------------------------------------------------------------------------------- //
        StringBuffer colStr = _createColStr();
        StringBuffer rowStr = _createRowStr(params);

        PanelBuilder builder = ParamSetFormForAFew.createPanelBuilder(colStr, rowStr);
        CellConstraints cc = new CellConstraints();

        int rowcnt = 3;
        for (int i = 0; i < params.length; i++) {
            GFieldPlusChooser chooser = params[i].getSelectionComponent();

            if (params[i].isFileBased()) {
                if (chooser instanceof GDirFieldPlusChooser) {
                    (((GDirFieldPlusChooser) chooser)).getTextField().setBackground(ParamSetFormForAFew.LIGHT_GREEN);
                }
            }

            JLabel label = new JLabel(params[i].getHtmlLabel_v3());

            ParamSetFormForAFew.enableToolTips(label, params[i]);
            builder.add(label, cc.xy(1, rowcnt));
            builder.add(chooser.getComponent(), cc.xy(3, rowcnt));
            rowcnt += 2; // because the spaces also count as a row
        }

        builder.add(new JLabel(""), cc.xy(1, rowcnt));
        builder.add(bBuild, cc.xy(3, rowcnt));

        final JPanel paramPanel = builder.getPanel();
        // -------------------------------------------------------------------------------------------- //

        this.setLayout(new BorderLayout(10, 10));
        this.add(paramPanel, BorderLayout.NORTH);

        this.fFiller = GuiHelper.createWaitingPlaceholder();
        this.add(fFiller, BorderLayout.CENTER);

        //this.add(createControlPanel(), BorderLayout.SOUTH);

        this.revalidate();
    }


    private static StringBuffer _createColStr() {
        return new StringBuffer("220dlu,      4dlu,        200dlu,   4dlu,  4dlu"); // columns
    }

    private static StringBuffer _createRowStr(final Param[] params) {
        StringBuffer rowStr = new StringBuffer();
        rowStr.append("pref, 10dlu,"); // for the spacer
        for (int i = 0; i < params.length + 1; i++) { // +1 for the button
            rowStr.append("pref, 5dlu");
            if (params.length != i - 1) {
                rowStr.append(",");
            }
        }
        return rowStr;
    }

    public JMenuBar getJMenuBar() {
        return EMPTY_MENU_BAR;
    }

} // End class LeadingEdgeReportWidget
