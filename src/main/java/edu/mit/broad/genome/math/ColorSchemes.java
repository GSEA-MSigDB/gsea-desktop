/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.math;

import java.awt.*;

/**
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class ColorSchemes {

    public static class ColorScheme {

        private final String fName;
        private final ColorValue[] fColorValues;

        /**
         * Privatized Class Constructor.
         */
        public ColorScheme(final String type, final int[] colors, final String[] colorlabels) {

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
                if (((ColorScheme) obj).fName.equals(this.fName)) {
                    return true;
                }
            }

            return false;
        }

        public int hashCode() {
            return fName.hashCode();
        }

    } // BaseColorScheme


    public static class BroadCancer extends ColorScheme {
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

    public static class BroadCancerRed extends ColorScheme {
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

    public static class BroadCancerBlue extends ColorScheme {
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
}