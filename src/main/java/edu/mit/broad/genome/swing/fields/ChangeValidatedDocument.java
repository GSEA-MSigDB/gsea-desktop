/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.swing.fields;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import java.awt.*;
import java.text.Format;
import java.text.ParsePosition;

/**
 * Only allows valid text to be entered or only allows removal of characters
 * if the result is still valid as determined by the Format. This only allows
 * number to be typed in to the document if the Format is one of the NumberFormat
 * instances.  Other Format subclasses could be used but the data entered would
 * have to be valid while only partially typed.  Other Format subclasses would
 * prabably be better suited to an action-validated document.
 * <p/>
 * Adapted from the sun tutorial:
 * http://java.sun.com/docs/books/tutorial/uiswing/components/textfield.html#validation
 *
 * @author kohm
 */
public class ChangeValidatedDocument extends PlainDocument {

    /**
     * the current Format subclass
     */
    private final Format format;

    /**
     * keeps track of the position that the parser stopped at
     */
    private final ParsePosition status = new ParsePosition(0);

    /**
     * constructor
     */
    public ChangeValidatedDocument(Format f) {

        if (f == null) {
            throw new NullPointerException("Format cannot be null");
        }

        format = f;
    }

    /**
     * overridden to only allow valid characters
     */
    public void insertString(final int offs, final String str, final AttributeSet a)
            throws BadLocationException {

        if (str == null) {
            return;
        }

        final String currentText = getText(0, getLength());
        final String beforeOffset = currentText.substring(0, offs);
        final String afterOffset = currentText.substring(offs, currentText.length());
        final String proposedResult = beforeOffset + str + afterOffset;

        //System.out.println("proposedResult '" + proposedResult + "'");

        if (isResultOK(proposedResult)) {
            super.insertString(offs, str, a);
        }
    }

    /**
     * overridden to only allow valid deleting of characters
     */
    public void remove(final int offs, final int len) throws BadLocationException {

        final String currentText = getText(0, getLength());
        final String beforeOffset = currentText.substring(0, offs);
        final String afterOffset = currentText.substring(len + offs, currentText.length());
        final String proposedResult = beforeOffset + afterOffset;

        if ((proposedResult.length() == 0) || isResultOK(proposedResult)) {
            super.remove(offs, len);
        }
    }

    /**
     * helper method determines if the current text parses ok
     */
    protected boolean isResultOK(final String proposedResult) {

        //reset status
        status.setErrorIndex(-1);
        status.setIndex(0);

        final Object result = format.parseObject(proposedResult, status);
        final boolean ok = ((result != null) && (status.getIndex() == proposedResult.length())
                && (status.getErrorIndex() < 0));

        if (!ok) {
            Toolkit.getDefaultToolkit().beep();
            //System.err.println("insertString: could not parse: " + proposedResult);
        }

        return ok;
    }

}
