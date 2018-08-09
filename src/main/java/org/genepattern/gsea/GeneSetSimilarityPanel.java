/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package org.genepattern.gsea;

import edu.mit.broad.genome.alg.AlgUtils;
import edu.mit.broad.genome.objects.GeneSet;
import org.genepattern.data.expr.ExpressionData;
import org.genepattern.data.expr.IExpressionData;
import org.genepattern.data.matrix.DoubleMatrix2D;
import org.genepattern.data.matrix.IExpressionDataUtil;
import org.genepattern.heatmap.GradientColorScheme;
import org.genepattern.heatmap.HeatMapComponent;
import org.genepattern.heatmap.HeatMapPanel.ToolTipProvider;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;

/**
 * @author Joshua Gould
 */
public class GeneSetSimilarityPanel {
    HeatMapComponent heatMap;

    JPanel legend;

    private final static int JACCARD = 0;

    private final static int HYPER_GEOM = 1;

    private final static int SIM = 2;

    private int similarityMeasure = JACCARD;

    private NumberFormat numberFormat;

    private GeneSet[] geneSets;

    public GeneSetSimilarityPanel(Frame parent) {
        numberFormat = NumberFormat.getNumberInstance();
        numberFormat.setMaximumFractionDigits(4);
        GradientColorScheme colorScheme = new GradientColorScheme(0, 1, 0.5,
                Color.GREEN, Color.RED, Color.WHITE);
        colorScheme.setUseDoubleGradient(false);
        legend = (JPanel) colorScheme.getLegend();

        heatMap = new HeatMapComponent(parent, IExpressionDataUtil
                .createRandomData(1, 1), // XXX set dummy data initially
                legend);
        heatMap.getHeatMapPanel().setUpperTriangular(true);
        heatMap.getHeatMapPanel().setToolTipProvider(new ToolTipProvider() {

            public String getToolTipText(int row, int column) {
                IExpressionData data = heatMap.getExpressionData();
                String value = numberFormat.format(data.getValue(row, column));
                int intersection = AlgUtils.intersectSize(geneSets[row],
                        geneSets[column]);
                int union = AlgUtils.unionAllCount(new GeneSet[]{
                        geneSets[row], geneSets[column]});

                return "<html>" + value + " (intersection=" + intersection
                        + ", union=" + union + ")<br>" + data.getRowName(row)
                        + " (size=" + geneSets[row].getNumMembers() + ")"
                        + "<br>" + data.getColumnName(column) + " (size="
                        + geneSets[column].getNumMembers() + ")";

            }

        });
        heatMap.setFeatureUIString("Gene Set");
        heatMap.setSampleUIString("Gene Set");
        heatMap.setShowFeatureAnnotator(false);
        heatMap.setShowSampleAnnotator(false);
        heatMap.setShowColorSchemeOptions(false);
        heatMap.setShowRowDescriptions(false);
        heatMap.setRowSize(12);
        heatMap.setColumnSize(12);
        heatMap.setColorConverter(colorScheme);
        heatMap.setShowFeatureTableHeader(false);

        heatMap.getHeatMapPanel().addPropertyChangeListener("columnSize",
                new PropertyChangeListener() {

                    public void propertyChange(PropertyChangeEvent evt) {
                        Integer newValue = (Integer) evt.getNewValue();
                        Dimension size = legend.getPreferredSize();
                        size.width = heatMap.getExpressionData()
                                .getColumnCount()
                                * newValue.intValue();
                        legend.setPreferredSize(size);
                    }

                });
    }

    public void setGeneSets(GeneSet[] geneSets) {
        this.geneSets = geneSets;
        double[][] similarityMatrix = new double[geneSets.length][geneSets.length];
        String[] names = new String[geneSets.length];
        for (int i = 0; i < geneSets.length; i++) {
            names[i] = geneSets[i].getName(true);
            for (int j = i; j < geneSets.length; j++) {
                int intersection = AlgUtils.intersectSize(geneSets[i],
                        geneSets[j]);
                int union = AlgUtils.unionAllCount(new GeneSet[]{geneSets[i],
                        geneSets[j]});
                int geneSet_i_size = geneSets[i].getNumMembers();
                int geneSet_j_size = geneSets[j].getNumMembers();
                if (similarityMeasure == JACCARD) {
                    double jaccard = ((double) intersection) / union;
                    similarityMatrix[i][j] = jaccard;
                } else if (similarityMeasure == HYPER_GEOM) {
                    // x vector of quantiles representing the number of white
                    // balls drawn without replacement from an urn which
                    // contains both black and white balls.

                    // m the number of white balls in the urn.
                    // n the number of black balls in the urn.
                    // k the number of balls drawn from the urn.
                    int populationSize = union; // = m + n
                    int numberOfSuccesses = geneSet_i_size; // m
                    int sampleSize = geneSet_j_size; // k

                    int x = intersection;
                    // DistributionFactory f =
                    // DistributionFactory.newInstance();
                    // HypergeometricDistribution dist = f
                    // .createHypergeometricDistribution(populationSize,
                    // numberOfSuccesses, sampleSize);

                    // try {
                    // double p = 1 - dist.cumulativeProbability(x);
                    // similarityMatrix[i][j] = p;

                    // } catch (MathException e) {
                    // e.printStackTrace();
                    // }
                } else if (similarityMeasure == SIM) {
                    double sim = Math.sqrt((intersection / geneSets[i]
                            .getNumMembers())
                            * (intersection / geneSets[j].getNumMembers()));
                    similarityMatrix[i][j] = sim;
                }
            }

        }
        DoubleMatrix2D matrix = new DoubleMatrix2D(similarityMatrix, names,
                names);
        IExpressionData data = new ExpressionData(matrix, null, null);
        heatMap.setExpressionData(data);

        Dimension size = legend.getPreferredSize();
        size.width = heatMap.getExpressionData().getColumnCount()
                * heatMap.getHeatMapPanel().getColumnSize();
        legend.setPreferredSize(size);
    }

    public HeatMapComponent getHeatMapComponent() {
        return heatMap;
    }

    public JMenuBar getMenuBar() {
        heatMap.setOptionsDialogOptions(false, false, false);
        return heatMap.createMenuBar(false, false, false, false);
    }
}
