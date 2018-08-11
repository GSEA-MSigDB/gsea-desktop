/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.swing.fields;

import edu.mit.broad.genome.swing.GuiHelper;
import edu.mit.broad.genome.swing.windows.GListWindow;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * <p>JTextField with a button that when clicked on popus ups a JList window where  i or more
 * selections can be made. The selections are then pasted as comma delimited text into
 * the JTextField</p>
 * <p> </p>
 * <p> </p>
 * <p> </p>
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class GOptionsFieldPlusChooser extends JPanel implements GFieldPlusChooser {

    protected final Logger log = Logger.getLogger(GOptionsFieldPlusChooser.class);
    protected JTextField tfEntry = new JTextField(40);
    protected JButton bEntry = new JButton(GuiHelper.ICON_ELLIPSIS);
    protected GListWindow fWindow;

    protected GOptionsFieldPlusChooser() {
    }

    // needed as otherwise a defaulkt one is added and then again one another one is added
    // if the setCustomActionListener is called
    public GOptionsFieldPlusChooser(final boolean addDefaultActionListener, final Action help_action_opt) {

        this.fWindow = new GListWindow(help_action_opt);
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
                Object[] sels = fWindow.show();
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
                continue;
            }

            buf.append(sels[i].toString().trim());

            if (i != sels.length - 1) {
                buf.append(',');
            }
        }

        tfEntry.setText(buf.toString());
    }

    /**
     * @param mode one of the ListSelectionModel constants
     */
    public void setListSelectionMode(int mode) {
        fWindow.setListSelectionMode(mode);
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

    public GListWindow getJListWindow() {
        return fWindow;
    }

    public void setValue(Object obj) {
        if (obj == null) {
            this.setText(null);
        } else {
            this.setText(obj.toString());
        }
    }

}    // End GOptionsFieldPlusChooser
