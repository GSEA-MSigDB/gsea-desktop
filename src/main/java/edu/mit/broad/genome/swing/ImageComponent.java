/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.swing;

import javax.swing.*;
import java.awt.*;

/**
 * @author Aravind Subramanian
 */// / ImageComponent is used for displaying the image
public class ImageComponent extends JComponent {

    private Image image = null;
    private boolean scaled = false;
    private Dimension size = null;
    private Insets insets = new Insets(0, 0, 0, 0);

    /**
     * Class constructor
     *
     * @param image
     * @param scaled
     */
    public ImageComponent(Image image, boolean scaled) {
        this.image = image;
        this.scaled = scaled;
    }

    public void paint(Graphics g) {
        super.paint(g);
        insets = getInsets(insets);
        size = getSize(size);
        if (image == null) {
            return;
        }
        if (scaled) {
            g.drawImage(image,
                    insets.left, insets.top,
                    size.width - insets.left - insets.right,
                    size.height - insets.top - insets.bottom,
                    this);
        } else {
            g.drawImage(image, insets.left, insets.top, this);
        }
    }

    public Dimension getMinimumSize() {
        int imgw = 32, imgh = 32;
        if (image != null) {
            imgw = image.getWidth(this);
            imgh = image.getHeight(this);
        }
        insets = getInsets(insets);
        return new Dimension(
                insets.left + Math.max(32, imgw / 10) + insets.right,
                insets.top + Math.max(32, imgh / 10) + insets.bottom
        );
    }

    public Dimension getPreferredSize() {
        int imgw = 32, imgh = 32;
        if (image != null) {
            imgw = image.getWidth(this);
            imgh = image.getHeight(this);
        }
        insets = getInsets(insets);
        return new Dimension(
                insets.left + imgw + insets.right,
                insets.top + imgh + insets.bottom
        );
    }

    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

} // End class ImageComponent
