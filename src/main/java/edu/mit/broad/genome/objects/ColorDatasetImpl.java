/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.objects;

import edu.mit.broad.genome.math.*;
import edu.mit.broad.genome.math.ColorSchemes.ColorScheme;

import java.awt.*;

/**
 * A Dataset with logic to assign a Color to each element corresponding
 * to its float value.
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class ColorDatasetImpl extends DatasetProxy implements ColorDataset {

    //TODO: evaluate if these instance variables are really needed. 
    // These are set but never used; they are protected but there are no subclasses
    protected ScaleMode fScaleMode;

    protected Orientation fOrientType;

    protected ColorScheme fColorScheme;

    protected GraphMode fGraphMode;

    private Color[][] fColors;

    /**
     * Class Constructor.
     *
     * @param ds
     * @param smode
     * @param colorScheme
     * @param orient
     */
    public ColorDatasetImpl(final Dataset ds,
                            final ScaleMode smode,
                            final ColorScheme colorScheme,
                            final Orientation orient,
                            final GraphMode gmode) {

        super("colored." + ds.getName(), ds);

        this.fScaleMode = smode;
        this.fColorScheme = colorScheme;
        this.fOrientType = orient;
        this.fGraphMode = gmode;

        cacheColors();
    }

    /**
     * Class Constructor.
     *
     * @param ds
     * @param colorScheme
     */
    public ColorDatasetImpl(final Dataset ds, final ColorScheme colorScheme) {
        this(ds, DEFAULT_SCALE_MODE, colorScheme, DEFAULT_ORIENTATION, DEFAULT_GRAPH_MODE);
    }

    public ColorDatasetImpl(final Dataset ds, final ScaleMode scaleMode, final ColorScheme colorScheme) {
        this(ds, scaleMode, colorScheme, DEFAULT_ORIENTATION, DEFAULT_GRAPH_MODE);
    }

    protected void cacheColors() {

        if (fOrientType.isByRow()) {
            cacheColorsByRow();
        } else if (fOrientType.isByCol()) {
            cacheColorsByColumn();
        } else {
            throw new IllegalStateException("Unknown orientType option: " + fOrientType);
        }
    }

    protected void cacheColorsByRow() {

        // wierd for loop structure as trying to optimize calcs
        //log.debug("Beginning to cache fColors by ROW from: " + this.getClass());

        fColors = new Color[getNumRow()][getNumCol()];

        for (int r = 0; r < getNumRow(); r++) {
            Vector vr = getRow(r);
            Vector v = vr.toVectorNaNless(); // @note

            if (fScaleMode.isRelative()) {
                double center = v.meanOrMedian(fScaleMode.isMean());
                double max = v.max();
                double min = v.min();

                for (int c = 0; c < getNumCol(); c++) {
                    if (fScaleMode.isMean()) {

                        boolean coloraswhite = false;
                        if (fScaleMode == ScaleMode.REL_MEAN_ZERO_OMITTED) { // @todo improve
                            coloraswhite = true;
                        }

                        //fColors[r][c] = Helper.computeRelativeMeanColor(getElement(r, c), min, max, center, fGraphMode, fColorScheme);
                        fColors[r][c] = Helper.computeRelativeMeanColor(getElement(r, c), min, max, center, fGraphMode, fColorScheme, coloraswhite);
                    } else {
                        fColors[r][c] = Helper.computeRelativeMedianColor(getElement(r, c), min, max,
                                center, fGraphMode, fColorScheme);
                    }
                }
            } else {
                for (int c = 0; c < getNumCol(); c++) {
                    fColors[r][c] = Helper.computeAbsoluteColor(getElement(r, c), fGraphMode, fColorScheme);

                    //if (fScaleMode == SCALE_OPTIONS[2]) fColors[r][c] = computeAbsoluteColor(getElement(r, c), v);
                    //else if (fScaleMode == FOUR_COLOR) fColors[r][c] = computeFourColor(getElement(r, c), v);
                    //else throw new IllegalArgumentException("Unknwon ScaleMode" + fScaleMode);
                }
            }
        }

        //log.debug("Finished caching Colors: " + this.getClass());
    }

    protected void cacheColorsByColumn() {

        // wierd for loop structure as trying to optimize calcs
        log.info("Beginning to cache fColors by COL from: " + this.getClass());

        fColors = new Color[getNumRow()][getNumCol()];

        for (int c = 0; c < getNumCol(); c++) {
            Vector v = getColumn(c);

            if (fScaleMode.isRelative()) {
                double mean = v.mean();
                double median = v.median();
                double max = v.max();
                double min = v.min();

                for (int r = 0; r < getNumRow(); r++) {
                    if (fScaleMode.isMean()) {
                        fColors[r][c] = Helper.computeRelativeMeanColor(getElement(r, c), min, max, mean, fGraphMode, fColorScheme, false);
                    } else {
                        fColors[r][c] = Helper.computeRelativeMedianColor(getElement(r, c), min, max,
                                median, fGraphMode, fColorScheme);
                    }
                }
            } else {
                for (int r = 0; r < getNumRow(); r++) {
                    fColors[r][c] = Helper.computeAbsoluteColor(getElement(r, c), fGraphMode, fColorScheme);

                    //if (fScaleMode == SCALE_OPTIONS[2]) fColors[r][c] = computeAbsoluteColor(getElement(r, c), v);
                    //else if (fScaleMode == FOUR_COLOR) fColors[r][c] = computeFourColor(getElement(r, c), v);
                    //else throw new IllegalArgumentException("Unknwon ScaleMode" + fScaleMode);
                }
            }
        }

        log.info("Finished caching Colors: " + this.getClass());
    }

    public Color getColor(int row, int col) {
        return fColors[row][col];
    }


}    // End ColorDataset
