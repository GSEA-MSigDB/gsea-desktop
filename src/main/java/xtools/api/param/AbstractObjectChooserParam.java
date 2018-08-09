/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xtools.api.param;

import edu.mit.broad.genome.swing.fields.GFieldPlusChooser;
import edu.mit.broad.genome.swing.fields.GOptionsFieldPlusChooser;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public abstract class AbstractObjectChooserParam extends AbstractParam {

    protected GOptionsFieldPlusChooser fChooser;

    /**
     * Class constructor
     *
     * @param name
     * @param desc
     * @param hint
     * @param reqd IMP IMP as this is an optionS the default must be an array too
     *             Object[] is an Object, so confusion can set in - and hence tightly constrained constructor
     */
    AbstractObjectChooserParam(String name, String nameEnglish, Class[] classes, String desc, Object[] def, Object[] hints, boolean reqd) {
        super(name, nameEnglish, classes, desc, def, hints, reqd);
    }

    AbstractObjectChooserParam(String name, String nameEnglish, Class cl,
                               String desc,
                               Object[] def, Object[] hints, boolean reqd) {
        this(name, nameEnglish, new Class[]{cl}, desc, def, hints, reqd);
    }

    protected static String format(final Object[] vals) {

        if (vals == null) {
            return "";
        }

        StringBuffer buf = new StringBuffer();

        for (int i = 0; i < vals.length; i++) {
            if (vals[i] == null) {
                continue;
            }

            buf.append(vals[i].toString().trim());

            if (i != vals.length - 1) {
                buf.append(',');
            }
        }

        return buf.toString();
    }

    public void setValue(String[] ss) {
        //log.debug("CALLING SUPER SETVALUES");
        super.setValue(ss);
    }

    public GFieldPlusChooser getSelectionComponent() {

        if (fChooser == null) {
            //fChooser = new GOptionsFieldPlusChooser(getActionListener(), Application.getWindowManager().getRootFrame());
            // do in 2 stages, as the al needs a valid (non-null) chooser at its construction
            fChooser = new GOptionsFieldPlusChooser(false, createHelpAction());
            fChooser.setCustomActionListener(getActionListener());
            String text = this.getValueStringRepresentation(false);
            if (text == null) {
                text = format((Object[]) getDefault());
            }

            if (isFileBased()) { // as otherwise lots of exceptions thrown if user edits a bad file
                // @todo but problem is that no way to cancel and "null out" a choice once made
                //fChooser.getTextField().setEditable(false);
            }

            log.debug("setting text: " + text + " " + getDefault());
            fChooser.setText(text);
            fChooser.setListSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            ParamHelper.addDocumentListener(fChooser.getTextField(), this);
        }

        return fChooser;
    }

    protected ActionListener getActionListener() {
        return new MyActionListener();
    }

    // we impl custom here as dont want existing text to be wiped out if the button is clicked but
    // no choice made
    // Plus so that we can select the current ones if possible
    class MyActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {

            Object prev = getValue();
            Object[] sels;
            if (prev != null && prev instanceof Object[]) {
                sels = fChooser.getJListWindow().show(getHints(), (Object[]) prev);
            } else {
                sels = fChooser.getJListWindow().show(getHints(), new Object[]{});
            }

            //log.debug("Got selections: " + sels);

            if ((sels == null) || (sels.length == 0)) { // <-- @note

            } else {
                String str = format(sels);
                fChooser.setText(str);
            }
        }
    }

    // param full is NA
    // have to make the strs into paths
    public String getValueStringRepresentation(boolean full) {

        Object val = getValue();

        if (val == null) {
            return null;
        }

        Object[] objs = (Object[]) val;

        return format(objs);
    }

    public boolean isFileBased() {
        return false;
    }

} // End AbstractObjectsChooserParam
