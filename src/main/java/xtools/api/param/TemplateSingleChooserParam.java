/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xtools.api.param;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import edu.mit.broad.genome.Constants;
import edu.mit.broad.genome.Printf;
import edu.mit.broad.genome.XLogger;
import edu.mit.broad.genome.objects.Dataset;
import edu.mit.broad.genome.objects.Template;
import edu.mit.broad.genome.objects.TemplateFactory;
import edu.mit.broad.genome.objects.TemplateMode;
import edu.mit.broad.genome.parsers.AuxUtils;
import edu.mit.broad.genome.parsers.ParserFactory;
import edu.mit.broad.genome.swing.fields.GFieldPlusChooser;
import edu.mit.broad.genome.utils.SystemUtils;
import edu.mit.broad.xbench.core.api.Application;
import edu.mit.broad.xbench.xchoosers.TemplateChooserUI;
import edu.mit.broad.xbench.xchoosers.TemplateSelection;

/**
 * Only 1 template choosable
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class TemplateSingleChooserParam extends StringMultiChooserParam implements ActionListener {

    protected TemplateMode fMode;
    private TemplateChooserUI fTemplateChooser;
    private TemplateSelection fCurrBag;
    private Logger log = XLogger.getLogger(TemplateSingleChooserParam.class);

    public TemplateSingleChooserParam(String name, String nameEnglish, TemplateMode mode, boolean reqd) {
        super(name, nameEnglish, CLS_DESC, new String[]{}, new String[]{}, reqd);
        this.fMode = mode;
    }

    public GFieldPlusChooser getSelectionComponent() {
        return _getSelectionComponent();
    }

    public Template getTemplate() throws Exception {
        return _getTemplates(null)[0];
    }

    public boolean isFileBased() {
        return true;
    }

    protected GFieldPlusChooser _getSelectionComponent() {
    
        if (fTemplateChooser == null) {
            //log.debug("creating TemplateChooser component");
            fTemplateChooser = new TemplateChooserUI(false, fMode);
            fChooser = new TemplateChooserUI.Field(this);
            fChooser.setValue(getValueStringRepresentation(true));
            ParamHelper.addDocumentListener(fChooser.getTextField(), this);
        }
    
        return fChooser;
    }

    public void actionPerformed(ActionEvent e) {
        try {
            TemplateSelection bag = fTemplateChooser.showChooser(fCurrBag);
            if (bag != null) {
                fChooser.setText(bag.formatForUI());
            }
            this.fCurrBag = bag;
        } catch (Throwable t) {
            Application.getWindowManager().showError("Could not query Phenotypes", t);
        }
    }

    private Template[] _getGeneTemplates(final String[] ss, final Dataset ds) {
    
        if (ss == null || ss.length == 0) {
            return null;
        }
    
        try {
            ds.getRowIndex(ss[0]);
    
            Template[] tss = new Template[ss.length];
            for (int i = 0; i < ss.length; i++) {
                tss[i] = TemplateFactory.createContinuousTemplate(ss[i], ds);
            }
    
            return tss;
    
        } catch (Throwable t) {
            return null; // not a gene template
        }
    }

    protected Template[] _getTemplates(Dataset dsOptX) throws Exception {
    
        String[] ss = getStrings();
        Printf.out(ss);
    
        final Template[] nn_ts = _getGeneTemplates(ss, dsOptX);
        if (nn_ts != null) {
            return nn_ts;
        }
    
        if (ss.length == 0) {
            throw new IllegalArgumentException("No templates specified -- 0 length str array");
        }
    
        List templates = new ArrayList();
        Template currMainTemplate = null;
    
        for (int i = 0; i < ss.length; i++) {
            if (!_isPath(ss[i])) { // either aux or an auto-splitter word, with just the auxname or with a fullpath
                currMainTemplate = _getSourceTemplate(ss[i], currMainTemplate);
                String auxname = AuxUtils.getAuxNameOnlyNoHash(ss[i]);
                log.debug("parsing: " + ss[i] + "< and i got auxname>" + auxname + "<" + " currMaintemplate: " + currMainTemplate.getName());
                //TODO: Simplify? Vestigal case from former MultiChooserParam 
                if (auxname.equals(Constants.ONE_VERSUS_ALL)) {
                    throw new IllegalArgumentException("Invalid Template option: " + auxname + " multiple templates not allowed in this usage");
                } else if (auxname.equals(Constants.ONE_VERSUS_ALL_ONLY_FORWARD)) {
                    throw new IllegalArgumentException("Invalid Template option: " + auxname + " multiple templates not allowed in this usage");
                } else if (auxname.equals(Constants.ALL_PAIRS)) {
                    throw new IllegalArgumentException("Invalid Template option: " + auxname + " multiple templates not allowed in this usage");
                } else {
                    File file = ParserFactory.getCache().getSourceFile(currMainTemplate);
                    file = AuxUtils.getBaseFileFromAuxFile(file);
                    Template t = ParserFactory.readTemplate(new File(file.getPath() + "#" + auxname));
                    templates.add(t);
                }
    
            } else {//has to be a path
                Template t = ParserFactory.readTemplate(new File(ss[i]), true, false, true); // @note imp to use cache for the templates from sample names
                templates.add(t);
                currMainTemplate = t;
            }
        }
    
        return (Template[]) templates.toArray(new Template[templates.size()]);
    }

    private boolean _isPath(String pathOrName) {
        if (pathOrName.indexOf(File.separator) == -1) {
            return false;
        } else {
            return true;
        }
    }

    private Template _getSourceTemplate(String pathOrName, Template currTemplate) throws Exception {
        //System.out.println("asking for: " + pathOrName);
        if (pathOrName.indexOf(File.separatorChar) == -1) { // eg, not a path
            if (currTemplate == null) {
                // may still be in cwd (dir of execution so try) josh gould fix
                return ParserFactory.readTemplate(new File(SystemUtils.getPwd(), pathOrName));
            } else {
                return currTemplate;
            }
        } else {
            return ParserFactory.readTemplate(new File(pathOrName));
        }
    }

}    // End class TemplateSingleChooserParam
