/*
 * Copyright (c) 2003-2020 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
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
 */
// TODO: add type parameters for the various Swing JComboBox (and related) declarations.  Issue here is
// that they are so general due to the Parameter class hierarchy.
public class ParamHelper {

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

    static GComboBoxField createActionListenerBoundHintsComboBox(boolean editable, ActionListener al, Param param) {
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

    // Special case of the above for RandomeSeedTypeParam, since we represent these with String values instead of the actual
    // parameter value (from getHints()).  None of the other params seem to behave this way, but we won't go any further
    // than this for now as there may be a better way to do this in the long run.
    static GComboBoxField createActionListenerBoundHintsComboBox(boolean editable, ActionListener al, RandomSeedTypeParam param) {
        DefaultComboBoxModel model = new DefaultComboBoxModel(param.getHints());

        // Here's the only difference from the above
        if ((param.getValue() != null) && (model.getIndexOf(param.getValue().toString()) == -1)) {
            model.addElement(param.getValue());
        }

        JComboBox cb = new JComboBox(model);
        cb.setEditable(editable);
        cb.setSelectedItem(param.getValue());
        cb.addActionListener(al);
        return new GComboBoxField(cb);
    }

    static GComboBoxField createActionListenerBoundPobComboBox(ActionListener al, Class[] pobClasses) {
        JComboBox cb = new JComboBox();
        cb.setEditable(false);
        ObjectBindery.bind(cb, pobClasses);
        cb.addActionListener(al);
        return new GComboBoxField(cb);
    }

    static void safeSelectFirst(JComboBox cb) {
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
    static void safeSelectPobValueDefaultOrFirst(JComboBox cb, Param param) {
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

        if (indx == -1) { // give up and use the first
            if (cb.getModel().getSize() > 0) {
                cb.setSelectedIndex(0);
            }
        } else {
            cb.setSelectedIndex(indx);
        }

    }

    static void safeSelectValueDefaultOrNone(JComboBox cb, Param param) {

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
    static void safeSelectValueDefaultByString(JComboBox cb, Param param) {
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
}