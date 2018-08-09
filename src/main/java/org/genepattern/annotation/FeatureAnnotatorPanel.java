/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package org.genepattern.annotation;

import org.genepattern.data.expr.IExpressionData;
import org.genepattern.heatmap.PixelConverter;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * @author Joshua Gould
 */
public class FeatureAnnotatorPanel extends JPanel {

    private SparseClassVector classVector;

    private int widthPerClass = 6;

    private PixelConverter pixelConverter;

    private IExpressionData data;

    private int rowSize;

    private int numAnnotations = 0;

    public FeatureAnnotatorPanel(SparseClassVector _classVector,
                                 IExpressionData _data, int rowSize, final JComponent c) {
        setBackground(Color.WHITE);
        this.classVector = _classVector;
        this.data = _data;
        this.rowSize = rowSize;

        classVector.addListener(new SparseClassVectorListener() {

            public void classChanged() {
                numAnnotations = 0;

                for (int i = 0; i < data.getRowCount(); i++) {
                    List classNumbers = classVector.getClassNumbers(i);

                    if (classNumbers != null) {
                        numAnnotations = Math.max(classNumbers.size(),
                                numAnnotations);
                    }
                }

                c.invalidate();
                c.validate();
                c.doLayout();

                Component a = c.getTopLevelAncestor();
                a.invalidate();
                a.validate();
                a.doLayout();
                repaint();
            }

        });
        pixelConverter = new PixelConverter(this);
        pixelConverter.setRowSize(rowSize);

    }

    public void setRowSize(int i) {
        rowSize = i;
    }

    public Dimension getPreferredSize() {
        return new Dimension(numAnnotations * widthPerClass
                + (numAnnotations + 1) * 2, data.getRowCount() * rowSize);
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Rectangle bounds = g.getClipBounds();

        int top = pixelConverter.getTopIndex(bounds.y);
        int bottom = pixelConverter.getBottomIndex(bounds.y + bounds.height,
                data.getRowCount());

        for (int i = top; i < bottom; i++) {
            List classNumbers = classVector.getClassNumbers(i);
            if (classNumbers != null) {
                int xStart = 2;
                for (int j = 0; j < classNumbers.size(); j++) {
                    Integer classNumber = (Integer) classNumbers.get(j);
                    g.setColor(classVector.getColor(classNumber));
                    g.fillRect(xStart, i * rowSize, widthPerClass, rowSize);

                    xStart += widthPerClass + 2;
                }
            }
        }

    }

}
