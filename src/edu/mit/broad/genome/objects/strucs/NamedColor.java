/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.objects.strucs;

import java.awt.*;

/**
 * A color and its common English name
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class NamedColor {

    public final String name;
    public final Color color;

    /**
     * Class constructor
     *
     * @param name
     * @param color
     */
    public NamedColor(String name, Color color) {
        this.name = name;
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public Color getColor() {
        return color;
    }

    public int hashCode() {
        return toString().hashCode();
    }

    public String toString() {
        return name + color;
    }

    public boolean equals(Object obj) {
        if (obj instanceof NamedColor) {
            NamedColor nc = (NamedColor) obj;
            if (nc.name.equals(name) && nc.color.equals(color)) {
                return true;
            }
        }

        return false;
    }

} // End class NamedColor
