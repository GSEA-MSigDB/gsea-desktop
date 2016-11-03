/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.utils;

import java.awt.Color;

/**
 * This class provides Color-related utility methods.
 * @author David Eby
 */
public class ColorUtils {
    public static Color pickRandomColor() {
        Double redComponent = Math.random();
        Double greenComponent = Math.random();
        Double blueComponent = Math.random();
        return new Color(redComponent.floatValue(), greenComponent.floatValue(),
                blueComponent.floatValue());
    }

    public static Color[] pickRandomColors(int ncolors, Color[] firstUseThese) {
        // Protect against 0 or a null preference array...
        if (ncolors <= 0) return new Color[] {};
        if (firstUseThese == null) {
            firstUseThese = new Color[] {};
        }

        Color[] colors = new Color[ncolors];
        for (int i = 0; i < ncolors; i++) {
            if (i < firstUseThese.length) {
                colors[i] = firstUseThese[i];
            } else {
                colors[i] = pickRandomColor();
            }
        }
        return colors;
    }
}    // End ColorUtils
