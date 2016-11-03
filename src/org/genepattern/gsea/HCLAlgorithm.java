/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package org.genepattern.gsea;

import org.genepattern.data.expr.IExpressionData;
import org.genepattern.data.matrix.IExpressionDataUtil;
import org.tigr.microarray.mev.cluster.algorithm.AlgorithmData;
import org.tigr.microarray.mev.cluster.algorithm.AlgorithmException;
import org.tigr.microarray.mev.cluster.algorithm.impl.HCL;
import org.tigr.util.FloatMatrix;

import edu.mit.broad.genome.StandardException;
import edu.mit.broad.genome.objects.Dataset;
import edu.mit.broad.genome.objects.GPWrappers;

import java.util.Arrays;

/**
 * @author Joshua Gould
 */
public class HCLAlgorithm {

    private final static String AVERAGE_LINKAGE = "0";

    private final static String SINGLE_LINKAGE = "-1";

    private static final String PEARSON = "1";

    public static Dataset cluster(final Dataset ds) {
        final IExpressionData expressionData = GPWrappers.createIExpressionData(ds);
        try {
            HCLAlgorithm alg = new HCLAlgorithm(expressionData);
            final IExpressionData ied = IExpressionDataUtil.sliceView(expressionData,
                    alg.getGenes_order(), alg.getSamples_order());
            return GPWrappers.createDataset(ied, ds.getAnnot());
        } catch (AlgorithmException e) {
            // Wrap in a GSEA exception in case we ever swap out the Clustering implementation.
            throw new StandardException("Unexpected issue while clustering", e, 9001);
        }
    }

    private int[] genes_order;

    private int[] samples_order;

    /**
     * Perform a Hierarchical Clustering run using the MeV library.  This will cluster by both Rows 
     * (using Single Linkage) and by Columns (using Average Linkage) and with the Pearson distance measure .  The order arrays are used by our Heatmap component but we only cluster to 
     * get order of Rows/Samples here, not to display a visual dendogram.
     * 
     * @param expressionData
     * @throws AlgorithmException
     */
    public HCLAlgorithm(IExpressionData expressionData) throws AlgorithmException {

        // TODO: Probably slow going point-by-point.  Work out of Matrix instead...
        // Should be able to grab a copy of the underlying array and pass that directly
        FloatMatrix inputMatrix = new FloatMatrix(expressionData.getRowCount(),
                expressionData.getColumnCount());
        for (int i = 0, rows = expressionData.getRowCount(); i < rows; i++) {
            for (int j = 0, cols = expressionData.getColumnCount(); j < cols; j++) {
                inputMatrix.set(i, j, (float) expressionData.getValue(i, j));
            }
        }
        
        // Cluster by Rows
        HCL hclRows = new HCL();
        AlgorithmData rowData = new AlgorithmData();
        rowData.addMatrix("experiment", inputMatrix);
        rowData.addParam("distance-function", PEARSON);
        rowData.addParam("distance-absolute", "false");
        rowData.addParam("calculate-genes", "true");
        rowData.addParam("method-linkage", SINGLE_LINKAGE);
        AlgorithmData genesResult = hclRows.execute(rowData);
        int[] genes_child_1_array = genesResult.getIntArray("child-1-array");
        int[] genes_child_2_array = genesResult.getIntArray("child-2-array");
        int[] genes_node_order = genesResult.getIntArray("node-order");
        genes_order = getLeafOrder(genes_node_order, genes_child_1_array,
                genes_child_2_array, null);

        // Cluster by Columns
        HCL hclCols = new HCL();
        AlgorithmData colData = new AlgorithmData();
        colData.addMatrix("experiment", inputMatrix);
        colData.addParam("distance-function", PEARSON);
        colData.addParam("distance-absolute", "false");
        colData.addParam("calculate-genes", "false");
        colData.addParam("method-linkage", AVERAGE_LINKAGE);
        AlgorithmData samplesResult = hclCols.execute(colData);

        int[] samples_child_1_array = samplesResult.getIntArray("child-1-array");
        int[] samples_child_2_array = samplesResult.getIntArray("child-2-array");
        int[] samples_node_order = samplesResult.getIntArray("node-order");
        samples_order = getLeafOrder(samples_node_order,
                samples_child_1_array, samples_child_2_array, null);
    }

    public int[] getGenes_order() {
        return genes_order;
    }

    public int[] getSamples_order() {
        return samples_order;
    }

    private int fillLeafOrder(int[] leafOrder, int[] child1, int[] child2,
                              int pos, int index, int[] indices) {
        if (child1[index] != -1) {
            pos = fillLeafOrder(leafOrder, child1, child2, pos, child1[index],
                    indices);
        }
        if (child2[index] != -1) {
            pos = fillLeafOrder(leafOrder, child1, child2, pos, child2[index],
                    indices);
        } else {
            leafOrder[pos] = indices == null ? index : indices[index];
            pos++;
        }
        return pos;
    }

    private int[] getLeafOrder(int[] nodeOrder, int[] child1, int[] child2,
                               int[] indices) {
        int[] leafOrder = new int[nodeOrder.length];
        Arrays.fill(leafOrder, -1);
        fillLeafOrder(leafOrder, child1, child2, 0, child1.length - 2, indices);
        return leafOrder;
    }
}