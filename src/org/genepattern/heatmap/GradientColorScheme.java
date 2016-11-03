/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package org.genepattern.heatmap;

import org.genepattern.data.expr.IExpressionData;

import javax.swing.*;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * @author Joshua Gould
 */
public class GradientColorScheme implements ColorScheme {
    double min, max, mid;

    private IExpressionData data;

    private BufferedImage posImage, negImage;

    boolean useDoubleGradient = true;

    public GradientColorScheme(double min, double max, double mid,
                               Color posColor, Color negColor, Color neutralColor) {
        this.min = min;
        this.max = max;
        this.mid = mid;
        posImage = createGradientImage(neutralColor, posColor);
        negImage = createGradientImage(negColor, neutralColor);
    }

    /**
     * Creates a gradient image given specified <CODE>Color</CODE>(s)
     *
     * @param color1 <CODE>Color</CODE> to display at left side of gradient
     * @param color2 <CODE>Color</CODE> to display at right side of gradient
     * @return returns a gradient image
     */
    private BufferedImage createGradientImage(Color color1, Color color2) {
        BufferedImage image = new BufferedImage(256, 1, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D graphics = image.createGraphics();
        GradientPaint gp = new GradientPaint(0, 0, color1, 255, 0, color2);
        graphics.setPaint(gp);
        graphics.drawRect(0, 0, 255, 1);
        graphics.dispose();
        return image;
    }

    public void setUseDoubleGradient(boolean useDoubleGradient) {
        this.useDoubleGradient = useDoubleGradient;
    }

    public Color getColor(int row, int column) {

        int rgb;
        double value = data.getValue(row, column);

        if (useDoubleGradient) {
            double maximum = value < mid ? this.min : this.max;
            int colorIndex = (int) (255 * (value - mid) / (maximum - mid));
            colorIndex = colorIndex > 255 ? 255 : colorIndex;
            rgb = value < mid ? negImage.getRGB(255 - colorIndex, 0) : posImage
                    .getRGB(colorIndex, 0);
        } else {
            double span = this.max - this.min;
            int colorIndex = 0;
            if (value <= min) {
                colorIndex = 0;
            } else if (value >= max) {
                colorIndex = 255;
            } else {
                colorIndex = (int) (((value - this.min) / span) * 255);
            }
            rgb = posImage.getRGB(colorIndex, 0);
        }
        return new Color(rgb);
    }

    public void setDataset(IExpressionData d) {
        this.data = d;
    }

    public Component getLegend() {
        return new Legend(min, mid, max, negImage, posImage, useDoubleGradient);
    }

    private static class Legend extends JPanel {

        private Insets insets;

        private int height = 15;

        BufferedImage negColorImage;

        BufferedImage posColorImage;

        boolean isAntiAliasing;

        private boolean useDoubleGradient = true;

        private double minValue;

        private double midValue;

        private double maxValue;

        /**
         * Constructs a <code>MultipleArrayHeader</code> with specified insets
         * and trace space.
         */
        public Legend(double min, double mid, double max,
                      BufferedImage negativeImage, BufferedImage positiveImage,
                      boolean useDoubleGradient) {
            this.minValue = min;
            this.midValue = mid;
            this.maxValue = max;
            this.negColorImage = negativeImage;
            this.posColorImage = positiveImage;
            setBackground(Color.white);
            insets = new Insets(0, 0, 0, 0);
            setPreferredSize(new Dimension(120, 40));
            this.useDoubleGradient = useDoubleGradient;
        }

        /**
         * Paints the header into specified graphics.
         */
        public void paint(Graphics g1D) {
            super.paint(g1D);

            Graphics2D g = (Graphics2D) g1D;
            int width = getWidth();
            if (useDoubleGradient) {
                g.drawImage(this.negColorImage, insets.left, 0,
                        (int) (width / 2f), height, null);
                g.drawImage(this.posColorImage, (int) (width / 2f)
                        + insets.left, 0, (int) (width / 2.0), height, null);
            } else {
                g.drawImage(this.posColorImage, insets.left, 0, width, height,
                        null);
            }

            FontMetrics hfm = g.getFontMetrics();
            int fHeight = hfm.getHeight();

            g.setColor(Color.black);
            if (isAntiAliasing) {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_OFF);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            }
            int textWidth;
            g.drawString(String.valueOf(this.minValue), insets.left, height
                    + fHeight);
            textWidth = hfm.stringWidth(String.valueOf(midValue));
            if (useDoubleGradient)
                g.drawString(String.valueOf(midValue), (int) (width / 2f)
                        - textWidth / 2 + insets.left, height + fHeight);
            textWidth = hfm.stringWidth(String.valueOf(this.maxValue));
            g.drawString(String.valueOf(this.maxValue), (width - textWidth)
                    + insets.left, height + fHeight);

        }
    }
}
