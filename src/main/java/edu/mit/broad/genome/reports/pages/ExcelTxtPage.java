/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.reports.pages;

import edu.mit.broad.genome.NotImplementedException;
import edu.mit.broad.genome.objects.Dataframe;
import edu.mit.broad.genome.objects.IDataframe;
import edu.mit.broad.genome.objects.StringDataframe;
import edu.mit.broad.genome.parsers.DataframeParser;
import edu.mit.broad.genome.parsers.StringDataframeParser;
import edu.mit.broad.genome.reports.RichDataframe;

import java.io.OutputStream;

/**
 * A simple txt representation of a spreadsheet format (i.e tab delimited)
 */
public class ExcelTxtPage implements Page {
    private String fName;
    private IDataframe fIdf;

    /**
     * Class constructor
     *
     * @param idf
     */
    public ExcelTxtPage(final String name, final IDataframe idf) {
        if (idf == null) {
            throw new IllegalArgumentException("Param idf cannot be null");
        }

        if (idf instanceof RichDataframe) {
            this.fIdf = ((RichDataframe) idf).getDataframe(); // @note hack
        } else {
            this.fIdf = idf;
        }

        this.fName = name;
    }

    public String getName() {
        return fName;
    }

    public String getExt() {
        return "tsv";
    }

    // @maint add more support and cleanup interface later
    public void write(OutputStream os) throws Exception {
        if (fIdf instanceof StringDataframe) {
            StringDataframeParser parser = new StringDataframeParser();
            parser.setSilentMode(true);
            parser.export(fIdf, os);
        } else if (fIdf instanceof Dataframe) {
            DataframeParser parser = new DataframeParser();
            parser.setSilentMode(true);
            parser.export(fIdf, os);
        } else {
            throw new NotImplementedException("idf object: " + fIdf);
        }
    }
}