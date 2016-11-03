/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.xchoosers;

import edu.mit.broad.genome.XLogger;
import edu.mit.broad.genome.objects.Template;
import edu.mit.broad.genome.objects.TemplateDerivative;
import edu.mit.broad.genome.parsers.AuxUtils;
import edu.mit.broad.genome.parsers.ParserFactory;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Class to hold a Template Selectection from 1 source file
 */
public class TemplateSelection {

    private Object fMainTemplate_or_mainTemplateFile;

    protected Set fTemplateNamesOrPaths;

    private Logger log = XLogger.getLogger(TemplateSelection.class);

    protected TemplateSelection() {
    }

    /**
     * Class constructor
     *
     * @param mainTemplate
     */
    //files  used for continuous templates
    public TemplateSelection(final Object mainTemplate_or_mainTemplateFile) {
        if (mainTemplate_or_mainTemplateFile == null) {
            throw new IllegalArgumentException("Param mainTemplate_or_mainTemplateFile cannot be null");
        }

        this.fMainTemplate_or_mainTemplateFile = mainTemplate_or_mainTemplateFile;
    }

    public Object getMainObject() {
        return fMainTemplate_or_mainTemplateFile;
    }

    public Set getTemplateNames() {
        if (fTemplateNamesOrPaths == null) {
            log.debug("Odd that fTemplateNamesOrPaths is null. fMainTemplate: " + fMainTemplate_or_mainTemplateFile);
            return new HashSet();
        } else {
            return Collections.unmodifiableSet(fTemplateNamesOrPaths);
        }
    }

    public void add(final TemplateDerivative td, final boolean parentNamePlusMyName, final boolean fullPath) {
        if (fTemplateNamesOrPaths == null) {
            fTemplateNamesOrPaths = new HashSet();
        }

        String add = td.getName(parentNamePlusMyName, fullPath);
        //log.debug("Adding templateNameOrPath: " + add);
        fTemplateNamesOrPaths.add(add);
    }


    // IMP IMP IMP
    // format is:
    // fullpath2mainTemplate#foo,bar,zog
    // OVA and ALL_PAIRS are magic strings that expand
    public String formatForUI() {

        if (fTemplateNamesOrPaths == null) {
            return null;
        }

        String[] vals = (String[]) fTemplateNamesOrPaths.toArray(new String[fTemplateNamesOrPaths.size()]);

        if (vals == null) {
            return "";
        }

        StringBuffer buf = new StringBuffer();
        File file;
        String nm;
        if (fMainTemplate_or_mainTemplateFile instanceof File) {
            file = (File) fMainTemplate_or_mainTemplateFile;
            nm = file.getName();
        } else {
            Template t = (Template) fMainTemplate_or_mainTemplateFile;
            if (ParserFactory.getCache().isCached(t)) {
                file = ParserFactory.getCache().getSourceFile(fMainTemplate_or_mainTemplateFile);
            } else {
                file = new File(t.getName()); // i.e one makde on the fly from sample names
            }
            nm = ((Template) fMainTemplate_or_mainTemplateFile).getName();
        }

        file = AuxUtils.getBaseFileFromAuxFile(file);

        //log.debug("Formatting for UI main template is: " + nm + " and its file path: " + file);
        buf.append(file.getPath());

        //if (fMainTemplate.isContinuous()) {
        //  buf.append('#').append(nm);
        // } else {

        if (vals.length == 0) {
            return buf.toString(); // no extra ones, just the main template
        }

        boolean firstAuxOne = true;
        for (int i = 0; i < vals.length; i++) {
            if (vals[i] == null) {
                continue;
            }

            String name = AuxUtils.getAuxNameOnlyNoHash(vals[i]);
            if (name.equals(nm)) { // dont add, already added

            } else {
                if (firstAuxOne) {
                    buf.append('#');
                    firstAuxOne = false;
                }
                buf.append(name);
            }

            if (i != vals.length - 1) {
                buf.append(',');
            }
        }
        // }

        //log.debug("Got string: " + buf.toString());
        return buf.toString();
    }


} // End inner class CurrSel
