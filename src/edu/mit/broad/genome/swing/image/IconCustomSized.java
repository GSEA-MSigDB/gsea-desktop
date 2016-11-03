/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.swing.image;

import javax.swing.*;
import java.net.URL;

/**
 * an image icon that can be custom sized
 */
public class IconCustomSized extends ImageIcon {

    private final int width;
    private final int height;

    /**
     * @param url
     * @param width
     * @param height
     */
    public IconCustomSized(URL url, int width, int height) {
        super(url);

        this.width = width;
        this.height = height;
    }

    public int getIconWidth() {
        return width;
    }

    public int getIconHeight() {
        return height;
    }

}


