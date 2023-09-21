/*
 * Copyright (c) 2003-2023 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package org.genepattern.gsea;

import edu.mit.broad.genome.alg.ComparatorFactory;
import edu.mit.broad.genome.alg.GeneSetStats;
import edu.mit.broad.genome.alg.gsea.PValueCalculator;
import edu.mit.broad.genome.alg.gsea.PValueCalculatorImpls;
import edu.mit.broad.genome.math.Matrix;
import edu.mit.broad.genome.math.Order;
import edu.mit.broad.genome.objects.BitSetDataset;
import edu.mit.broad.genome.objects.Dataset;
import edu.mit.broad.genome.objects.DefaultDataset;
import edu.mit.broad.genome.objects.DefaultGeneSetMatrix;
import edu.mit.broad.genome.objects.GPWrappers;
import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.objects.GeneSetMatrix;
import edu.mit.broad.genome.objects.RankedList;
import edu.mit.broad.genome.objects.esmatrix.db.EnrichmentDb;
import edu.mit.broad.genome.objects.esmatrix.db.EnrichmentResult;
import gnu.trove.TFloatIntHashMap;

import org.genepattern.data.expr.IExpressionData;
import org.genepattern.heatmap.ColorScheme;
import org.genepattern.heatmap.HeatMapComponent;
import org.genepattern.menu.jfree.JFreeMenuBar;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;

/**
 * @author jgould
 */
public class LeadingEdgeAnalysis {
    public static final LeadingEdgeAnalysis runAnalysis(EnrichmentDb edb,
            String[] gsetNames, JTabbedPane tabbedPane) {
        Frame parentFrame = (tabbedPane == null) ? null : 
            (Frame)tabbedPane.getTopLevelAncestor();
        
        Dimension containerDim = (tabbedPane == null) ? null :
            new Dimension(tabbedPane.getWidth(), tabbedPane.getHeight());
        
        final GeneSet[] gsets = new GeneSet[gsetNames.length];
        for (int r = 0; r < gsetNames.length; r++) {
            EnrichmentResult result = edb.getResultForGeneSet(gsetNames[r]);
            gsets[r] = result.getSignal().getAsGeneSet();
        }
        final GeneSetMatrix lev_gmx = new DefaultGeneSetMatrix(
                "leading_edge_matrix_for_" + edb.getName(), gsets);
        LeadingEdgeAnalysis analysis = new LeadingEdgeAnalysis(lev_gmx, edb
                .getRankedList(), parentFrame, containerDim);
        analysis.setResultDirectory(edb.getEdbDir());
        return analysis;
    }
    
    public static EnrichmentResult[] getAllResultsFromEdb(EnrichmentDb edb_original) {
        String normModeName = "meandiv"; // hard coded
        final PValueCalculator pvc = new PValueCalculatorImpls.GseaImpl(
                normModeName);
        final EnrichmentResult[] results = pvc
                .calcNPValuesAndFDR(edb_original.getResults());
        final EnrichmentDb edb = edb_original.cloneDeep(results);
        EnrichmentResult[] enrichmentResults = edb
                .getResults(new ComparatorFactory.EnrichmentResultByNESComparator(Order.DESCENDING));
        return enrichmentResults;
    }

    private JPanel mainComponent;
    
    private JaccardHistogram jaccardHistogram = new JaccardHistogram();
    private final GeneHistogram geneHistogram = new GeneHistogram();
    private HeatMapComponent geneSetSimilarityHeatmap;

    private LeadingEdgePanel leadingEdgePanel;

    private Dataset clusteredDataset = null;

    private final static String LEADING_EDGE_MATRIX_KEY = "Leading Edge Matrix";

    private final static String GENE_SET_SIMILARITY_MATRIX_KEY = "Gene Set Similarity Matrix";

    private final static String GENE_HISTOGRAM_KEY = "Gene Histogram";

    private final static String JACCARD_HISTOGRAM_KEY = "Jaccard Histogram of Gene Sets";

    private Dataset _morph(Dataset ds, RankedList rl) {
        Matrix m = new Matrix(ds.getNumRow(), ds.getNumCol());
        for (int r = 0; r < ds.getNumRow(); r++) {
            for (int c = 0; c < ds.getNumCol(); c++) {
                float score = rl.getScore(ds.getColumnName(c));
                float value = ds.getElement(r, c);
                if (value == 1) {
                    m.setElement(r, c, score);
                } else {
                    m.setElement(r, c, value); // ought to be zero
                }
            }
        }

        return new DefaultDataset(ds.getName(), m, ds.getRowNames(), ds
                .getColumnNames(), ds.getAnnot());
    }

    public void setResultDirectory(File file) {
        leadingEdgePanel.setResultDirectory(file);
    }

    public LeadingEdgeAnalysis(final GeneSetMatrix lev_gmx, RankedList rankedList,
            final Frame parent, Dimension containerDim) {
        final GeneSet[] gsets = lev_gmx.getGeneSets();
        final Dataset lev_ds = new BitSetDataset(lev_gmx).toDataset();
    
        try {
            clusteredDataset = HCLAlgorithm.cluster(lev_ds);

        } catch (Throwable t) {
            clusteredDataset = lev_ds;
            t.printStackTrace();
        }

        // TODO: refactor the following to separate UI from computation and reporting.
        
        final Dataset lev_ds_clustered_m = rankedList != null ? _morph(
                clusteredDataset, rankedList) : clusteredDataset;

        leadingEdgePanel = new LeadingEdgePanel(parent);
        ColorScheme cs = rankedList != null ? GPWrappers
                .createColorScheme_for_lev_with_score(lev_ds_clustered_m)
                : new ColorScheme() {

            public Color getColor(int row, int column) {
                return lev_ds_clustered_m.getElement(row, column) == 0 ? Color.white
                        : Color.yellow;
            }

            public void setDataset(IExpressionData d) {
            }

            public Component getLegend() {
                return null;
            }

        };

        leadingEdgePanel.setData(GPWrappers
                .createIExpressionData(lev_ds_clustered_m), cs);

        GeneSetStats stats = new GeneSetStats();
        GeneSetStats.RedStruc rs = stats.calcRedundancy(gsets, false);

        GeneSetSimilarityPanel geneSetSimilarityPanel = new GeneSetSimilarityPanel(
                parent);
        // reorder genesets so that they are in the same order as the clustered
        // gene sets
        GeneSet[] reorderedGeneSets = new GeneSet[gsets.length];
        Map<String, Integer> geneSetName2Index = new HashMap<String, Integer>();
        for (int i = 0; i < gsets.length; i++) {
            geneSetName2Index.put(gsets[i].getName(true), i);
        }

        for (int i = 0; i < gsets.length; i++) {
            String geneSetName = lev_ds_clustered_m.getRowName(i);
            reorderedGeneSets[i] = gsets[geneSetName2Index.get(geneSetName)];
        }

        geneSetSimilarityPanel.setGeneSets(reorderedGeneSets);

        TFloatIntHashMap jaccardToOccurrencesMap = rs.jaccardDistrib;
        jaccardHistogram.setJaccardToOccurrencesMap(jaccardToOccurrencesMap);
        jaccardHistogram.setPreferredSize(new Dimension(100, 100));

        RankedList featureFrequency = rs.featureFreq;
        geneHistogram.setFeatureFrequency(featureFrequency,
                rankedList != null ? rankedList : null);

        ChartMouseListener listener = new ChartMouseListener() {

            public void chartMouseClicked(ChartMouseEvent event) {
                int mouseX = event.getTrigger().getX();
                int mouseY = event.getTrigger().getY();
                int index = geneHistogram.getXIndex(mouseX, mouseY);
                if (index != -1) {
                    String geneName = geneHistogram.getGeneName(index);
                    int columnIndex = lev_ds_clustered_m
                            .getColumnIndex(geneName);
                    leadingEdgePanel.getHeatMapComponent().getSampleTable()
                            .setColumnSelectionInterval(columnIndex,
                                    columnIndex);
                }
            }

            public void chartMouseMoved(ChartMouseEvent event) {
            }
        };

        geneHistogram.getChartPanel().addChartMouseListener(listener);
        geneHistogram.setPreferredSize(new Dimension(100, 100));
        
        JScrollPane leadingEdgeSP = new JScrollPane(leadingEdgePanel.getHeatMapComponent());
        leadingEdgeSP.setBorder(null);

        geneSetSimilarityHeatmap = geneSetSimilarityPanel.getHeatMapComponent();
        JScrollPane geneSetSP = new JScrollPane(geneSetSimilarityHeatmap);
        geneSetSP.setBorder(null);

        // Skip over the rest if we are running without a Frame (CLI reporting, for example)
        if (parent == null) return;

        mainComponent = new JPanel();
        JSplitPane topBottomSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        JSplitPane leftRightTopSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        JSplitPane leftRightBotSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        
        topBottomSplit.add(leftRightTopSplit);
        topBottomSplit.add(leftRightBotSplit);
        topBottomSplit.setResizeWeight(0.5d);
        leftRightTopSplit.setResizeWeight(0.5d);
        leftRightBotSplit.setResizeWeight(0.5d);
        mainComponent.add(topBottomSplit);
        
        UIManager.getDefaults().put("JideSplitPane.dividerSize", 9);
        int width = containerDim.width;
        int height = containerDim.height;
        mainComponent.setPreferredSize(containerDim);

        JPanel f = new JPanel(new BorderLayout());
        f.setToolTipText(LEADING_EDGE_MATRIX_KEY);
        f.add(leadingEdgePanel.getMenuBar(), BorderLayout.PAGE_START);
        f.add(leadingEdgeSP, BorderLayout.CENTER);
        f.setPreferredSize(new Dimension(width / 2, height / 2));
        leftRightTopSplit.add(f);
        f.setVisible(true);
        
        f = new JPanel(new BorderLayout());
        f.setToolTipText(GENE_SET_SIMILARITY_MATRIX_KEY);
        f.add(geneSetSimilarityPanel.getMenuBar(), BorderLayout.PAGE_START);
        f.add(geneSetSP, BorderLayout.CENTER);
        f.setPreferredSize(new Dimension(width / 2, height / 2));
        leftRightTopSplit.add(f);
        f.setVisible(true);

        f = new JPanel(new BorderLayout());
        f.setToolTipText(GENE_HISTOGRAM_KEY);
        JFreeMenuBar menuBar = new JFreeMenuBar(geneHistogram
                .getChartPanel(), parent);
        f.add(menuBar, BorderLayout.PAGE_START);
        f.add(new JScrollPane(geneHistogram), BorderLayout.CENTER);
        f.setPreferredSize(new Dimension(width / 2, height / 2));
        leftRightBotSplit.add(f);
        f.setVisible(true);

        f = new JPanel(new BorderLayout());
        f.setToolTipText(JACCARD_HISTOGRAM_KEY);
        menuBar = new JFreeMenuBar(jaccardHistogram.getChartPanel(), parent);
        f.add(menuBar, BorderLayout.PAGE_START);
        f.add(new JScrollPane(jaccardHistogram), BorderLayout.CENTER);
        f.setPreferredSize(new Dimension(width / 2, height / 2));
        leftRightBotSplit.add(f);
        f.setVisible(true);

        topBottomSplit.setDividerLocation(0.5d);
        leftRightTopSplit.setDividerLocation(0.5d);
        leftRightBotSplit.setDividerLocation(0.5d);
    }

    public Component getComponent() {
        return mainComponent;
    }
    
    public JaccardHistogram getJaccardHistogram() {
        return jaccardHistogram;
    }

    public GeneHistogram getGeneHistogram() {
        return geneHistogram;
    }

    public HeatMapComponent getGeneSetSimilarityHeatmap() {
        return geneSetSimilarityHeatmap;
    }

    public LeadingEdgePanel getLeadingEdgePanel() {
        return leadingEdgePanel;
    }
    
    public Dataset getClusteredDataset() {
        return clusteredDataset;
    }
}
