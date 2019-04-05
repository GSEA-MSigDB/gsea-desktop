/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.objects;

import edu.mit.broad.genome.math.*;
import edu.mit.broad.genome.math.ColorSchemes.ColorScheme;

import org.apache.log4j.Logger;

import java.awt.*;

/**
 * A Dataset with logic to assign a Color to each element corresponding
 * to its float value.
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public interface ColorDataset extends Dataset {

    /**
     * default is relative & mean
     */
    public static final ScaleMode DEFAULT_SCALE_MODE = ScaleMode.REL_MEAN;

    /**
     * default is wrt row
     */
    public static final Orientation DEFAULT_ORIENTATION = Orientation.ROW;

    public static final GraphMode DEFAULT_GRAPH_MODE = GraphMode.LINEAR;

    public Color getColor(final int row, final int col);

    static class Helper {

        private static final Logger klog = Logger.getLogger(Helper.class);


        /**
         * the coloring alg.
         *
         * @todo get the alg from a's old n=code and add notes
         * x-> the ellemnts value and v the row it is on
         */
        public static Color computeRelativeMeanColor(double x, double min, double max, double mean,
                                                     GraphMode graphMode, ColorScheme colorScheme,
                                                     final boolean colorZeroAsWhite
        ) {

            if (colorZeroAsWhite && x == 0) {
                return Color.WHITE;
            }

            if (graphMode.isLinear()) {
                double cVal = (float) 0.5;
                Color color = null;
                int ind = -1;

                try {
                    if (x < min) {
                        x = min;
                    }

                    if (x >= max) {
                        x = max;
                    }

                    // The problem with this method is that if there is an extreme spike, then
                    // most of the values will be colorScheme[0] and the spike will be colorScheme[11]
                    cVal = (x - min) / (max - min);

                    // So modify cVal based on the mean
                    // Use mean as the threshold (halfway) value
                    // e.g. if x == mean, then cVal == .5
                    // e.g. scheme min -> mean into 0..5
                    //      and mean -> max into .5..1
                    //
                    if (mean != Float.NEGATIVE_INFINITY) {
                        if (x <= mean) {
                            cVal = (float) .5 * (x - min) / (mean - min);
                        } else {
                            cVal = (float) .5 + (float) .5 * (x - mean) / (max - mean);
                        }
                    }

                    ind = (int) (cVal * colorScheme.getNumColors());

                    // Sometimes cVal == 1 so correct for that here
                    if (ind == colorScheme.getNumColors()) {
                        ind = colorScheme.getNumColors() - 1;
                    }

                    color = colorScheme.getColor(ind);
                } catch (Exception e) {
                    klog.error("ColorMatrix: Cval: " + cVal + " Xval: " + x + " Index: " + ind
                            + " Min: " + min + " Max: " + max + " Mean: " + mean, e);
                }

                return color;
            } else if (graphMode.IsLog()) {
                // @todo check what log really should mean
                return colorScheme.getMinColor();
            } else {
                throw new IllegalStateException("Unknown GraphMode: " + graphMode);
            }
        }

        /**
         * The coloring alg.
         *
         * @todo get the alg from a's old n=code and add notes
         * x-> the elemnts value and v the row it is on
         */
        public static Color computeRelativeMedianColor(double x, double min, double max, double median,
                                                       GraphMode graphMode, ColorScheme colorScheme
        ) {

            if (graphMode.isLinear()) {
                double cVal = (float) 0.5;
                Color color = null;
                int ind = -1;

                try {
                    if (x < min) {
                        x = min;
                    }

                    if (x >= max) {
                        x = max;
                    }

                    // The problem with this method is that if there is an extreme spike, then
                    // most of the values will be colorScheme[0] and the spike will be colorScheme[11]
                    cVal = (x - min) / (max - min);

                    // So modify cVal based on the mean
                    // Use mean as the threshold (halfway) value
                    // e.g. if x == mean, then cVal == .5
                    // e.g. scheme min -> mean into 0..5
                    //      and mean -> max into .5..1
                    //
                    if (median != Float.NEGATIVE_INFINITY) {
                        if (x <= median) {
                            cVal = (float) .5 * (x - min) / (median - min);
                        } else {
                            cVal = (float) .5 + (float) .5 * (x - median) / (max - median);
                        }
                    }

                    ind = (int) (cVal * colorScheme.getNumColors());

                    // Sometimes cVal == 1 so correct for that here
                    if (ind == colorScheme.getNumColors()) {
                        ind = colorScheme.getNumColors() - 1;
                    }

                    color = colorScheme.getColor(ind);
                } catch (Exception e) {
                    klog.error("ColorMatrix: Cval: " + cVal + " Xval: " + x + " Index: " + ind
                            + " Min: " + min + " Max: " + max + " Median: " + median, e);
                }

                return color;
            } else if (graphMode.IsLog()) {
                // @todo check what log really should mean
                return colorScheme.getMinColor();
            } else {
                throw new IllegalStateException("Unknown graphmode: " + graphMode);
            }
        }

        /**
         * The coloring alg.
         *
         * @todo get the alg from a's old n=code and add notes
         * x-> the elemnts value and v the row it is on
         */
        public static Color computeAbsoluteColor(float x,
                                                 GraphMode graphMode, ColorScheme colorScheme
        ) {

            if (graphMode.isLinear()) {
                float min = 0;
                float max = 8000;
                float mean = Float.NEGATIVE_INFINITY;
                float cVal = (float) 0.5;
                Color color = null;
                int ind = -1;

                try {
                    if (x < min) {
                        x = min;
                    }

                    if (x >= max) {
                        x = max;
                    }

                    // The problem with this method is that if there is an extreme spike, then
                    // most of the values will be colorScheme[0] and the spike will be colorScheme[11]
                    cVal = (x - min) / (max - min);

                    // So modify cVal based on the mean
                    // Use mean as the threshold (halfway) value
                    // e.g. if x == mean, then cVal == .5
                    // e.g. scheme min mean into 0..5
                    //      and mean max into .5..1
                    //
                    if (mean != Float.NEGATIVE_INFINITY) {
                        if (x <= mean) {
                            cVal = (float) .5 * (x - min) / (mean - min);
                        } else {
                            cVal = (float) .5 + (float) .5 * (x - mean) / (max - mean);
                        }
                    }

                    ind = (int) (cVal * colorScheme.getNumColors());

                    // Sometimes cVal == 1 so correct for that here
                    if (ind == colorScheme.getNumColors()) {
                        ind = colorScheme.getNumColors() - 1;
                    }

                    color = colorScheme.getColor(ind);
                } catch (Exception e) {
                    klog.error("ColorMatrix: Cval: " + cVal + " Xval: " + x + " Index: " + ind
                            + " Min: " + min + " Max: " + max + " Mean: " + mean, e);
                }

                return color;
            } else if (graphMode.IsLog()) {

                // @todo check what log really should mean
                return colorScheme.getMinColor();
            } else {
                throw new IllegalStateException("Unknown GraphMode:" + graphMode);
            }
        }


    }

}    // End ColorDataset
