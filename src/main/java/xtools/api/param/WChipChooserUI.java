/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xtools.api.param;

import edu.mit.broad.genome.swing.GuiHelper;
import edu.mit.broad.genome.swing.fields.GFieldPlusChooser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class WChipChooserUI extends JPanel implements GFieldPlusChooser {

    private JTextField tfEntry = new JTextField(40);
    private JButton bEntry = new JButton(GuiHelper.ICON_ELLIPSIS);
    private WChipChooserWindow fWindow; // @note make lazily -- see below

    public WChipChooserUI() {
        this.setLayout(new BorderLayout());
        tfEntry.setEditable(true);
        this.add(tfEntry, BorderLayout.CENTER);
        this.add(bEntry, BorderLayout.EAST);
    }

    public void setCustomActionListener(final ActionListener customActionListener) {
        bEntry.addActionListener(customActionListener);
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

    public WChipChooserWindow getJListWindow() {
        return _window();
    }

    // @note imp to make lazilly as this calls up application
    private WChipChooserWindow _window() {
        if (fWindow == null) {
            this.fWindow = new WChipChooserWindow();
        }

        return fWindow;
    }

    public void setValue(final Object obj) {
        if (obj == null) {
            this.setText(null);
        } else {
            this.setText(obj.toString());
        }
    }
}