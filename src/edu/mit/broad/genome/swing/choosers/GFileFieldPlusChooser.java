/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.swing.choosers;

import edu.mit.broad.genome.swing.GuiHelper;
import edu.mit.broad.genome.swing.fields.GFieldPlusChooser;
import edu.mit.broad.genome.swing.fields.GFileField;
import edu.mit.broad.xbench.core.api.Application;
import edu.mit.broad.xbench.explorer.filemgr.XFileChooser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

/**
 * A GFieldPlusChooser that contains a JTextField and a JButton. Clicking on the
 * button brings up the file chooser widget and allows the user to select a File.
 * Once done, the File choosen can be accessed through getValue() or getFile()
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 * @note diff from GFileField -> this contains a button to launch a chooser
 * <p/>
 * Note: This class use the H&B works file chooser widget rather than JFileChooser.
 */
public class GFileFieldPlusChooser extends JPanel implements GFieldPlusChooser {

    /**
     * The text fields that holds file path
     */
    private GFileField tfValue;

    /**
     * Default withOUT border
     */
    public GFileFieldPlusChooser() {
        init();
    }

    /**
     * the initialization method
     */
    private void init() {

        JButton but = new JButton(GuiHelper.ICON_ELLIPSIS);

        but.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {

                XFileChooser fFileChooser = Application.getFileManager().getFileChooser();

                boolean proceed = fFileChooser.showOpenDialog();

                if (proceed) {
                    File selection = fFileChooser.getSelectedFile();
                    tfValue.setText(selection.getPath());
                }
            }
        });

        tfValue = new GFileField();

        this.setLayout(new BorderLayout());
        this.add(tfValue, BorderLayout.CENTER);
        this.add(but, BorderLayout.EAST);
    }


    /**
     * GFieldPlusChooser impl.
     * <p/>
     * Imp o not get the file from hb file chooser -- as user might
     * directly eneter value into text
     * // fields rather than through file chooser
     *
     * @return A File representing the users selection (or if the selection was nopt changed,
     *         the orginal File specified in argumant to class constructor).
     */
    public Object getValue() {

        String text = tfValue.getText();

        if (text == null) {
            return null;
        } else {
            return new File(text);
        }

    }

    /**
     * @param value
     */
    public void setValue(Object value) {

        if (value == null) {
            tfValue.setText("");
        } else {
            tfValue.setText(value.toString());
        }
    }

    /**
     * GFieldPlusChooser impl.
     *
     * @return This instance of GFileFieldPlusChooser
     */
    public JComponent getComponent() {
        return this;
    }

    // so that actionlisteners might be added
    public JTextField getTextField() {
        return tfValue;
    }

}    // End GFileFieldPlusChooser
