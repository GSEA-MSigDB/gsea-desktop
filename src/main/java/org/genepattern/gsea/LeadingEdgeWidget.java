/*
 * Copyright (c) 2003-2023 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package org.genepattern.gsea;

import edu.mit.broad.genome.JarResources;
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
import org.jfree.ui.SortableTableModel;

import xtools.api.XToolsApplication;
import xtools.api.param.ToolParamSet;
import xtools.gsea.LeadingEdgeTool;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingWorker;
import javax.swing.UIManager;

import static java.lang.System.exit;
import static java.lang.System.setProperty;

/**
 * @author Joshua Gould
 * @author Aravind Subramanian
 */
public class LeadingEdgeWidget implements Widget {
    private static final Icon ICON = JarResources.getIcon("Lev16_b.gif");
    private EnrichmentDb edb;
    private JTabbedPane tabbedPane;

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

    static {
        // don't show accelerators in menus
        setProperty(PlotAction.SHOW_ACCELERATORS_PROPERTY, "false");
    }

    public LeadingEdgeWidget(EnrichmentDb edb) {
        if (edb == null) {
            throw new IllegalArgumentException("Param edb cannot be null");
        }

        runningInGenePattern = true;
        tabbedPane = new JTabbedPane();
        this.edb = edb;
        init(edb);
        tabbedPane.addTab("GSEA Results", viewAndSearchComponent);
    }

    public LeadingEdgeWidget(JTabbedPane tabbedPane, EnrichmentDb edb) {
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

        viewAndSearchComponent = new ViewAndSearchComponent("Run leading edge analysis", createTableModel(),
                runListener, reportListener);
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

    private SortableTableModel createTableModel() {
        return new MyTableModel(LeadingEdgeAnalysis.getAllResultsFromEdb(edb));
    }

    private static class MyTableModel extends SortableTableModel {
        private String[] columnNames = {"Gene Set", "Size", "ES", "NES", "NOM p-val",
                "FDR q-val", "FWER p-val", "Rank at Max", "Leading Edge"};
        private Class[] columnClasses = {String.class, Integer.class, Float.class,
                Float.class, Float.class, Float.class, Float.class,
                Integer.class, String.class};
        private Comparator<EnrichmentResult> BY_GS_NAME = new Comparator<EnrichmentResult>() {
            public int compare(EnrichmentResult result1, EnrichmentResult result2) {
                if (result1 == null) return result2 == null ? 0 : -1;
                if (result2 == null) return 1;
                return result1.getGeneSetName().compareTo(result2.getGeneSetName());
            }
        };
        private Comparator<EnrichmentResult> BY_GS_MEMBERS = new Comparator<EnrichmentResult>() {
            public int compare(EnrichmentResult result1, EnrichmentResult result2) {
                if (result1 == null) return result2 == null ? 0 : -1;
                if (result2 == null) return 1;
                return result1.getGeneSet().getNumMembers() - result2.getGeneSet().getNumMembers();
            }
        };
        private Comparator<EnrichmentResult> BY_ES = new Comparator<EnrichmentResult>() {
            public int compare(EnrichmentResult result1, EnrichmentResult result2) {
                if (result1 == null) return result2 == null ? 0 : -1;
                if (result2 == null) return 1;
                return Float.compare(result1.getScore().getES(), result2.getScore().getES());
            }
        };
        private Comparator<EnrichmentResult> BY_NES = new Comparator<EnrichmentResult>() {
            public int compare(EnrichmentResult result1, EnrichmentResult result2) {
                if (result1 == null) return result2 == null ? 0 : -1;
                if (result2 == null) return 1;
                return Float.compare(result1.getScore().getNES(), result2.getScore().getNES());
            }
        };
        private Comparator<EnrichmentResult> BY_NP = new Comparator<EnrichmentResult>() {
            public int compare(EnrichmentResult result1, EnrichmentResult result2) {
                if (result1 == null) return result2 == null ? 0 : -1;
                if (result2 == null) return 1;
                return Float.compare(result1.getScore().getNP(), result2.getScore().getNP());
            }
        };
        private Comparator<EnrichmentResult> BY_FDR = new Comparator<EnrichmentResult>() {
            public int compare(EnrichmentResult result1, EnrichmentResult result2) {
                if (result1 == null) return result2 == null ? 0 : -1;
                if (result2 == null) return 1;
                return Float.compare(result1.getScore().getFDR(), result2.getScore().getFDR());
            }
        };
        private Comparator<EnrichmentResult> BY_FWER = new Comparator<EnrichmentResult>() {
            public int compare(EnrichmentResult result1, EnrichmentResult result2) {
                if (result1 == null) return result2 == null ? 0 : -1;
                if (result2 == null) return 1;
                return Float.compare(result1.getScore().getFWER(), result2.getScore().getFWER());
            }
        };
        private Comparator<EnrichmentResult> BY_RANK_AT_MAX = new Comparator<EnrichmentResult>() {
            public int compare(EnrichmentResult result1, EnrichmentResult result2) {
                if (result1 == null) return result2 == null ? 0 : -1;
                if (result2 == null) return 1;
                return result1.getSignal().getRankAtMax() - result2.getSignal().getRankAtMax();
            }
        };
        private Comparator<EnrichmentResult> BY_LEADING_EDGE = new Comparator<EnrichmentResult>() {
            public int compare(EnrichmentResult result1, EnrichmentResult result2) {
                if (result1 == null) return result2 == null ? 0 : -1;
                if (result2 == null) return 1;
                return EnrichmentReports.getLeadingEdge(result1).compareTo(EnrichmentReports.getLeadingEdge(result2));
            }
        };

        private EnrichmentResult[] enrichmentResults;
        private EnrichmentResult[] sortedResults;

        public MyTableModel(EnrichmentResult[] enrichmentResults) {
            this.enrichmentResults = enrichmentResults;
            this.sortedResults = enrichmentResults;
        }

        @Override
        public boolean isSortable(int column) {
            return true;
        }

        @Override
        public void sortByColumn(int column, boolean ascending) {
            super.sortByColumn(column, ascending);
            Arrays.parallelSort(sortedResults, comparatorByColumn(column, ascending));
        }

        public Comparator<EnrichmentResult> comparatorByColumn(int column, boolean ascending) {
            switch (column) {
            case 0:
                return ascending ? BY_GS_NAME : BY_GS_NAME.reversed();
            case 1:
                return ascending ? BY_GS_MEMBERS : BY_GS_MEMBERS.reversed();
            case 2:
                return ascending ? BY_ES : BY_ES.reversed();
            case 3:
                return ascending ? BY_NES : BY_NES.reversed();
            case 4:
                return ascending ? BY_NP : BY_NP.reversed();
            case 5:
                return ascending ? BY_FDR : BY_FDR.reversed();
            case 6:
                return ascending ? BY_FWER : BY_FWER.reversed();
            case 7:
                return ascending ? BY_RANK_AT_MAX : BY_RANK_AT_MAX.reversed();
            case 8:
                return ascending ? BY_LEADING_EDGE : BY_LEADING_EDGE.reversed();
            default:
                return ascending ? BY_GS_NAME : BY_GS_NAME.reversed();
            }
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
            GeneSet gset = sortedResults[rowIndex].getGeneSet();
            EnrichmentScore score = sortedResults[rowIndex].getScore();
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
                    return sortedResults[rowIndex].getSignal().getRankAtMax();
                case 8:
                    return EnrichmentReports.getLeadingEdge(sortedResults[rowIndex]);
            }
            return null;
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
                            tabbedPane);
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
