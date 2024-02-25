/*
 * Copyright (c) 2003-2024 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.swing.fields;

import javax.swing.*;
import java.text.NumberFormat;

/**
 * Allows the user to only enter a whole number.  This doesn't not need to be used
 * in Java 1.4 or latter.
 *
 * @author kohm
 * @see FormatTextTest
 * @see javax.swing.JFormattedTextField
 */
public class GIntegerField extends JTextField implements GFieldPlusChooser {
    private final NumberFormat integerFormatter;

    public GIntegerField(final int value, final int columns) {
        this(value, columns, NumberFormat.getNumberInstance());
        integerFormatter.setGroupingUsed(false);
    }

    public GIntegerField(final int value, final int columns, final NumberFormat format) {
        super(new ChangeValidatedDocument(format), null, columns);

        integerFormatter = format;

        format.setParseIntegerOnly(true);
        setInt(value);
    }

    public void setInt(int value) {
        super.setText(integerFormatter.format(value));
    }

    /**
     * overriden now will throw number format exeption if text s not a number
     */
    public void setText(String text) {
        if ((text != null) && (text.length() > 0)) {
            final int value = Integer.parseInt(text);

            super.setText(integerFormatter.format(value));
        } else {
            super.setText("0");
        }
    }

    /**
     * @return The user specified Integer
     */
    public Object getValue() {
        try {
            if (this.getText() == null) {
                return null;
            } else {
                return Integer.valueOf(this.getText());
            }
        } catch (Throwable t) {
            return null;
        }
    }

    public void setValue(Object obj) {
        Integer i;

        if (obj == null) {
            i = Integer.MIN_VALUE; // better than exceptioning out
        } else if (obj instanceof Float) {
            i = (Integer) obj;
        } else {
            i = Integer.valueOf(obj.toString());
        }

        this.setText(i.toString());
    }

    public JComponent getComponent() {
        return this;
    }
}
