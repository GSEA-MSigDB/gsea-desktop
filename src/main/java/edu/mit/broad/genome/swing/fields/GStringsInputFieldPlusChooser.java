/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.swing.fields;

import edu.mit.broad.genome.swing.GuiHelper;
import edu.mit.broad.genome.swing.windows.GTextAreaWindow;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.StringTokenizer;

/**
 * <p>JTextField with a button that when clicked on popus ups a JList window where  i or more
 * selections can be made. The selections are then pasted as comma delimited text into
 * the JTextField</p>
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 * @note diff from a chooser in that the input field is a Text area and not a list (ie chooser)
 */
public class GStringsInputFieldPlusChooser extends JPanel implements GFieldPlusChooser {

    private final JTextField tfEntry = new JTextField(40);
    private final JButton bEntry = new JButton(GuiHelper.ICON_ELLIPSIS);
    private final GTextAreaWindow fWindow;

    private static final String PARSE_DELIMS = ",\t\n";// dont parse on spaces

    /**
     * Class Constructor.
     *
     * @param options
     */
    public GStringsInputFieldPlusChooser(String text) {

        this.fWindow = new GTextAreaWindow(text);

        init();
    }

    public GStringsInputFieldPlusChooser() {

        this.fWindow = new GTextAreaWindow();

        init();
    }

    private void init() {

        jbInit();
        bEntry.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {

                String text = fWindow.show();

                if (text != null) {
                    format(text);
                }
            }
        });
    }

    private void jbInit() {

        this.setLayout(new BorderLayout());
        tfEntry.setEditable(true);
        this.add(tfEntry, BorderLayout.CENTER);
        this.add(bEntry, BorderLayout.EAST);
    }

    private void format(String text) {
        StringBuffer buf = new StringBuffer();

        StringTokenizer tok = new StringTokenizer(text, PARSE_DELIMS);

        while (tok.hasMoreTokens()) {
            buf.append(tok.nextToken()).append(',');
        }

        tfEntry.setText(buf.toString());
    }

    public String getText() {
        return tfEntry.getText();
    }

    public void setText(String text) {
        tfEntry.setText(text);
    }

    /**
     * so that the tf can hbave its events listened to
     *
     * @return
     */
    public JTextField getTextField() {
        return tfEntry;
    }

    public Object getValue() {
        return getText();
    }

    public JComponent getComponent() {
        return this;
    }

    public void setValue(Object obj) {
        if (obj == null) {
            this.setText(null);
        } else {
            this.setText(obj.toString());
        }
    }

}    // End GStringsInputFieldPlusChooser
