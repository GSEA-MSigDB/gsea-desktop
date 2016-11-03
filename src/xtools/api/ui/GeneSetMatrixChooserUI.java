/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xtools.api.ui;

import edu.mit.broad.genome.XLogger;
import edu.mit.broad.genome.swing.GuiHelper;
import edu.mit.broad.genome.swing.fields.GFieldPlusChooser;
import edu.mit.broad.genome.swing.fields.GOptionsFieldPlusChooser;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class GeneSetMatrixChooserUI extends JPanel implements GFieldPlusChooser {

    protected final Logger log = XLogger.getLogger(GOptionsFieldPlusChooser.class);
    protected JTextField tfEntry = new JTextField(40);
    protected JButton bEntry = new JButton(GuiHelper.ICON_ELLIPSIS);
    protected GeneSetMatrixChooserWindow fWindow; // @note make lazilly -- see below

    // needed as otherwise a default one is added and then again one another one is added
    // if the setCustomActionListener is called
    public GeneSetMatrixChooserUI(final boolean addDefaultActionListener) {
        if (addDefaultActionListener) {
            init();
        } else {
            jbInit();
        }
    }

    public void setCustomActionListener(final ActionListener customActionListener) {
        bEntry.addActionListener(customActionListener);
    }

    private void init() {

        jbInit();
        bEntry.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Object[] sels = _window().show();
                format(sels);
            }
        });
    }

    private void jbInit() {

        this.setLayout(new BorderLayout());
        tfEntry.setEditable(true);
        this.add(tfEntry, BorderLayout.CENTER);
        this.add(bEntry, BorderLayout.EAST);
    }

    private void format(Object[] sels) {

        if (sels == null) {
            tfEntry.setText("");
            return;
        }

        StringBuffer buf = new StringBuffer();

        for (int i = 0; i < sels.length; i++) {
            if (sels[i] == null) {
                //
            } else {

                buf.append(sels[i].toString().trim());

                if (i != sels.length - 1) {
                    buf.append(',');
                }
            }
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

    public GeneSetMatrixChooserWindow getJListWindow() {
        return _window();
    }

    // @note imp to make lazilly as this calls up application
    private GeneSetMatrixChooserWindow _window() {
        if (fWindow == null) {
            this.fWindow = new GeneSetMatrixChooserWindow();
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


}    // End GeneSetMatrixChooserUI

