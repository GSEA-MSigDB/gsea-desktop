/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.objects;

import edu.mit.broad.genome.parsers.AuxUtils;
import edu.mit.broad.genome.parsers.ParserFactory;

import java.io.File;

/**
 * @author Aravind Subramanian
 */
public class TemplateDerivatives {


    public static class ContTemplateDerivative implements TemplateDerivative {

        private String fAuxName;
        private String fJustName;
        private File fMainTemplateFile;

        // auxName is of the form foo#bar (and not just bar)
        public ContTemplateDerivative(final String auxName, final File mainTemplateFile) {
            if (auxName == null) {
                throw new IllegalArgumentException("Parameter auxName cannot be null");
            }
            if (mainTemplateFile == null) {
                throw new IllegalArgumentException("Parameter mainTemplateFile cannot be null");
            }

            this.fAuxName = auxName;
            this.fMainTemplateFile = mainTemplateFile;

            this.fJustName = AuxUtils.getAuxNameOnlyNoHash(fAuxName);
        }

        public String getName(final boolean parentNamePlusMyName, final boolean fullPath) {

            String ret;

            if (fullPath) {
                ret = fMainTemplateFile.getPath() + "#";
            } else if (parentNamePlusMyName) {
                ret = fMainTemplateFile.getName() + "#";
            } else {
                ret = "";
            }

            return ret + fJustName; // imp and not fAuxName as that has the template name on it too
        }

    } // End class AuxTemplateDerivative

    public static class PseudoTemplateDerivative implements TemplateDerivative {

        private Template fMainTemplate;

        // auxName is of the form foo#bar (and not just bar)
        public PseudoTemplateDerivative(final Template mainTemplate) {
            if (mainTemplate == null) {
                throw new IllegalArgumentException("Parameter mainTemplate cannot be null");
            }

            this.fMainTemplate = mainTemplate;
        }

        public String getName(final boolean parentNamePlusMyName, final boolean fullPath) {

            if (fullPath) {
                return ParserFactory.getCache().getSourcePath(fMainTemplate);
            } else {
                return fMainTemplate.getName();
            }

        }

    } // End class PseudoTemplateDerivative

    public static class AuxTemplateDerivative implements TemplateDerivative {

        private String fAuxName;
        private String fJustName;
        private Template fMainTemplate;

        // auxName is of the form foo#bar (and not just bar)
        public AuxTemplateDerivative(final String auxName, final Template mainTemplate) {
            if (auxName == null) {
                throw new IllegalArgumentException("Parameter auxName cannot be null");
            }
            if (mainTemplate == null) {
                throw new IllegalArgumentException("Parameter mainTemplate cannot be null");
            }

            this.fAuxName = auxName;
            this.fMainTemplate = mainTemplate;


            this.fJustName = AuxUtils.getAuxNameOnlyNoHash(fAuxName);
        }

        public String getName(final boolean parentNamePlusMyName, final boolean fullPath) {

            String ret;

            if (fullPath) {
                ret = ParserFactory.getCache().getSourcePath(fMainTemplate) + "#";
            } else if (parentNamePlusMyName) {
                ret = fMainTemplate.getName() + "#";
            } else {
                ret = "";
            }

            return ret + fJustName; // imp and not fAuxName as that has the template name on it too
        }

    } // End class AuxTemplateDerivative

}
