/*
 * Copyright (c) 2003-2023 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package org.genepattern.gsea;

import com.jidesoft.swing.JideTabbedPane;

import edu.mit.broad.genome.JarResources;
import edu.mit.broad.genome.math.XMath;
import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.objects.esmatrix.db.EnrichmentDb;
import edu.mit.broad.genome.objects.esmatrix.db.EnrichmentResult;
import edu.mit.broad.genome.objects.esmatrix.db.EnrichmentScore;
import edu.mit.broad.genome.parsers.ParserFactory;
import edu.mit.broad.genome.reports.EnrichmentReports;
import edu.mit.broad.genome.utils.ZipUtility;
import edu.mit.broad.xbench.core.Widget;
import edu.mit.broad.xbench.core.api.Application;
import edu.mit.broad.xbench.tui.TaskManager;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.genepattern.menu.PlotAction;
import org.genepattern.uiutil.UIUtil;

import xtools.api.XToolsApplication;
import xtools.api.param.ToolParamSet;
import xtools.gsea.LeadingEdgeTool;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import static java.lang.System.exit;
import static java.lang.System.setProperty;

/**
 * @author Joshua Gould
 * @author Aravind Subramanian
 */
public class LeadingEdgeWidget implements Widget {
    private static final Icon ICON = JarResources.getIcon("Lev16_b.gif");
    private EnrichmentDb edb;
    private JideTabbedPane tabbedPane;

    // counts how many times analysis was run
    private static int runs = 0;
    private ViewAndSearchComponent viewAndSearchComponent;
    private JLabel positiveLabel;
    private JLabel negativeLabel;
    private JPanel phenotypePanel;

    // whether viewer is running in GenePattern
    private boolean runningInGenePattern;
    private JFileChooser htmlReportDirChooser;

    // index in table of gene set
    private final static int GENE_SET_INDEX = 0;

    // index in table of score
    private final static int SCORE_INDEX = 2;

    static {
        // don't show accelerators in menus
        setProperty(PlotAction.SHOW_ACCELERATORS_PROPERTY, "false");
    }

    public LeadingEdgeWidget(EnrichmentDb edb) {
        if (edb == null) {
            throw new IllegalArgumentException("Param edb cannot be null");
        }

        runningInGenePattern = true;
        tabbedPane = new JideTabbedPane();
        tabbedPane.setHideOneTab(true);
        tabbedPane.setTabEditingAllowed(true);
        tabbedPane.setShowCloseButtonOnTab(true);
        this.edb = edb;
        init(edb);
        tabbedPane.addTab("GSEA Results", viewAndSearchComponent);
        tabbedPane.setTabClosableAt(0, false);
    }

    public LeadingEdgeWidget(JideTabbedPane tabbedPane, EnrichmentDb edb) {
        if (edb == null) {
            throw new IllegalArgumentException("Param edb cannot be null");
        }

        runningInGenePattern = false;
        this.tabbedPane = tabbedPane;
        UIUtil.setMessageHandler(new XToolsMessageHandler());
        this.edb = edb;
        init(edb);
        tabbedPane.addTab("GSEA Results", viewAndSearchComponent);
    }

    public JMenuBar getJMenuBar() {
        return EMPTY_MENU_BAR;
    }

    public JComponent getWrappedComponent() {
        return tabbedPane;
    }

    public JComponent getViewAndSearchComponent() {
        return viewAndSearchComponent;
    }

    public String getAssociatedTitle() {
        return null;
    }

    public Icon getAssociatedIcon() {
        return ICON;
    }

    private void init(EnrichmentDb edb) {
        positiveLabel = new JLabel(" ");
        positiveLabel.setForeground(Color.RED);

        String tcp = "na pos";
        String tcn = "na neg";
        if (edb.getTemplate() != null) {
            tcn = edb.getTemplate().getClassName(0);
            tcn = edb.getTemplate().getClassName(1);
        }

        positiveLabel.setText("positive phenotype: " + tcp + "   ");

        negativeLabel = new JLabel(" ");
        negativeLabel.setForeground(Color.BLUE);
        negativeLabel.setText("negative phenotype: " + tcn);

        phenotypePanel = new JPanel();
        phenotypePanel.add(positiveLabel);
        phenotypePanel.add(negativeLabel);

        final ActionListener runListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                runAnalysis();
            }
        };
        final ActionListener reportListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                buildHtmlReport();
            }
        };

        viewAndSearchComponent = new ViewAndSearchComponent(
                "Run leading edge analysis", runListener,
                reportListener);

        viewAndSearchComponent.setTableModel(createTableModel());
        viewAndSearchComponent.getTable().setDefaultRenderer(String.class,
                new ESRenderer(GENE_SET_INDEX, SCORE_INDEX));
    }

    private void buildHtmlReport() {
        File outputDir = edb.getEdbDir();
        if (runningInGenePattern) {
            if (htmlReportDirChooser == null) {
                htmlReportDirChooser = new JFileChooser();
                htmlReportDirChooser.setDialogTitle("Choose output directory");
                htmlReportDirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                htmlReportDirChooser.setMultiSelectionEnabled(false);
            }
            if (htmlReportDirChooser.showOpenDialog(tabbedPane
                    .getTopLevelAncestor()) != JFileChooser.APPROVE_OPTION) {
                return;
            }
            outputDir = htmlReportDirChooser.getSelectedFile();
        }
        final File _outputDir = outputDir;
        tabbedPane.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            SwingWorker<Object, Void> worker = new SwingWorker<Object, Void>() {
                @Override
                protected Object doInBackground() throws Exception {
                    LeadingEdgeTool tool = new LeadingEdgeTool();
                    ToolParamSet paramSet = (ToolParamSet)tool.getParamSet();
                    if (runningInGenePattern) {
                        paramSet.getAnalysisDirParam().setValue(_outputDir);
                    }

                    // Set necessary params before running the Tool.  Note that running in the TaskManager will
                    // actually clone the Tool & ParamSet via Properties Strings, so it must be able to serialize
                    // cleanly. This is a problem for certain Gene Sets that contain commas as this is the default
                    // name separator, so we override it here to use the semicolon and avoid the issue.
                    // For the same reason, we need to serialize the gsets String array with the semicolon so
                    // that it is properly done - the cloning process will use commas. 
                    // We could set other params here as well, particularly image format.
                    // TODO: we could probably avoid this when runningInGenePattern as the Tool executes directly.
                    paramSet.getParam("altDelim").setValue(";");
                    String gsets = StringUtils.join(viewAndSearchComponent.getSelectedColumnArray(GENE_SET_INDEX), ";");
                    paramSet.getParam("gsets").setValue(gsets);
                    paramSet.getParam("dir").setValue(edb.getEdbDir());
                    
                    if (!runningInGenePattern) {
                        TaskManager.getInstance().run(tool, paramSet, Thread.MIN_PRIORITY);
                    } else {
                        tool.execute();
                    }
                    return null;
                }
            };
            worker.execute();
        } catch (Throwable t) {
            t.printStackTrace();
            tabbedPane.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            UIUtil.showErrorDialog(tabbedPane.getTopLevelAncestor(), "An error occurred while building the HTML report");
        }
        tabbedPane.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

    private TableModel createTableModel() {
        return new MyTableModel(LeadingEdgeAnalysis.getAllResultsFromEdb(edb));
    }

    private static class MyTableModel extends AbstractTableModel {
        private String[] columnNames = {"Gene Set", "Size", "ES", "NES", "NOM p-val",
                "FDR q-val", "FWER p-val", "Rank at Max", "Leading Edge"};

        private Class[] columnClasses = {String.class, Integer.class, Float.class,
                Float.class, Float.class, Float.class, Float.class,
                Integer.class, String.class};

        private EnrichmentResult[] enrichmentResults;

        public MyTableModel(EnrichmentResult[] enrichmentResults) {
            this.enrichmentResults = enrichmentResults;
        }

        public int getRowCount() {
            return enrichmentResults.length;
        }

        public int getColumnCount() {
            return columnNames.length;
        }

        public Class getColumnClass(int j) {
            return columnClasses[j];
        }

        public String getColumnName(int j) {
            return columnNames[j];
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            GeneSet gset = enrichmentResults[rowIndex].getGeneSet();
            EnrichmentScore score = enrichmentResults[rowIndex].getScore();
            switch (columnIndex) {
                case 0:
                    return gset.getName(true);
                case 1:
                    return gset.getNumMembers();
                case 2:
                    return score.getES();
                case 3:
                    return score.getNES();
                case 4:
                    return score.getNP();
                case 5:
                    return score.getFDR();
                case 6:
                    return score.getFWER();
                case 7:
                    return enrichmentResults[rowIndex].getSignal().getRankAtMax();
                case 8:
                    return EnrichmentReports.getLeadingEdge(enrichmentResults[rowIndex]);
            }
            return null;
        }
    }

    /*
     * GramTableCellRenderer for table headers. Headers in bold font -- looks
     * like excel headers.
     */
    private class ESRenderer extends DefaultTableCellRenderer {
        private int nameColumnIndex;
        private int scoreColumnIndex;

        public ESRenderer(int nameColumnIndex, int scoreColumnIndex) {
            this.nameColumnIndex = nameColumnIndex;
            this.scoreColumnIndex = scoreColumnIndex;
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, 
                boolean hasFocus, int row, int col) {
            row = viewAndSearchComponent.getTable().getActualRowAt(row);

            super.getTableCellRendererComponent(table, value, isSelected,
                    hasFocus, row, col);

            if (col == nameColumnIndex) {
                float es = ((Float) table.getValueAt(row, scoreColumnIndex))
                        .floatValue();
                if (XMath.isPositive(es)) {
                    this.setForeground(Color.RED);
                } else {
                    this.setForeground(Color.BLUE);
                }
                if (value != null) this.setText(value.toString());
            } else {
                this.setForeground(Color.BLACK);
            }

            return this;
        }
    }

    private void runAnalysis() {
        tabbedPane.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            SwingWorker<LeadingEdgeAnalysis, Void> worker = new SwingWorker<LeadingEdgeAnalysis, Void>() {
                @Override
                protected LeadingEdgeAnalysis doInBackground() throws Exception {
                    runs++;
                    try {
                    return LeadingEdgeAnalysis.runAnalysis(edb, 
                            viewAndSearchComponent.getSelectedColumnArray(GENE_SET_INDEX), 
                            (Frame) tabbedPane.getTopLevelAncestor());
                    }
                    catch (Throwable t) {
                        t.printStackTrace();
                        tabbedPane.setCursor(Cursor
                                .getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                        UIUtil.showErrorDialog(tabbedPane.getTopLevelAncestor(),
                                "An error occurred while running leading edge analysis");
                        return null;
                    }
                }
                
                @Override
                protected void done() {
                    try {
                        LeadingEdgeAnalysis analysis = get();
                        if (analysis == null) return;

                        tabbedPane.addTab("Leading Edge Analysis-" + runs, analysis
                                .getComponent());
                        tabbedPane.setSelectedComponent(analysis.getComponent());
                        tabbedPane.setCursor(Cursor
                                .getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    } catch (Throwable t) {
                        t.printStackTrace();
                        tabbedPane.setCursor(Cursor
                                .getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                        UIUtil.showErrorDialog(tabbedPane.getTopLevelAncestor(),
                                "An error occurred while running leading edge analysis");
                    }
                }
            };
            worker.execute();
        } catch (Throwable t) {
            t.printStackTrace();
            tabbedPane.setCursor(Cursor
                    .getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            UIUtil.showErrorDialog(tabbedPane.getTopLevelAncestor(),
                    "An error occurred while running leading edge analysis");
        }
    }

    /**
     * <p>
     * This main method supports call into Widget by GenePattern module
     * GSEALeadingEdgeViewer, which is a java-based visualizer of
     * GenePattern GSEA module results.
     * </p>
     * <p>
     * Note that it is necessary to provide a JIDE Software license key in order to use JIDE Components, Dock and Grids.
     * The key included here was generously provided to the GSEA development team for use with the GSEA project.
     * Other developers and commercial users should contact http://www.jidesoft.com to determine what type of license
     * is needed.
     * </p>
     * @param args a single argument, containing path of the zip
     *             output file generated by the GSEA GP module.
     */
    public static void main(String[] args) { // for running using GenePattern
        com.jidesoft.utils.Lm.verifyLicense(
                "Broad Institute of MIT and Harvard",
                "Gene set  enrichment analysis java desktop application",
                "YSjBO6OJfF9WbavzI73Jt1HgDI4x9L21");
        if (!SystemUtils.IS_OS_MAC_OSX) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (args.length != 1) {
            UIUtil.showMessageDialog(null,
                    "org.genepattern.gsea.LeadingEdgeWidget.main expecting single command line argument");
            exit(0);
        }

        String zipFile = args[0];
        File dir = null;
        try {
            dir = File.createTempFile("gsea", null);
        } catch (IOException e) {
            e.printStackTrace();
            UIUtil.showMessageDialog(null,
                    "An error occurred while reading the input zip file");
            exit(0);
        }
        dir.delete();
        dir.mkdir();
        final File f = dir;
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    FileUtils.deleteDirectory(f);
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                    UIUtil.showMessageDialog(null,
                            "unable to delete directory containing unpacked GSEA zip file");
                }
            }
        });

        ZipUtility zipUtil = new ZipUtility();
        try {
            zipUtil.unzip(new File(zipFile), dir);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            UIUtil.showMessageDialog(null,
                            "An error occurred while unzipping directory archive");
            exit(0);
        }

        String title = "Leading Edge Viewer - " + new File(zipFile).getName();
        Application.registerHandler(new XToolsApplication());

        EnrichmentDb edb = null;
        try {
            edb = ParserFactory.readEdb(dir, true, true);
        } catch (Exception e1) {
            e1.printStackTrace();
            UIUtil.showMessageDialog(null,
                    "An error occurred while reading the input file");
            exit(0);
        }
        JFrame frame = new JFrame();
        frame.setTitle(title);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        LeadingEdgeWidget widget = new LeadingEdgeWidget(edb);
        frame.getContentPane().add(widget.tabbedPane);
        UIUtil.sizeToScreen(frame);
        frame.setVisible(true);
    }
}
