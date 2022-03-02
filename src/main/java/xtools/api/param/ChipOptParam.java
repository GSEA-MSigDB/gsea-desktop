/*
 * Copyright (c) 2003-2022 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xtools.api.param;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;

import org.genepattern.uiutil.FTPFile;
import org.genepattern.uiutil.FTPList;

import edu.mit.broad.genome.alg.ComparatorFactory;
import edu.mit.broad.genome.objects.PersistentObject;
import edu.mit.broad.genome.parsers.ParserFactory;
import edu.mit.broad.genome.swing.fields.GFieldPlusChooser;
import edu.mit.broad.vdb.VdbRuntimeResources;
import edu.mit.broad.vdb.chip.Chip;
import edu.mit.broad.xbench.core.ObjectBindery;
import edu.mit.broad.xbench.prefs.XPreferencesFactory;
import xapps.gsea.GseaWebResources;
import xtools.api.ui.NamedModel;

/**
 * @author Aravind Subramanian, David Eby
 */
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

            if (vals[i] == null) {
                continue;
            }

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
        private ChipListRenderer rend = new ChipListRenderer();

        public MyPobActionListener() { }

        // cant have this in the class constructor as the action list needs to
        // be instantiated before the chooser object is made
        public void setChooser(WChipChooserUI chooser) {
            this.fChooser = chooser;
        }

        private ListModel createFTPModel() {
            if (! XPreferencesFactory.kOnlineMode.getBoolean()) {
                DefaultListModel model = new DefaultListModel();
                model.addElement("Offline mode");
                model.addElement("Change this in Menu=>Preferences");
                model.addElement("Use 'Load Data' to access local CHIP files.");
                model.addElement("Choose chips from other tabs");
                // Bit of a hack: disable special CHIP rendering since none are loaded.
                rend.setSkipRenderCheck(true);
                return model;
            } else {
                try {
                    FTPList ftpList;
                    ftpList = new FTPList(GseaWebResources.getGseaFTPServer(),
                                          GseaWebResources.getGseaFTPServerUserName(),
                                          GseaWebResources.getGseaFTPServerPassword(),
                                          GseaWebResources.getGseaFTPServerChipDir(),
                                          new ComparatorFactory.ChipNameComparator());
                    ftpList.quit();
                    return ftpList.getModel();
                } catch (Exception e) {
                    klog.error(e.getMessage(), e);
                    DefaultListModel model = new DefaultListModel();
                    model.addElement("Error listing Broad website");
                    model.addElement(e.getMessage());
                    model.addElement("Use 'Load Data' to access local CHIP files.");
                    model.addElement("Choose chips from other tabs");
                    // Ditto.  It's more important here since the error message can (and does) contain
                    // Strings that match on the rendering code.
                    rend.setSkipRenderCheck(true);
                    return model;
                }
            }
        }

        public void actionPerformed(ActionEvent e) {
            if (fChooser == null) {
                return;
            }

            NamedModel[] models;
            final NamedModel chipsFromFTPModel = new NamedModel("Chips (from website)", createFTPModel());

            models = new NamedModel[] {
                            chipsFromFTPModel,
                            new NamedModel("Chips (local .chip)", ObjectBindery.getModel(Chip.class))
                    };

            final Object[] sels = fChooser.getJListWindow().showDirectlyWithModels(models, ListSelectionModel.SINGLE_SELECTION, rend);

            if ((sels != null) && (sels.length > 0)) {
                String[] paths = new String[sels.length];
                for (int i = 0; i < sels.length; i++) {
                    if (sels[i] instanceof FTPFile) {
                        paths[i] = ((FTPFile) sels[i]).getPath();
                    } else {
                        paths[i] = ParserFactory.getCache().getSourcePath(sels[i]);
                    }
                }

                String str = ChooserHelper.formatPob(sels);
                fChooser.setText(str);
            }
        }
    }

    static class ChipListRenderer extends DefaultListCellRenderer {
        private boolean skipRenderCheck = false;

        public void setSkipRenderCheck(boolean skipRenderCheck) {
            this.skipRenderCheck = skipRenderCheck;
        }

        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            // order is important
            if (value == null) {
                return this;
            }

            String str;
            if (value instanceof FTPFile) {
                str = ((FTPFile) value).getName();
                this.setText(str);
            } else {
                str = value.toString();
            }

            if (!isSelected && !skipRenderCheck) {
                setForeground(Color.BLACK);
                setIcon(null);
            }

            if (!skipRenderCheck && str.contains(ComparatorFactory.ChipNameComparator.getHighestVersionId())) {
                Font font = this.getFont();
                String fontName = font.getFontName();
                int fontSize = font.getSize();
                this.setFont(new Font(fontName, Font.BOLD, fontSize));
            }
            
            return this;
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
