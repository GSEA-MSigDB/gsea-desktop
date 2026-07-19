/*
 * Copyright (c) 2003-2022 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xtools.api.param;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import edu.mit.broad.genome.Constants;
import edu.mit.broad.genome.Printf;
import edu.mit.broad.genome.objects.Dataset;
import edu.mit.broad.genome.objects.Template;
import edu.mit.broad.genome.objects.TemplateFactory;
import edu.mit.broad.genome.objects.TemplateMode;
import edu.mit.broad.genome.parsers.AuxUtils;
import edu.mit.broad.genome.parsers.ParserFactory;
import edu.mit.broad.genome.utils.SystemUtils;

/**
 * Only 1 template choosable
 *
 * @author Aravind Subramanian
 */
public class TemplateSingleChooserParam extends StringMultiChooserParam {
    protected TemplateMode fMode;

    public TemplateSingleChooserParam(String name, String nameEnglish, TemplateMode mode, boolean reqd) {
        super(name, nameEnglish, CLS_DESC, new String[] {}, new String[] {}, reqd);
        this.fMode = mode;
    }

    public TemplateMode getMode() {
        return fMode;
    }

    public Template getTemplate() throws Exception {
        return _getTemplates(null)[0];
    }

    public boolean isFileBased() {
        return true;
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
            throw new IllegalArgumentException(
                    "No templates specified.  Please load a CLS file and choose the phenotype labels.");
        }

        List<Template> templates = new ArrayList<>();
        Template currMainTemplate = null;

        for (int i = 0; i < ss.length; i++) {
            if (!_isPath(ss[i])) {
                currMainTemplate = _getSourceTemplate(ss[i], currMainTemplate);
                String auxname = AuxUtils.getAuxNameOnlyNoHash(ss[i]);
                log.debug("parsing: " + ss[i] + "< and i got auxname>" + auxname + "<"
                        + " currMaintemplate: " + currMainTemplate.getName());
                if (auxname.equals(Constants.ONE_VERSUS_ALL)) {
                    throw new IllegalArgumentException(
                            "Invalid Template option: " + auxname + " multiple templates not allowed in this usage");
                } else if (auxname.equals(Constants.ONE_VERSUS_ALL_ONLY_FORWARD)) {
                    throw new IllegalArgumentException(
                            "Invalid Template option: " + auxname + " multiple templates not allowed in this usage");
                } else if (auxname.equals(Constants.ALL_PAIRS)) {
                    throw new IllegalArgumentException(
                            "Invalid Template option: " + auxname + " multiple templates not allowed in this usage");
                } else {
                    File file = ParserFactory.getCache().getSourceFile(currMainTemplate);
                    file = AuxUtils.getBaseFileFromAuxFile(file);
                    Template t = ParserFactory.readTemplate(new File(file.getPath() + "#" + auxname));
                    templates.add(t);
                }

            } else {
                Template t = ParserFactory.readTemplate(new File(ss[i]), true, false, true);
                templates.add(t);
                currMainTemplate = t;
            }
        }

        return templates.toArray(new Template[templates.size()]);
    }

    private boolean _isPath(String pathOrName) {
        return pathOrName.indexOf(File.separator) >= 0;
    }

    private Template _getSourceTemplate(String pathOrName, Template currTemplate) throws Exception {
        if (pathOrName.indexOf(File.separatorChar) == -1) {
            if (currTemplate == null) {
                return ParserFactory.readTemplate(new File(SystemUtils.getPwd(), pathOrName));
            } else {
                return currTemplate;
            }
        } else {
            return ParserFactory.readTemplate(new File(pathOrName));
        }
    }
}
