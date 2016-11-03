/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xtools.api.param;

import edu.mit.broad.genome.JarResources;
import edu.mit.broad.genome.alg.ComparatorFactory;
import edu.mit.broad.genome.objects.PersistentObject;
import edu.mit.broad.genome.parsers.ParseUtils;
import edu.mit.broad.genome.parsers.ParserFactory;
import edu.mit.broad.genome.swing.fields.GFieldPlusChooser;
import edu.mit.broad.vdb.VdbRuntimeResources;
import edu.mit.broad.vdb.chip.Chip;
import edu.mit.broad.vdb.chip.ChipHelper;
import edu.mit.broad.xbench.core.ObjectBindery;
import edu.mit.broad.xbench.prefs.XPreferencesFactory;

import org.genepattern.uiutil.FTPFile;
import org.genepattern.uiutil.FTPList;

import xapps.gsea.GseaWebResources;
import xtools.api.ui.NamedModel;

import javax.swing.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
abstract class WChipChooserAbstractParam extends AbstractParam {

    private boolean fMultipleAllowed = true;

    private MyPobActionListener fAl;

    protected WChipChooserUI fChooser;

    /**
     * Class constructor
     *
     * @param name
     * @param nameEnglish
     * @param desc
     * @param reqd
     * @param multipleAlllowed
     */
    WChipChooserAbstractParam(final String name,
                              final String nameEnglish,
                              final String desc,
                              final boolean reqd,
                              final boolean multipleAlllowed) {
        super(name, nameEnglish, Chip[].class, desc, new Chip[]{}, new Chip[]{}, reqd);
        this.fMultipleAllowed = multipleAlllowed;
    }

    //-----------------------------------------------------------------------//
    //------------------------- CORE METHODS --------------------------------//
    //-----------------------------------------------------------------------//
    protected Object[] _getObjects() throws Exception {

        Object val = getValue();

        Object[] objs;

        // cant et a ftp file object because it has to be a string n the obect chooser text area
        // log.debug("value = " + val + " class: " + val.getClass());

        if (val instanceof String) {
            String[] paths = _parse(val.toString());
            objs = VdbRuntimeResources.getChips(paths);
        } else if (val instanceof Object[]) {
            objs = (Object[]) val;
        } else {
            objs = new Object[]{val};
        }

        return objs;
    }

    protected Chip _getChip() throws Exception {
        return ChipHelper.createComboChip(_getChips());
    }

    protected Chip[] _getChips() throws Exception {
        Object[] objs = _getObjects();

        //System.out.println(">>> " + objs + " " + objs.length);
        if (isReqd() && objs.length == 0) {
            throw new IllegalArgumentException("Must specify Chip parameters, got: " + objs.length);
        }

        Chip[] chips = new Chip[objs.length];

        for (int i = 0; i < objs.length; i++) {
            chips[i] = (Chip) objs[i];
        }

        return chips;
    }

    private static String[] _parse(final String s) {

        if (s == null) {
            throw new IllegalArgumentException("Parameter s cannot be null");
        }

        Set vals = ParseUtils.string2stringsSet(s, ",", false); // only commas!!

        System.out.println("to parse>" + s + "< got: " + vals);

        Set use = new HashSet();
        for (Iterator it = vals.iterator(); it.hasNext();) {
            String key = it.next().toString();
            if (key.length() > 0) {
                use.add(key);
            }
        }

        return (String[]) use.toArray(new String[use.size()]);
    }

    // override base class method to do for both pobs and strings
    private String format(final Object[] vals) {
        if (vals == null) {
            return "";
        }

        StringBuffer buf = new StringBuffer();

        for (int i = 0; i < vals.length; i++) {

            if (vals[i] == null) {
                continue;
            }

            log.debug(vals[i].getClass());

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

    // override base impl
    protected ActionListener getActionListener() {
        if (fAl == null) {
            this.fAl = new MyPobActionListener(fMultipleAllowed);
            fAl.setChooser(fChooser);
        }

        return fAl;
    }

    public boolean isFileBased() {
        return true;
    }

    // redo from the abstract super class here as we dont want to swap out the model
    // (model is the datasets etc)
    private static class MyPobActionListener implements ActionListener {

        private boolean fMultipleAllowed;
        private WChipChooserUI fChooser;
        private ChipListRenderer rend = new ChipListRenderer();

        public MyPobActionListener(boolean multipleAllowed) {
            this.fMultipleAllowed = multipleAllowed;
        }

        // cant have this in the class constructor as the action list needs to
        // be instantiated before the chooser object is made
        public void setChooser(WChipChooserUI chooser) {
            this.fChooser = chooser;
        }

        private ListModel createFTPModel() {

            if (XPreferencesFactory.kOnlineMode.getBoolean() == false) {
                DefaultListModel model = new DefaultListModel();
                model.addElement("Offline mode");
                model.addElement("Change this in Menu=>Preferences");
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
                    klog.error(e);
                    DefaultListModel model = new DefaultListModel();
                    model.addElement("Error listing Broad website");
                    model.addElement(e.getMessage());
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
                //klog.debug("Chooser not yet inited: " + fChooser);
                return;
            }

            int selmode;
            if (fMultipleAllowed == false) {
                selmode = ListSelectionModel.SINGLE_SELECTION;
            } else {
                selmode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION;
            }
            
            NamedModel[] models;
            final NamedModel chipsFromFTPModel = new NamedModel("Chips (from website)", createFTPModel());

            models = new NamedModel[]
                    {
                            chipsFromFTPModel,
                            new NamedModel("Chips (local .chip)", ObjectBindery.getModel(Chip.class))
                    };

            final Object[] sels = fChooser.getJListWindow().showDirectlyWithModels(models, selmode, rend);

            if ((sels == null) || (sels.length == 0)) { // <-- @note
            } else {
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


    // just paint affy in one color, mouse in another
    static class ChipListRenderer extends DefaultListCellRenderer {
        private boolean skipRenderCheck = false;
        
        public boolean isSkipRenderCheck() {
            return skipRenderCheck;
        }

        public void setSkipRenderCheck(boolean skipRenderCheck) {
            this.skipRenderCheck = skipRenderCheck;
        }

        static Icon affy_icon = JarResources.getIcon("Chip16.png");

        public Component getListCellRendererComponent(JList list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {

            // doesnt work properly unless called
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            //log.debug(value + " class: " + value.getClass());
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

            if (isSelected == false && !skipRenderCheck) {
                if (VdbRuntimeResources.isChipAffy_hacky(str)) {
                    setForeground(Color.BLUE);
                    setIcon(affy_icon);
                } else if (VdbRuntimeResources.isChipGeneSymbol(str)) {
                    setForeground(Color.MAGENTA);
                    setIcon(null);
                } else if (VdbRuntimeResources.isChipSeqAccession(str)) {
                    setForeground(Color.MAGENTA);
                    setIcon(null);
                } else {
                    setForeground(Color.BLACK);
                    setIcon(null);
                }
            }

            return this;
        }
    }    // End ChipListRenderer

    // have to make the strs into paths
    public String getValueStringRepresentation(final boolean full) {

        Object val = getValue();

        if (val == null) {
            return null;
        }

        // log.debug("value: " + val.getClass() + " " + val);

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
            //fChooser = new GOptionsFieldPlusChooser(getActionListener(), Application.getWindowManager().getRootFrame());
            // do in 2 stages, as the al needs a valid (non-null) chooser at its construction
            fChooser = new WChipChooserUI(false);
            fChooser.setCustomActionListener(getActionListener());
            String text = this.getValueStringRepresentation(false);
            if (text == null) {
                text = format((Object[]) getDefault());
            }

            if (isFileBased()) { // as otherwise lots of exceptions thrown if user edits a bad file
                // @todo but probelm is that no way to cancel and "null out" a choice once made
                //fChooser.getTextField().setEditable(false);
            }

            fChooser.setText(text);
            ParamHelper.addDocumentListener(fChooser.getTextField(), this);
        }

        return fChooser;
    }

} // End class WChipChooserAbstractParam
