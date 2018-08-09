/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xtools.api.param;

import edu.mit.broad.genome.parsers.ParserFactory;
import edu.mit.broad.genome.swing.fields.GComboBoxField;
import edu.mit.broad.genome.swing.fields.GFieldUtils;
import edu.mit.broad.xbench.core.ObjectBindery;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.ActionListener;
import java.io.File;

/**
 * Bunch of static methods that help concrete Params do their thing.
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class ParamHelper {

    /**
     * Privatized Class constructor
     */
    private ParamHelper() {
    }

    public static void addDocumentListener(final JTextField tf, final Param param) {

        tf.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                param.setValue(tf.getText());
                if (param.isFileBased()) {
                    tf.setForeground(GFieldUtils.getFileFieldColor(tf.getText()));
                }
            }

            public void removeUpdate(DocumentEvent e) {
                param.setValue(tf.getText());
                if (param.isFileBased()) {
                    tf.setForeground(GFieldUtils.getFileFieldColor(tf.getText()));
                }
            }

            public void changedUpdate(DocumentEvent e) {
            }
        });

    }

    protected static GComboBoxField createActionListenerBoundHintsComboBox(boolean editable, ActionListener al, Param param) {
        DefaultComboBoxModel model = new DefaultComboBoxModel(param.getHints());

        if ((param.getValue() != null) && (model.getIndexOf(param.getValue()) == -1)) {
            model.addElement(param.getValue());
        }

        JComboBox cb = new JComboBox(model);
        cb.setEditable(editable);
        cb.setSelectedItem(param.getValue());
        cb.addActionListener(al);
        return new GComboBoxField(cb);
    }

    protected static GComboBoxField createActionListenerBoundPobComboBox(ActionListener al, Class[] pobClasses) {
        JComboBox cb = new JComboBox();
        cb.setEditable(false);
        ObjectBindery.bind(cb, pobClasses, false);
        cb.addActionListener(al);
        return new GComboBoxField(cb);
    }

    protected static void safeSelectFirst(JComboBox cb) {
        // imp -- so that somethign is selected at startup -> the act of selection
        // fires the event that does the param setting of value
        if (cb.getModel().getSize() > 0) {
            cb.setSelectedIndex(0);
        }
    }

    //??
    // imp -- so that something is selected at startup -> the act of selection
    // fires the event that does the param setting of value
    // value if not null and if found in the cbx
    // else 0 (if has at least 1 element)
    protected static void safeSelectPobValueDefaultOrFirst(JComboBox cb, Param param) {
        int indx = -1;

        /// first see if the value exists
        Object val = param.getValue();
        if (val != null) {
            indx = findByPobPathIndex(cb, val);
        }

        // next try the default
        if (indx == -1) {
            indx = findByPobPathIndex(cb, param.getDefault());
        }

        //log.debug("Index = " + indx + " value: " + value);

        if (indx == -1) { // give up and use the first
            if (cb.getModel().getSize() > 0) {
                cb.setSelectedIndex(0);
            }
        } else {
            cb.setSelectedIndex(indx);
        }

    }

    protected static void safeSelectValueDefaultOrNone(JComboBox cb, Param param) {

        Object t = param.getValue();

        if (t == null) {
            t = param.getDefault();
        }

        if (t != null) {
            cb.setSelectedItem(t);
        }

    }

    // need to use this as cant use new object as it isnt equal
    // for instance, see MetricParam
    // the signal2n is def, but its a different object than the one in hints
    protected static void safeSelectValueDefaultByString(JComboBox cb, Param param) {
        Object sel = getIfHasValue(param.getValue(), cb);

        if (sel == null) { // null value or value not found, so use default
            sel = param.getDefault();
        }

        if (sel == null) {
            return; // cannot do anything
        }

        String sels = sel.toString();
        for (int i = 0; i < cb.getModel().getSize(); i++) {
            if (cb.getModel().getElementAt(i).toString().equals(sels)) {
                cb.setSelectedIndex(i);
                return;
            }
        }

        safeSelectFirst(cb);
    }

    private static Object getIfHasValue(Object val, JComboBox cb) {

        if (val == null) {
            return null;
        }

        String vals = val.toString();
        for (int i = 0; i < cb.getModel().getSize(); i++) {
            if (cb.getModel().getElementAt(i).toString().equals(vals)) {
                return val;
            }
        }

        return null;

    }

    private static int findByPobPathIndex(JComboBox cb, Object path) {

        if (path == null) {
            return -1;
        }

        ComboBoxModel model = cb.getModel();

        // first compare by string
        for (int i = 0; i < model.getSize(); i++) {
            //log.debug("elem: " + i + " is: " + model.getElementAt(i));
            Object obj = model.getElementAt(i);
            File f = ParserFactory.getCache().getSourceFile(obj);
            if (f != null) {
                if (f.getPath().equals(path.toString())) {
                    return i;
                }
            }
        }

        // no luck

        return -1;
    }

} // End ParamHelper
