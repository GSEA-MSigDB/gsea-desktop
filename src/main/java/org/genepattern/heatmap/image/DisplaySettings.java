/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package org.genepattern.heatmap.image;

import org.genepattern.heatmap.ColorScheme;
import org.genepattern.heatmap.RowColorScheme;

import java.awt.*;

/**
 * Settings for creating a heatmap image
 *
 * @author Joshua Gould
 */
public class DisplaySettings {
    /**
     * The size in pixels of an element along the x axis
     */
    public int columnSize = 6;

    /**
     * gets color for a value in a dataset
     */
    public ColorScheme colorConverter = new RowColorScheme(
            RowColorScheme.COLOR_RESPONSE_LINEAR);

    /**
     * The size in pixels of an element along the y axis
     */
    public int rowSize = 6;

    /**
     * Whether to draw a grid between elements
     */
    public boolean drawGrid = true;

    /**
     * The grid color when <tt>drawGrid</tt> is <tt>true</tt>
     */
    public Color gridLinesColor = Color.BLACK;

    /**
     * Whether to draw row names
     */
    public boolean drawRowNames = true;

    /**
     * Whether to draw column names
     */
    public boolean drawColumnNames = true;

    /**
     * Whether to draw row descriptions in the expression dataset
     */
    public boolean drawRowDescriptions = false;

    /**
     * height of sample annotations
     */
    public int sampleAnnonationsHeight = 6;

    /**
     * number of pixels between sample annotation rows
     */
    public int sampleAnnotationSpacing = 0;

    /**
     * whether to show grid lines around the annotations
     */
    public boolean showSampleGridLines = false;

    /**
     * whether to show grid lines around the feature annotations
     */
    public boolean showFeatureGridLines = false;

    /**
     * Whether the matrix is upper triangular (square matrices only)
     */
    public boolean upperTriangular = false;

    /**
     * Whether to draw the heat map elements
     */
    public boolean drawHeatMapElements = true;

    public DisplaySettings() {

    }
}
