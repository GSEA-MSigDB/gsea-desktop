/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package org.genepattern.heatmap;

import javax.swing.*;
import java.awt.*;

/**
 * @author Joshua Gould
 */
public class PixelConverter {

    private JComponent component;

    int rowSize, columnSize;

    public PixelConverter(JComponent component) {
        this.component = component;
    }

    public void setColumnSize(int i) {
        columnSize = i;
    }

    public void setRowSize(int i) {
        rowSize = i;
    }

    public int getLeftIndex(int left) {
        if (left < component.getInsets().left) {
            return 0;
        }
        return (left - component.getInsets().left) / columnSize;
    }

    public int getTopIndex(int top) {
        if (top < component.getInsets().top) {
            return 0;
        }
        return (top - component.getInsets().top) / rowSize;
    }

    public int getRightIndex(int right, int limit) {
        if (right < 0) {
            return 0;
        }
        int result = right / columnSize + 1;
        return result > limit ? limit : result;
    }

    public int getBottomIndex(int bottom, int limit) {
        if (bottom < 0) {
            return 0;
        }
        int result = bottom / rowSize + 1;
        return result > limit ? limit : result;
    }

    public int columnAtPoint(Point p) {
        return (int) ((p.getX() - component.getInsets().left) / columnSize);
    }

    public int rowAtPoint(Point p) {
        return (int) ((p.getY() - component.getInsets().top) / rowSize);
    }

}
