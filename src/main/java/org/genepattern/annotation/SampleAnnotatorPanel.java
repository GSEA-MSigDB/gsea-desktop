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
public class SampleAnnotatorPanel extends JPanel {

    private SparseClassVector classVector;

    private int heightPerClass = 6;

    private PixelConverter pixelConverter;

    private IExpressionData data;

    private int columnSize;

    private int numAnnotations = 0;

    private int spacer = 2;

    public SampleAnnotatorPanel(SparseClassVector _classVector,
                                IExpressionData _data, int columnSize, final JComponent c) {
        setBackground(Color.WHITE);
        this.classVector = _classVector;
        this.data = _data;
        this.columnSize = columnSize;

        classVector.addListener(new SparseClassVectorListener() {

            public void classChanged() {
                numAnnotations = 0;
                for (int i = 0; i < data.getColumnCount(); i++) {
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
        pixelConverter.setColumnSize(columnSize);

    }

    public void setColumnSize(int i) {
        columnSize = i;
    }

    public Dimension getPreferredSize() {
        return new Dimension(data.getColumnCount() * columnSize, numAnnotations
                * heightPerClass + numAnnotations * spacer);
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Rectangle bounds = g.getClipBounds();

        int left = pixelConverter.getLeftIndex(bounds.x);
        int right = pixelConverter.getRightIndex(bounds.x + bounds.width, data
                .getColumnCount());

        for (int col = left; col < right; col++) {
            List classNumbers = classVector.getClassNumbers(col);
            if (classNumbers != null) {
                int y = (numAnnotations - 1) * heightPerClass
                        + (numAnnotations - 1) * spacer;
                for (int j = 0; j < classNumbers.size(); j++) {
                    Integer classNumber = (Integer) classNumbers.get(j);
                    g.setColor(classVector.getColor(classNumber));
                    g.fillRect(col * columnSize, y, columnSize, heightPerClass);
                    y = y - heightPerClass - spacer;
                }
            }
        }

    }

}
