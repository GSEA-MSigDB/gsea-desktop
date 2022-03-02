/*
 * Copyright (c) 2003-2022 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.xbench.xchoosers;

import com.jidesoft.dialog.ButtonPanel;
import edu.mit.broad.genome.JarResources;
import edu.mit.broad.genome.alg.ComparatorFactory;
import edu.mit.broad.genome.objects.*;
import edu.mit.broad.genome.parsers.ParserFactory;
import edu.mit.broad.genome.swing.fields.GFieldUtils;
import edu.mit.broad.genome.swing.fields.GOptionsFieldPlusChooser;
import edu.mit.broad.xbench.RendererFactory2;
import edu.mit.broad.xbench.core.api.Application;
import edu.mit.broad.xbench.core.api.DialogDescriptor;
import gnu.trove.TIntArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.*;
import java.util.List;

/**
 * Just the window component to show and get the template(s)
 * (NO chooser text field!)
 * <p/>
 * IMP IMP read me
 * <p/>
 * The reason this is soo complex is that we want to support both a GUI and a command line
 * specification of multiple templates (from the same main one)
 * Hence we cannot return an object and insterad have to return a formated string
 * (ie the same string that would be specified on the cmd line)
 * <p/>
 * <p/>
 * Notes
 * - the aux templates are always from the same mainTemplate (which can itself be aux or not)
 * -
 */
public class TemplateChooserUI {

    protected JComboBox cbTemplates;

    private Logger log = LoggerFactory.getLogger(TemplateChooserUI.class);

    private boolean fIsMultiAllowed;

    private TemplateMode fMode;

    private JList jlOptions;

    private int fSelectionMode;

    private JPanel chooserPanel;

    private boolean fComboTemplateSourceMode;

    private TemplateCreatorWidgets.OnTheFlyFromSampleNames otf;
    private TemplateCreatorWidgets.GenePhenotype gtf;

    /**
     * Class constructor
     *
     * @param isMultiAllowed
     */
    public TemplateChooserUI(boolean isMultiAllowed, TemplateMode mode) {

        this.fIsMultiAllowed = isMultiAllowed;
        this.fMode = mode;

    }

    private void jbInit() {

        this.jlOptions = new JList();

        if (fIsMultiAllowed) {
            jlOptions.setBorder(BorderFactory.createTitledBorder("Select one or more phenotype(s)"));
        } else {
            jlOptions.setBorder(BorderFactory.createTitledBorder("Select one phenotype)"));
        }

        jlOptions.setCellRenderer(new Renderer());
        setListOptionsInComboMode(false);

        // sadly tough to set the usual renderer as we pass Strings and not pobs. Alos want a simpliefied name shown
        if (fIsMultiAllowed) {
            fSelectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION;
        } else {
            fSelectionMode = ListSelectionModel.SINGLE_SELECTION;
        }

        cbTemplates = new JComboBox();
        cbTemplates.setBorder(BorderFactory.createTitledBorder("Select source file"));

        // dont bind via object bindery regular method as that grabs all templates - aux and not
        // we want to only show in the jcombobox the main (i.e non aux) templates
        // the aux templates will get auto displayed in the jlist when a cb item is choosen
        //ObjectBindery.bind(cbTemplates, Template.class);
        ComboBoxModel model = ParserFactory.getCache().createBoxModel(Template.class);
        cbTemplates.setModel(new TemplateNonAuxBoxModel(model));
        cbTemplates.setRenderer(new RendererFactory2.CommonLookListRenderer(true));

        // dont use, prefer to use item listener as item listener can be controlled
        // to react only ti changes
        // else this methods gets called on all template parsing events too!
        //cbTemplates.addActionListener(new MyActionListener());
        cbTemplates.addItemListener(new MyItemListener());
        if (cbTemplates.getModel().getSize() > 0) {
            cbTemplates.setSelectedIndex(0);
        }
        //doTemplateSelection((Template) cbTemplates.getSelectedItem());
        doTemplateSelection(cbTemplates.getSelectedItem());

        JPanel tcPanel = new JPanel();
        tcPanel.setLayout(new BorderLayout());
        tcPanel.add(cbTemplates, BorderLayout.NORTH);
        tcPanel.add(new JScrollPane(jlOptions), BorderLayout.CENTER);


        JButton bShowComboPhenotypes = new JButton("Show phenotypes from all source files");
        bShowComboPhenotypes.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // get all current templates from cache
                List allTss_orig = ParserFactory.getCache().getCachedObjectsL(Template.class);
                //allTss_orig = splitContinuousTemplates();
                List allTss = qualifyByTypeAndMode(allTss_orig, true);
                Collections.sort(allTss, new ComparatorFactory.PobComparator());
                List allTds = new ArrayList();
                for (int i = 0; i < allTss.size(); i++) {
                    Template template = (Template) allTss.get(i);
                    TemplateDerivative td = new TemplateDerivatives.PseudoTemplateDerivative(template);
                    allTds.add(td);
                }

                DefaultComboBoxModel model = new DefaultComboBoxModel(allTds.toArray(new TemplateDerivative[allTds.size()]));
                jlOptions.setModel(model);
                jlOptions.setSelectionMode(fSelectionMode);
                setListOptionsInComboMode(true);
            }
        });

        this.chooserPanel = new JPanel(new BorderLayout());

        this.chooserPanel.add(tcPanel, BorderLayout.CENTER);

        final ButtonPanel bp = new ButtonPanel(ButtonPanel.NO_LESS_THAN);
        bp.setBorder(BorderFactory.createTitledBorder("Options"));
        bp.addButton(bShowComboPhenotypes);

        final JButton bOnTheFly = new JButton("Create an on-the-fly phenotype ...");
        bOnTheFly.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (otf == null) {
                    otf = new TemplateCreatorWidgets.OnTheFlyFromSampleNames();
                }
                DialogDescriptor dd = Application.getWindowManager().createDialogDescriptor("On-the-fly phenotype by sample names", otf, JarResources.createHelpAction("on_the_fly_phenotype"));
                dd.setOnlyShowCloseOption();
                dd.show();
            }
        });
        bp.addButton(bOnTheFly);

        final JButton bFromGene = new JButton("Use a gene as the phenotype ...");
        bFromGene.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (gtf == null) {
                    gtf = new TemplateCreatorWidgets.GenePhenotype();
                }
                DialogDescriptor dd = Application.getWindowManager().createDialogDescriptor("Use a gene as the phenotype", gtf, JarResources.createHelpAction("gene_profile_phenotype"));
                dd.setOnlyShowCloseOption();
                dd.show();
            }
        });
        bp.addButton(bFromGene);

        this.chooserPanel.add(bp, BorderLayout.SOUTH);
    }

    private void setListOptionsInComboMode(boolean value) {

        if (value) {
            this.fComboTemplateSourceMode = true;
            jlOptions.setForeground(Color.MAGENTA);
        } else {
            this.fComboTemplateSourceMode = false;
            jlOptions.setForeground(Color.BLACK);
        }

    }

    private JComponent createChooserPanel(final TemplateSelection sel) throws Exception {

        if (chooserPanel == null) {
            jbInit();
        }

        // careful with rebuild / reset the model here -> that ruins the selection policy
        TIntArrayList indices = new TIntArrayList();
        if (sel != null && (sel instanceof TemplateSelectionMultiSource == false) && sel.getMainObject() != null) {
            final Object[] options = createTemplateOptions_safe(sel.getMainObject(), false); // we want to show the .cls ones here
            DefaultListModel model = new DefaultListModel();
            for (int i = 0; i < options.length; i++) {
                model.add(i, options[i]);
            }
            jlOptions.setModel(model);

            if (sel.getTemplateNames() != null && sel.getTemplateNames().size() < 3) {
                jlOptions.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); // disable multi sels if only 1 or 2 options (i.e a simple 2 class template)
            } else {
                jlOptions.setSelectionMode(fSelectionMode);
            }

            if (sel.getTemplateNames() != null) {
                Iterator it = sel.getTemplateNames().iterator();
                while (it.hasNext()) {
                    Object obj = it.next();
                    int index = model.indexOf(obj);
                    if (index != -1) {
                        indices.add(index);
                    }
                }
            }
        }

        if (indices.size() == 0) {
            safeSelectFirst(jlOptions);
        } else {
            jlOptions.setSelectedIndices(indices.toNativeArray());
        }

        return chooserPanel;
    }

    public TemplateSelection showChooser(final TemplateSelection selBag) throws Exception {

        JComponent comp = createChooserPanel(selBag);

        String text = "Select a phenotype";

        if (fSelectionMode == ListSelectionModel.MULTIPLE_INTERVAL_SELECTION) {
            text = "Select one or more phenotype(s)";
        }

        DialogDescriptor desc = Application.getWindowManager().createDialogDescriptor(text, comp, JarResources.createHelpAction("cls"));
        desc.enableDoubleClickableJList(jlOptions);
        int res = desc.show();

        if (res == DialogDescriptor.CANCEL_OPTION) {
            return null;
        } else {

            TemplateSelection sel;

            Object[] objs = jlOptions.getSelectedValues();
            if (fComboTemplateSourceMode) {
                sel = new TemplateSelectionMultiSource();
                for (int i = 0; i < objs.length; i++) {
                    sel.add((TemplateDerivative) objs[i], false, true);
                }

            } else {
                sel = new TemplateSelection(cbTemplates.getSelectedItem());
                for (int i = 0; i < objs.length; i++) {
                    sel.add((TemplateDerivative) objs[i], false, false);
                }
            }

            return sel;
        }

    }


    private Map fTemplateOptionsArrayCacheMap = new HashMap();

    private TemplateDerivative[] createTemplateOptions_safe(final Object fullTemplate, final boolean onlyHashOnesForBiphasic) {
        try {

            if (fullTemplate instanceof Template) {
                return createTemplateOptions_from_template((Template) fullTemplate, onlyHashOnesForBiphasic);
            } else if (fullTemplate instanceof TemplateNonAuxBoxModel.TemplateContWrapper) {
                return createTemplateOptions_file_cont(((TemplateNonAuxBoxModel.TemplateContWrapper) fullTemplate).sourceFile);
            } else if (fullTemplate instanceof File) {
                return createTemplateOptions_file_cont((File) fullTemplate);
            } else {
                throw new IllegalArgumentException("Unknown object: " + fullTemplate + " " + fullTemplate.getClass());
            }

        } catch (Exception e) {
            Application.getWindowManager().showError("Error making Template options", e);
            return new TemplateDerivative[]{};
        }

    }

    // does the real work
    private TemplateDerivative[] createTemplateOptions_file_cont(final File fullTemplate_file) throws Exception {

        if (fullTemplate_file == null) {
            return new TemplateDerivative[]{};
        }

        if (log.isDebugEnabled()) { log.debug("Creating template from source file: {}", fullTemplate_file.getPath()); }

        Object obj = fTemplateOptionsArrayCacheMap.get(fullTemplate_file);
        if (obj != null) { // cache it
            return (TemplateDerivative[]) obj;
        }

        // continuous templates (they are not all in cache as cache clobbers multi cls from same file)
        Template[] cts = ParserFactory.readTemplates(fullTemplate_file);
        Set set = new HashSet();
        for (int i = 0; i < cts.length; i++) {
            set.add(cts[i]);
        }
        cts = (Template[]) set.toArray(new Template[set.size()]);
        cts = qualifyByTypeAndMode(cts, false);


        TemplateDerivative[] tds = new TemplateDerivative[cts.length];
        for (int i = 0; i < cts.length; i++) {
            tds[i] = new TemplateDerivatives.ContTemplateDerivative(cts[i].getName(), fullTemplate_file);
        }


        fTemplateOptionsArrayCacheMap.put(fullTemplate_file, tds);
        return tds;
    }

    private TemplateDerivative[] createTemplateOptions_from_template(final Template fullTemplate,
                                                                     final boolean onlyHashOnesForBiphasic) throws Exception {

        if (fullTemplate == null) {
            return new TemplateDerivative[]{};
        }

        Object obj = fTemplateOptionsArrayCacheMap.get(fullTemplate);
        if (obj != null) { // cache it
            return (TemplateDerivative[]) obj;
        }

        boolean addOrig = true;

        Template[] tss = TemplateFactory.extractAllPossibleTemplates(fullTemplate, addOrig); // @note

        tss = qualifyByTypeAndMode(tss, onlyHashOnesForBiphasic);

        List tdsList = new ArrayList();

        for (int i = 0; i < tss.length; i++) {
            tdsList.add(new TemplateDerivatives.AuxTemplateDerivative(tss[i].getName(), fullTemplate));
        }

        TemplateDerivative[] tds = (TemplateDerivative[]) tdsList.toArray(new TemplateDerivative[tdsList.size()]);
        fTemplateOptionsArrayCacheMap.put(fullTemplate, tds);
        return tds;
    }

    private void safeSelectFirst(JList jl) {
        if (jl.getModel().getSize() >= 1) {
            jl.setSelectedIndex(0);
        }
    }

    class MyItemListener implements ItemListener {
        public void itemStateChanged(ItemEvent e) {
            try {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    Object sel = cbTemplates.getSelectedItem();
                    if (sel != null) {
                        doTemplateSelection(cbTemplates.getSelectedItem());
                    }

                    setListOptionsInComboMode(false);

                } else { // imp
                }

            } catch (Throwable t) {
                Application.getWindowManager().showError("Trouble making Templates", t);
            }
        }
    }

    private void doTemplateSelection(final Object selectedMainTemplate) {
        if (selectedMainTemplate == null) {
            return;
        }

        // careful with rebuild / reset the model here -> that ruins the selection policy
        Object[] options = createTemplateOptions_safe(selectedMainTemplate, true); // we dont want to show the .cls ones here
        DefaultListModel model = new DefaultListModel();
        for (int i = 0; i < options.length; i++) {
            model.add(i, options[i]);
        }
        jlOptions.setModel(model);

        if (options.length < 3) {
            jlOptions.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); // disable multi sels if only 1 or 2 options (i.e a simple 2 class template)
        } else {
            jlOptions.setSelectionMode(fSelectionMode);
        }

        safeSelectFirst(jlOptions);
    }

    private List qualifyByTypeAndMode(final List templates, boolean onlyAuxForBiphasicOnes) {

        //log.debug("# before qualifyByTypeAndMode: " + templates.size());
        final Template[] tss = qualifyByTypeAndMode((Template[]) templates.toArray(new Template[templates.size()]), onlyAuxForBiphasicOnes);

        //log.debug("# after qualifyByTypeAndMode: " + templates.size());

        List list = new ArrayList();
        for (int i = 0; i < tss.length; i++) {
            list.add(tss[i]);
        }

        return list;
    }

    // this is for the jlist and NOT the jcombobox
    private Template[] qualifyByTypeAndMode(final Template[] tss, final boolean onlyHashOnesForBiphasic) {
        List list = new ArrayList();

        if (fMode == TemplateMode.CONTINUOUS_ONLY) {
            for (int i = 0; i < tss.length; i++) {
                if (tss[i].isContinuous()) {
                    list.add(tss[i]);
                }
            }
        } else if (fMode == TemplateMode.CATEGORICAL_2_CLASS_ONLY) {
            for (int i = 0; i < tss.length; i++) {
                if (onlyHashOnesForBiphasic) {
                    if ((tss[i].isContinuous() == false) && (tss[i].getNumClasses() == 2) && tss[i].getName().indexOf('#') != -1) {
                        list.add(tss[i]);
                    }
                } else {
                    if ((tss[i].isContinuous() == false) && (tss[i].getNumClasses() == 2)) {
                        list.add(tss[i]);
                    }
                }
            }
        } else if (fMode == TemplateMode.CATEGORICAL_ONLY) {
            for (int i = 0; i < tss.length; i++) {
                if (tss[i].isContinuous() == false) {
                    list.add(tss[i]);
                }
            }
        } else if (fMode == TemplateMode.UNIPHASE_ONLY) {
            for (int i = 0; i < tss.length; i++) {
                if (tss[i].getNumClasses() == 1) {
                    list.add(tss[i]);
                }
            }
        } else if (fMode == TemplateMode.ALL) {
            return tss;
        } else if (fMode == TemplateMode.CATEGORICAL_2_CLASS_AND_NUMERIC) {

            for (int i = 0; i < tss.length; i++) {
                if (tss[i].isContinuous()) {
                    list.add(tss[i]);
                }
            }

            for (int i = 0; i < tss.length; i++) {
                if (onlyHashOnesForBiphasic) {
                    if ((tss[i].isContinuous() == false) && (tss[i].getNumClasses() == 2) && tss[i].getName().indexOf('#') != -1) {
                        list.add(tss[i]);
                    }
                } else {
                    if ((tss[i].isContinuous() == false) && (tss[i].getNumClasses() == 2)) {
                        list.add(tss[i]);
                    }
                }
            }


        } else {
            throw new RuntimeException("Unknown mode: " + fMode);
        }
        return (Template[]) list.toArray(new Template[list.size()]);
    }

    class Renderer extends DefaultListCellRenderer {

        public Component getListCellRendererComponent(JList list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {

            // doesnt work properly unless called
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            String text;

            if (value instanceof TemplateDerivative) {
                text = ((TemplateDerivative) value).getName(fComboTemplateSourceMode, false); // never show path here
            } else {
                throw new RuntimeException("Unknown object to render: " + value);
            }

            this.setText(text);
            return this;
        }

    }
    
    public static class Field extends GOptionsFieldPlusChooser {

        public Field(ActionListener al) {
            this.setLayout(new BorderLayout());

            tfEntry = new MyTextField();
            this.add(tfEntry, BorderLayout.CENTER);
            this.add(bEntry, BorderLayout.EAST);
            bEntry.addActionListener(al);
        }

        public void setText(String text) {
            super.setText(text);
            if (text == null) {
                return;
            }

            tfEntry.setForeground(GFieldUtils.getFileFieldColor(text));
        }

        private class MyTextField extends JTextField {
            public void processKeyEvent(KeyEvent ev) {
                Document doc = tfEntry.getDocument();
                try {
                    String text = tfEntry.getDocument().getText(0, doc.getLength());
                    this.setForeground(GFieldUtils.getFileFieldColor(text));
                    //ev.consume();
                    super.processKeyEvent(ev);
                } catch (javax.swing.text.BadLocationException e) {
                    super.processKeyEvent(ev);
                }
            }
        }
    }
}
