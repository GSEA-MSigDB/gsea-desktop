/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package org.genepattern.heatmap;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import javax.swing.*;
import java.awt.*;
import java.text.NumberFormat;

/**
 * @author KOhm
 * @author Joshua Gould
 */
public class LegendPanel extends javax.swing.JPanel {
    /**
     * formats the numbers to have one place past the decimal
     */
    public static final NumberFormat ONE_FRACTION_FORMAT = NumberFormat
            .getInstance();

    /**
     * formats the numbers to have no places past the decimal
     */
    public static final NumberFormat NO_FRACTION_FORMAT = NumberFormat
            .getInstance();

    /** static initializer */
    static {
        ONE_FRACTION_FORMAT.setMaximumFractionDigits(1);
        ONE_FRACTION_FORMAT.setMinimumFractionDigits(1);

        NO_FRACTION_FORMAT.setMaximumFractionDigits(0);
        NO_FRACTION_FORMAT.setMinimumFractionDigits(0);

    }

    /**
     * sets the grid to display the absolute color grid
     */
    final void setAbsoluteGrid(RowColorScheme converter) {
        final double[] values = converter.getSlots();
        setDisplayValues(converter.getColorMap(), values, "",
                NO_FRACTION_FORMAT, SwingConstants.LEFT);
    }

    /**
     * sets the grid to display the relative colors
     */
    final void setRelativeGrid(RowColorScheme converter) {
        final double[] values = new double[converter.getColorCount()];
        converter.calculateSlots(-3, 3, 0, values);
        final double new_values[] = new double[values.length + 1];
        new_values[0] = -3;
        System.arraycopy(values, 0, new_values, 1, values.length);
        setDisplayValues(converter.getColorMap(), new_values,
                "Normalized Expression", ONE_FRACTION_FORMAT,
                SwingConstants.LEFT);
    }

    public void setDisplayValues(Color[] colors, double[] values,
                                 String comment, NumberFormat format, int alignment) {
        String[] sValues = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            sValues[i] = format.format(values[i]);
        }
        setDisplayValues(colors, sValues, comment, alignment);
    }

    /**
     * sets up the display
     */
    public void setDisplayValues(Color[] colors, String[] values,
                                 String comment, int alignment) {
        this.removeAll();
        this.setBackground(Color.white);
        int num_colors = colors.length;

        StringBuffer colSpec = new StringBuffer();
        int width = 600;
        int widthPerLabel = width / num_colors;
        for (int j = 0; j < num_colors; j++) {
            if (j > 0) {
                colSpec.append(", ");
            }
            colSpec.append(widthPerLabel + "px");
        }
        colSpec.append(", pref");

        JPanel colorPanel = new JPanel(new FormLayout(colSpec.toString(),
                "pref, pref"));
        colorPanel.setBackground(Color.white);

        CellConstraints cc = new CellConstraints();

        for (int i = 0; i < num_colors; i++) {
            JPanel p = new JPanel();
            p.setToolTipText(values[i]);
            p.setBackground(colors[i]);
            p.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            JLabel lab = new JLabel(values[i]);
            lab.setToolTipText(values[i]);
            lab.setHorizontalAlignment(alignment);
            colorPanel.add(p, cc.xy(i + 1, 1));
            colorPanel.add(lab, cc.xy(i + 1, 2));
        }
        if (values.length > colors.length) {
            JLabel lab = new JLabel(values[values.length - 1]);
            lab.setToolTipText(values[values.length - 1]);
            colorPanel.add(lab, cc.xy(values.length, 2));
            lab.setHorizontalAlignment(alignment);
        }

        setLayout(new BorderLayout());
        JPanel temp = new JPanel();
        temp.setBackground(Color.white);
        temp.add(colorPanel);
        add(temp);

        JLabel commentLabel = new JLabel(comment, JLabel.CENTER);

        JPanel temp2 = new JPanel();
        temp2.setBackground(Color.white);
        temp2.add(commentLabel);
        add(temp2, BorderLayout.SOUTH);
    }
}
