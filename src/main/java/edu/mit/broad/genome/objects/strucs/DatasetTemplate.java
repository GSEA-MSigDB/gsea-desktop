/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.objects.strucs;

import edu.mit.broad.genome.NamingConventions;
import edu.mit.broad.genome.alg.DatasetGenerators;
import edu.mit.broad.genome.objects.Dataset;
import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.objects.Template;
import edu.mit.broad.genome.parsers.ParserFactory;

import java.io.File;

/**
 * @author Aravind Subramanian
 */
public class DatasetTemplate {

    private Dataset fDataset;
    private Template fTemplate;

    private File fDatasetFile;
    private File fTemplateFile;
    private boolean fAdd2Cache;

    private GeneSet fGeneSpace;
    private File fGeneSpaceFile;

    /**
     * Class constructor
     *
     * @param ds
     * @param t
     */
    public DatasetTemplate(final Dataset ds, final Template t) {

        if (ds == null) {
            throw new IllegalArgumentException("Param ds cannot be null");
        }

        if (t == null) {
            throw new IllegalArgumentException("Param t cannot be null");
        }

        this.fDataset = ds;
        this.fTemplate = t;
    }

    public String getTemplateName() {
        if (fTemplate != null) {
            return fTemplate.getName();
        } else {
            return NamingConventions.removeExtension(fTemplateFile.getName());
        }
    }

    public Dataset getDataset(boolean add2cache) {

        if (fDataset == null) {
            try {
                this.fDataset = ParserFactory.readDataset(fDatasetFile, add2cache, add2cache);
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }

        if (fGeneSpace != null) {
            return new DatasetGenerators().extractRows(fDataset, fGeneSpace);
        } else if (fGeneSpaceFile != null) {
            try {
                this.fGeneSpace = ParserFactory.readGeneSet(fGeneSpaceFile, false);
                return new DatasetGenerators().extractRows(fDataset, fGeneSpace);
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        } else {
            return fDataset;
        }
    }

    public Dataset getDataset() {
        return getDataset(fAdd2Cache);
    }

    public Template getTemplate() {
        if (fTemplate == null) {
            try {
                this.fTemplate = ParserFactory.readTemplate(fTemplateFile, fAdd2Cache, fAdd2Cache);
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }

        return fTemplate;
    }

} // End class DatasetTemplate


