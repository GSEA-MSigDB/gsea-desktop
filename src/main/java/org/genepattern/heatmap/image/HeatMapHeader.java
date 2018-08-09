/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package org.genepattern.heatmap.image;

import java.awt.*;
import java.util.List;

/**
 * This class is used to render header of an experiment.
 *
 * @author Joshua Gould
 */
public class HeatMapHeader {

    HeatMap heatMap;

    boolean drawSampleNames = true;

    Font font;

    int height = 0;

    private int sampleNameHeight;

    private final int spacer = 10;

    private int leftInsets = 0;

    public HeatMapHeader(HeatMap heatMap) {
        this.heatMap = heatMap;
    }

    /**
     * Updates size of this header.
     *
     * @param contentWidth Description of the Parameter
     * @param elementWidth Description of the Parameter
     */
    public void updateSize(int contentWidth, int elementWidth, Graphics2D g) {
        setElementWidth(elementWidth);
        if (heatMap.antiAliasing) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_OFF);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        }
        FontMetrics hfm = g.getFontMetrics();
        int maxHeight = 0;
        sampleNameHeight = 0;
        if (drawSampleNames) {
            for (int j = 0; j < heatMap.data.getColumnCount(); j++) {
                String name = heatMap.data.getColumnName(j);
                sampleNameHeight = Math.max(sampleNameHeight, hfm
                        .stringWidth(name));
            }
        }
        maxHeight += sampleNameHeight;
        if (heatMap.sampleAnnotator != null) {
            maxHeight += getAnnotationsHeight();
        }
        if (drawSampleNames) {
            maxHeight += spacer;
        }

        this.height = maxHeight;
    }

    void draw(Graphics2D g2) {
        if (heatMap.antiAliasing) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_OFF);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        }
        drawHeader(g2);
    }

    /**
     * Draws the header into specified graphics.
     *
     * @param g2 Description of the Parameter
     */
    private void drawHeader(Graphics2D g2) {
        final int samples = heatMap.data.getColumnCount();

        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(Color.black);

        int sampleNamePix = -height;
        int colorBarHeight = getAnnotationsHeight();
        if (colorBarHeight > 0) {
            sampleNamePix += colorBarHeight;
        }

        SampleAnnotator annotator = heatMap.sampleAnnotator;
        if (annotator != null && annotator.hasPhenotypeColors()) {
            for (int j = 0; j < samples; j++) {
                Color c = annotator.getPhenotypeColor(heatMap.data
                        .getColumnName(j));
                if (c != null) {
                    g2.setColor(c);
                    int bottom = height - colorBarHeight;
                    int top = bottom - sampleNameHeight - fm.getMaxAdvance();
                    g2.fillRect(j * heatMap.elementSize.width + leftInsets,
                            top, heatMap.elementSize.width, sampleNameHeight
                            + fm.getMaxAdvance());
                }
            }
        }

        if (drawSampleNames) {
            g2.setColor(Color.BLACK);
            g2.rotate(-Math.PI / 2);
            for (int sample = 0; sample < samples; sample++) {
                String name = heatMap.data.getColumnName(sample);
                g2.drawString(name, sampleNamePix, fm.getAscent()
                        + heatMap.elementSize.width * sample + leftInsets);

            }
            g2.rotate(Math.PI / 2);
        }

        if (heatMap.numSampleClasses > 0) {
            for (int j = 0; j < samples; j++) {
                List colors = annotator
                        .getColors(heatMap.data.getColumnName(j));
                if (colors != null) {
                    for (int i = 0; i < colors.size(); i++) {
                        Color c = (Color) colors.get(i);
                        if (c != null) {
                            g2.setColor(c);
                            int y = height
                                    - ((i + 1) * heatMap.ds.sampleAnnonationsHeight)
                                    - (i * heatMap.ds.sampleAnnotationSpacing)
                                    - spacer;
                            g2.fillRect(j * heatMap.elementSize.width
                                    + leftInsets, y, heatMap.elementSize.width,
                                    heatMap.ds.sampleAnnonationsHeight);
                        }

                    }
                }
            }
            Font oldFont = g2.getFont();
            g2.setColor(Color.BLACK);
            g2.setFont(new Font(heatMap.fontFamilyName, heatMap.fontStyle,
                    heatMap.ds.sampleAnnonationsHeight));

            for (int i = 0; i < heatMap.numSampleClasses; i++) {
                String label = annotator.getLabel(i);
                if (label != null) {
                    int y = height
                            - ((i + 1) * heatMap.ds.sampleAnnonationsHeight)
                            - (i * heatMap.ds.sampleAnnotationSpacing) - spacer;

                    g2.drawString(label, samples * heatMap.elementSize.width
                            + leftInsets + 10, y + fm.getAscent());
                }
            }
            g2.setFont(oldFont);

        }
    }

    /**
     * Sets an element width.
     *
     * @param width The new heatMap.elementSize.width value
     */
    private void setElementWidth(int width) {
        width = Math.min(width, 14);
        font = new Font(heatMap.fontFamilyName, heatMap.fontStyle, width);
    }

    /**
     * Returns height of color bar for experiments
     *
     * @return The colorBarHeight value
     */
    private int getAnnotationsHeight() {
        return heatMap.numSampleClasses > 0 ? heatMap.ds.sampleAnnonationsHeight
                * heatMap.numSampleClasses
                + (heatMap.numSampleClasses * heatMap.ds.sampleAnnotationSpacing)
                + spacer
                : spacer;
    }

}
