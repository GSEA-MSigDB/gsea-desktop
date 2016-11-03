/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package org.genepattern.heatmap.image;

import org.genepattern.data.expr.ExpressionConstants;
import org.genepattern.data.expr.IExpressionData;
import org.genepattern.heatmap.RowColorScheme;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * This class is used to draw a heat map.
 *
 * @author Joshua Gould
 */
public class HeatMap {

    public static int LOG_SCALE = 0;

    public static int LINEAR_SCALE = 1;

    public static int COLOR_RESPONSE_ROW = 0;

    public static int COLOR_RESPONSE_GLOBAL = 1;

    String[] rowDescriptions;

    IExpressionData data;

    /**
     * width and height of one 'cell' in the heatmap
     */
    Dimension elementSize = new Dimension(10, 10);

    /**
     * width of 'cells', gene names, left inset, gene class label area
     */
    private int contentWidth = 0;

    /**
     * height of this heatmap
     */
    int height = 0;

    boolean antiAliasing = true;

    String fontFamilyName = "monospaced";

    int fontStyle = Font.PLAIN;

    private HeatMapHeader header;

    private int geneNameWidth;

    private int leftBorder = 0;

    private Font font;

    /**
     * The max width of the gene annotations
     */
    int maxGeneAnnotationsWidth = 0;

    /**
     * The number of pixels after the gene name and before the gene annotation
     */
    int spaceAfterGeneNames = 10;

    /** if not null, draw a filled square to the left of the row name */

    /**
     * width of filled square
     */
    private int annotationWidth = 6;

    private int numFeatureClasses;

    int numSampleClasses;

    private FeatureAnnotator featureAnnotator;

    SampleAnnotator sampleAnnotator;

    private int[] annotationWidths;

    DisplaySettings ds = new DisplaySettings();

    /**
     * Constructs an <code>HeatMap</code> with specified data
     *
     * @param data Description of the Parameter
     */
    public HeatMap(IExpressionData data, Color[] colorMap) {
        this.data = data;
        this.rowDescriptions = new String[data.getRowCount()];
        for (int i = 0, rows = data.getRowCount(); i < rows; i++) {
            rowDescriptions[i] = data.getRowMetadata(i,
                    ExpressionConstants.DESC);
            if (rowDescriptions[i] == null) {
                rowDescriptions[i] = "";
            }
        }

        this.header = new HeatMapHeader(this);
        ds.colorConverter = RowColorScheme.getRowInstance(colorMap);
    }

    public int getContentWidth() {
        return this.contentWidth;
    }
    
    public int getHeightWithHeader() {
        return height + header.height;
    }
    
    public static HeatMap createHeatMap(IExpressionData data,
                                        DisplaySettings ds, FeatureAnnotator fa, SampleAnnotator sa) {
        HeatMap heatMap = new HeatMap(data, RowColorScheme.getDefaultColorMap());
        heatMap.setDisplaySettings(ds);
        heatMap.setSampleAnnotator(sa);
        heatMap.setFeatureAnnotator(fa);

        // Set the initial size for the HeatMap based on the initial state.
        updateHeatMapSize(heatMap);

        return heatMap;
    }

    private static void updateHeatMapSize(HeatMap heatMap) {
        BufferedImage bi = new BufferedImage(100, 100, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g = bi.createGraphics();

        // DRE Note: the statefulness of this component makes this code somewhat difficult to untangle.
        // I believe that the Graphics2D object is used *only* for obtaining FontMetrics to measure
        // the size but have had poor results trying to re-use the G2D object inside the snapshot()
        // method.  For now we will continue to use a separate call.
        heatMap.updateSize(g);
        heatMap.header.updateSize(heatMap.contentWidth,
                heatMap.elementSize.width, g);

        g.dispose();
    }

    private void setDisplaySettings(DisplaySettings ds) {
        this.ds = ds;
        setElementSize(ds.rowSize, ds.columnSize);
        header.drawSampleNames = ds.drawColumnNames;
        ds.colorConverter.setDataset(data);
    }

    private void setSampleAnnotator(SampleAnnotator annotator) {
        this.sampleAnnotator = annotator;
        if (sampleAnnotator != null) {
            for (int j = 0; j < data.getColumnCount(); j++) {
                List colors = sampleAnnotator.getColors(data.getColumnName(j));
                if (colors != null) {
                    numSampleClasses = Math
                            .max(numSampleClasses, colors.size());
                }
            }
        }
    }

    private void setFeatureAnnotator(FeatureAnnotator annotator) {
        this.featureAnnotator = annotator;
        if (featureAnnotator != null) {
            for (int j = 0; j < data.getRowCount(); j++) {
                List colors = featureAnnotator.getColors(data.getRowName(j));
                if (colors != null) {
                    numFeatureClasses = Math.max(numFeatureClasses, colors
                            .size());
                }
            }
        }
    }

    void draw(Graphics2D g2) {
        final int samples = data.getColumnCount();
        int left = 0;
        int right = samples;
        int top = 0;
        int bottom = data.getRowCount();

        // draw rectangles
        for (int row = top; row < bottom; row++) {
            for (int column = left; column < right; column++) {
                int x = column * elementSize.width + leftBorder;
                int y = row * elementSize.height;
                g2.setColor(ds.colorConverter.getColor(row, column));
                g2.fillRect(x, y, elementSize.width, elementSize.height);
            }
        }
        int expWidth = samples * this.elementSize.width + 5;

        if (featureAnnotator != null) { // draw color bars
            for (int row = 0; row < data.getRowCount(); row++) {
                List colors = featureAnnotator.getColors(data.getRowName(row));
                if (colors != null) {
                    for (int j = 0; j < colors.size(); j++) {
                        g2.setColor((Color) colors.get(j));
                        g2.fillRect(
                                annotationWidth * j + expWidth + leftBorder,
                                row * elementSize.height, annotationWidth,
                                elementSize.height);
                    }
                }

            }

        }

        if (this.antiAliasing) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_OFF);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        }

        // draw gene ids
        FontMetrics fm = g2.getFontMetrics();
        int uniqX = elementSize.width * samples + 10;

        if (featureAnnotator != null) {
            uniqX += annotationWidth * numFeatureClasses;
        }

        if (this.ds.drawRowNames || this.ds.drawRowDescriptions) {
            if (right >= samples) {
                g2.setColor(Color.black);

                for (int row = top; row < bottom; row++) {
                    int annY = row * elementSize.height + fm.getAscent();
                    if (this.ds.drawRowNames) {
                        String label = data.getRowName(row);
                        g2.drawString(label, uniqX + leftBorder, annY);
                    }
                    if (ds.drawRowDescriptions) {
                        int geneAnnotationX = uniqX + leftBorder
                                + geneNameWidth;
                        if (ds.drawRowNames) {
                            geneAnnotationX += spaceAfterGeneNames;
                        }
                        String annot = rowDescriptions[row];
                        if (annot != null) {
                            g2.drawString(annot, geneAnnotationX, annY);
                        }

                    }
                }
            }
        }

        if (featureAnnotator != null) {
            int annotationStartX = uniqX + leftBorder;
            g2.setColor(Color.BLACK);
            if (ds.drawRowNames) {
                annotationStartX += geneNameWidth + spaceAfterGeneNames;
            }
            if (ds.drawRowDescriptions) {
                annotationStartX += maxGeneAnnotationsWidth
                        + spaceAfterGeneNames;
            }
            for (int i = 0, rows = data.getRowCount(); i < rows; i++) {
                String rowName = data.getRowName(i);
                int annY = i * elementSize.height + fm.getAscent();
                for (int j = 0, cols = featureAnnotator.getColumnCount(); j < cols; j++) {
                    String s = featureAnnotator.getAnnotation(rowName, j);
                    if (s != null) {
                        int x = annotationStartX + 10 * j;
                        if (j > 0) {
                            x += annotationWidths[j - 1];
                        }
                        g2.drawString(s, x, annY);
                    }
                }

            }
        }

        if (ds.drawGrid || ds.showFeatureGridLines || ds.showSampleGridLines) {
            g2.setColor(ds.gridLinesColor);
            int leftx = left * elementSize.width + leftBorder;

            // increase if drawing border to row name
            int rightx = right * elementSize.width + leftBorder; 
            if (!ds.drawGrid && ds.showFeatureGridLines) {
                leftx = rightx;
            }
            if (ds.showFeatureGridLines) {
                rightx = contentWidth;
            }
            if (ds.drawGrid || ds.showFeatureGridLines) {
                for (int row = top; row <= bottom; row++) {
                    // draw horizontal lines
                    int y = row * elementSize.height;
                    if (ds.upperTriangular) {
                        int leftDiag = row * elementSize.width + leftBorder;
                        g2.drawLine(leftDiag, y, rightx, y);
                    } else {
                        g2.drawLine(leftx, y, rightx, y);
                    }
                }
            }

            int topY = 0;
            int bottomy = bottom * elementSize.height;

            if (ds.showSampleGridLines && ds.drawGrid) {
                g2.translate(0, -header.height);
                bottomy = height + header.height;
            } else if (ds.showSampleGridLines && !ds.drawGrid) {
                g2.translate(0, -header.height);
                bottomy = header.height;
            }

            if (ds.drawGrid || ds.showSampleGridLines) {
                for (int column = left; column <= right; column++) {
                    int x = column * elementSize.width + leftBorder;
                    if (ds.upperTriangular) {
                        int bottomDiag = elementSize.height * column;
                        g2.drawLine(x, topY, x, bottomDiag);
                    } else {
                        g2.drawLine(x, topY, x, bottomy);
                    }
                }
            }

        }
    }

    public BufferedImage snapshot() {
        BufferedImage bi = new BufferedImage(contentWidth, height
                + header.height, BufferedImage.TYPE_3BYTE_BGR);

        Graphics2D g2 = bi.createGraphics();
        drawSnapshot(g2);
        g2.dispose();
        return bi;
    }
    
    public void drawSnapshot(Graphics2D graphics) {
        int headerHeight = header.height;
        graphics.setColor(Color.white);
        graphics.fillRect(0, 0, contentWidth, height + headerHeight);
        graphics.setColor(Color.black);
        graphics.setFont(header.font);
        header.draw(graphics);
        graphics.translate(0, headerHeight);
        graphics.setFont(this.font);
        this.draw(graphics);
    }

    /**
     * Updates the size of this heatmap
     *
     * @param g Description of the Parameter
     */
    void updateSize(Graphics2D g) {

        int fontHeight = Math.min(14, elementSize.height);
        font = new Font(fontFamilyName, fontStyle, fontHeight);
        g.setFont(font);
        // setFont(font);
        int width = elementSize.width * data.getColumnCount() + 1 + leftBorder;
        if (ds.drawRowNames) {
            this.geneNameWidth = getMaxGeneNamesWidth(g);
            width += 20 + this.geneNameWidth;
        }

        if (ds.drawRowDescriptions) {
            this.maxGeneAnnotationsWidth = getMaxGeneDescriptionsWidth(g);
            if (ds.drawRowNames) {
                width += spaceAfterGeneNames;
            }
            width += this.maxGeneAnnotationsWidth;
        }

        if (featureAnnotator != null) {
            width += annotationWidth * numFeatureClasses;
            annotationWidths = getAnnotationsWidth(g);
            for (int i = 0; i < annotationWidths.length; i++) {
                width += annotationWidths[i] + 10;
            }
        }

        this.contentWidth = width;
        this.height = elementSize.height * data.getRowCount() + 1;
        if (!ds.drawHeatMapElements) {
            height = 0;
        }
    }

    private int[] getAnnotationsWidth(Graphics2D g) {
        FontMetrics fm = g.getFontMetrics();
        int[] widths = new int[featureAnnotator.getColumnCount()];
        for (int i = 0, rows = data.getRowCount(); i < rows; i++) {
            String rowName = data.getRowName(i);
            for (int j = 0, cols = featureAnnotator.getColumnCount(); j < cols; j++) {
                String annot = featureAnnotator.getAnnotation(rowName, j);
                if (annot != null) {
                    widths[j] = Math.max(widths[j], fm.stringWidth(annot));
                }
            }
        }
        return widths;

    }

    public void setElementSize(int width, int height) {
        elementSize.width = width;
        elementSize.height = height;
    }

    int getMaxGeneNamesWidth(Graphics2D g) {
        return getMaxWidth(g, true);
    }

    int getMaxGeneDescriptionsWidth(Graphics2D g) {
        return getMaxWidth(g, false);
    }

    /**
     * Returns max width of annotation strings.
     *
     * @param g Description of the Parameter
     * @return The maxWidth value
     */
    int getMaxWidth(Graphics2D g, boolean geneNames) {
        if (g == null) {
            return 0;
        }
        if (antiAliasing) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_OFF);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        }
        FontMetrics fm = g.getFontMetrics();
        int max = 0;
        String str;
        for (int i = 0; i < data.getRowCount(); i++) {
            if (geneNames) {
                str = data.getRowName(i);
            } else {
                str = rowDescriptions[i];
            }
            if (str != null) {
                max = Math.max(max, fm.stringWidth(str));
            }
        }
        return max;
    }

}
