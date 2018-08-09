/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.math;

import java.awt.*;

/**
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class ColorSchemes {

    /**
     * Base class
     */
    static abstract class BasicColorScheme implements ColorScheme {

        private final String fName;
        private final ColorValue[] fColorValues;

        /**
         * Privatized Class Constructor.
         */
        public BasicColorScheme(final String type, final int[] colors, final String[] colorlabels) {

            this.fName = type;
            this.fColorValues = new ColorValue[colors.length];

            for (int i = 0; i < colors.length; i++) {
                fColorValues[i] = new ColorValue(new Color(colors[i]), colorlabels[i]);
            }
        }

        public Color getMinColor() {
            return fColorValues[0].color;
        }

        public int getNumColors() {
            return fColorValues.length;
        }

        public Color getColor(int pos) {
            return fColorValues[pos].color;
        }

        public String getValue(int pos) {
            return fColorValues[pos].value;
        }

        public String toString() {
            return fName;
        }

        public boolean equals(final Object obj) {

            if (obj instanceof ColorScheme) {
                if (((BasicColorScheme) obj).fName.equals(this.fName)) {
                    return true;
                }
            }

            return false;
        }

        public int hashCode() {
            return fName.hashCode();
        }

    } // BaseColorScheme


    public static class BroadCancer extends BasicColorScheme {
        /**
         * Default mit heat map
         * Note that there are 12 diff colors
         */
        private static final int[] MIT_COLORS_SCHEME = {
                0x4500ad, 0x2700d1, 0x6b58ef, 0x8888ff, 0xc7c1ff, 0xd5d5ff,
                0xffc0e5, 0xff8989, 0xff7080,
                0xff5a5a, 0xef4040, 0xd60c00
        };

        private static final String[] MIT_COLORS_LABELS = {
                "-3.0", "-2.5", "-2.0", "-1.5", "-1.0", "-0.5", "+0.5", "+1.0", "+1.5", "+2.0", "+2.5", "+3.0"
        };


        public BroadCancer() {
            super("MIT", MIT_COLORS_SCHEME, MIT_COLORS_LABELS);
        }
    }

    public static class BroadCancerRed extends BasicColorScheme {
        /**
         * Default mit heat map
         * Note that there are 12 diff colors
         */
        private static final int[] MIT_RED_COLORS_SCHEME = {
                0xffc0e5, 0xff8989, 0xff7080,
                0xff5a5a, 0xef4040, 0xd60c00
        };

        private static final String[] MIT_RED_COLORS_LABELS = {
                "+0.5", "+1.0", "+1.5", "+2.0", "+2.5", "+3.0"
        };


        public BroadCancerRed() {
            super("MIT_RED", MIT_RED_COLORS_SCHEME, MIT_RED_COLORS_LABELS);
        }
    }

    public static class BroadCancerBlue extends BasicColorScheme {
        /**
         * Default mit heat map
         * Note that there are 12 diff colors
         */
        private static final int[] MIT_BLUE_COLORS_SCHEME = {
                0x4500ad, 0x2700d1, 0x6b58ef, 0x8888ff, 0xc7c1ff, 0xd5d5ff
        };

        private static final String[] MIT_BLUE_COLORS_LABELS = {
                "-3.0", "-2.5", "-2.0", "-1.5", "-1.0", "-0.5"
        };


        public BroadCancerBlue() {
            super("MIT_BLUE", MIT_BLUE_COLORS_SCHEME, MIT_BLUE_COLORS_LABELS);
        }
    }


}    // End DefaultColorScheme

/**
 * for the four color scheme
 */
/*
public static final Color EQUAL_COLOR = Color.green;
public static final Color LOWER_COLOR = Color.blue;
public static final Color HIGHER_COLOR = Color.red;
public static final Color NAN_COLOR = Color.black;
public static final Color ERROR_COLOR = Color.white;
*/

/*

private static final ColorValue[] STANFORD2_COLORS_= {
new ColorValue(new Color(0, 255, 0), "-3.0"),
new ColorValue(new Color(0, 170, 0), "-2.5"),
new ColorValue(new Color(0, 85, 0), "-1.0"),

new ColorValue(Color.black, "0"),

new ColorValue(new Color(85, 0, 0), "+1.0"),
new ColorValue(new Color(170, 0, 0), "+2.0"),
new ColorValue(new Color(255, 0, 0), "+3.0"),
};

// fromdchip 0.6, 1.8, 3
private static final ColorValue[] STANFORD3_COLORS_ = {
new ColorValue(new Color(0, 255, 0), "-3.0"),
new ColorValue(new Color(0, 153, 0), "-1.8"),
new ColorValue(new Color(0, 51, 0), "-0.6"),

new ColorValue(Color.black, "0"),

new ColorValue(new Color(51, 0, 0), "+0.6"),
new ColorValue(new Color(153, 0, 0), "+1.8"),
new ColorValue(new Color(255, 0, 0), "+3.0"),
};

*/

/*
private static final ColorValue[] STANFORD4_COLORS = {
    new ColorValue(new Color(0, 255, 0), "-3.0"),
    new ColorValue(new Color(0, 153, 0), "-1.8"),
    new ColorValue(new Color(0, 51, 0), "-0.6"),
    new ColorValue(new Color(0, 20, 0), "-0.25"),
    new ColorValue(new Color(0, 10, 0), "-0.1"),

    new ColorValue(Color.black, "0"),

    new ColorValue(new Color(10, 0, 0), "+0.1"),
    new ColorValue(new Color(20, 0, 0), "+0.25"),
    new ColorValue(new Color(51, 0, 0), "+0.6"),
    new ColorValue(new Color(153, 0, 0), "+1.8"),
    new ColorValue(new Color(255, 0, 0), "+3.0"),
};
*/

/* // these are the values listed in the eisen paper
// but they are log scale - here we ae not log scale
private static final ColorValue[] STANFORD_COLORS = {
    new ColorValue(new Color(0, 255, 0), ">8"),
    new ColorValue(new Color(0, 200, 0), ">6"),
    new ColorValue(new Color(0, 150, 0), ">4"),
    new ColorValue(new Color(0, 100, 0), ">2"),
    new ColorValue(new Color(0, 50, 0), ">0.5"),

    new ColorValue(Color.black, "1:1"),

    new ColorValue(new Color(50, 0, 0), ">0.5"),
    new ColorValue(new Color(100, 0, 0), ">2"),
    new ColorValue(new Color(150, 0, 0), ">4"),
    new ColorValue(new Color(200, 0, 0), ">6"),
    new ColorValue(new Color(255, 0, 0), ">8"),
};
*/
