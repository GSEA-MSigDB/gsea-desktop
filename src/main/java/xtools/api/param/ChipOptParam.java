/*
 * Copyright (c) 2003-2022 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xtools.api.param;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import edu.mit.broad.genome.objects.PersistentObject;
import edu.mit.broad.genome.parsers.ParserFactory;
import edu.mit.broad.genome.swing.fields.GFieldPlusChooser;
import edu.mit.broad.vdb.VdbRuntimeResources;
import edu.mit.broad.vdb.chip.Chip;

public class ChipOptParam extends AbstractParam {
    private WChipChooserUI fChooser;

    public ChipOptParam(boolean reqd) {
        this(CHIP, CHIP_ENGLISH, CHIP_DESC, reqd);
    }

    public ChipOptParam(final String name, final String nameEnglish, final String desc, final boolean reqd) {
        super(name, nameEnglish, Chip.class, desc, null, new Chip[]{}, reqd);
    }
    
    public Chip getChip() throws Exception {
        Object val = getValue();
        if (val == null) return null;
        return VdbRuntimeResources.getChip(val.toString());
    }

    private String format(final Object[] vals) {
        if (vals == null) { return ""; }

        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < vals.length; i++) {
            if (vals[i] == null) { continue; }

            log.debug("{}", vals[i].getClass());

            if (vals[i] instanceof PersistentObject) {
                String p = ParserFactory.getCache().getSourcePath(vals[i]);
                buf.append(p);
            } else {
                buf.append(vals[i].toString().trim());
            }

            if (i != vals.length - 1) {
                buf.append(',');
            }
        }

        return buf.toString();
    }

    public boolean isFileBased() { return true; }

    // redo from the abstract super class here as we dont want to swap out the model
    // (model is the datasets etc)
    private static class MyPobActionListener implements ActionListener {
        private WChipChooserUI fChooser;

        public MyPobActionListener() { }

        // cant have this in the class constructor as the action list needs to
        // be instantiated before the chooser object is made
        public void setChooser(WChipChooserUI chooser) {
            this.fChooser = chooser;
        }

        public void actionPerformed(ActionEvent e) {
            if (fChooser == null) { return; }

            final String[] selectedPaths = fChooser.getJListWindow().showDirectlyWithModels();
            if ((selectedPaths != null) && (selectedPaths.length > 0)) {
                // TODO: push refactoring into ChooserHelper
                fChooser.setText(ChooserHelper.formatPob(selectedPaths));
            }
        }
    }

    // have to make the strs into paths
    public String getValueStringRepresentation(final boolean full) {
        Object val = getValue();

        if (val == null) {
            return null;
        }

        if (val instanceof String) {
            return (String) val;
        } else if (val instanceof Object[]) {
            Object[] objs = (Object[]) val;
            return format(objs);
        } else {
            return format(new Object[]{val});
        }
    }

    public GFieldPlusChooser getSelectionComponent() {
        if (fChooser == null) {
            // do in 2 stages, as the actionListener needs a valid (non-null) chooser at its construction
            fChooser = new WChipChooserUI();
            MyPobActionListener actionListener = new MyPobActionListener();
            actionListener.setChooser(fChooser);
            fChooser.setCustomActionListener(actionListener);
            String text = this.getValueStringRepresentation(false);
            if (text == null) {
                text = format((Object[]) getDefault());
            }

            fChooser.setText(text);
            ParamHelper.addDocumentListener(fChooser.getTextField(), this);
        }

        return fChooser;
    }
}
