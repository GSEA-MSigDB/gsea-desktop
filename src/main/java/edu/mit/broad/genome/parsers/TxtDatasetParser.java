/*
 * Copyright (c) 2003-2022 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.parsers;

import edu.mit.broad.genome.Constants;
import edu.mit.broad.genome.NamingConventions;
import edu.mit.broad.genome.objects.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple dataset format with no res, gct cdt etc frills. Description field is optional.
 * TXT Dataset format -> no obvious extension
 * (txt is used for a lot of different things)
 *
 * @author Aravind Subramanian
 * @author David Eby
 */
public class TxtDatasetParser extends AbstractParser {
    public TxtDatasetParser() { super(Dataset.class); }

    /**
     * Only accepts Dataset objects.
     * see below for format produced
     */
    public void export(PersistentObject pob, File file) throws Exception {
        try {
            PrintWriter pw = startExport(pob, file);
            Dataset ds = (Dataset) pob;
            FeatureAnnot ann = ds.getAnnot().getFeatureAnnot();
    
            pw.print(Constants.NAME);
            pw.print('\t');
            pw.print(Constants.DESCRIPTION);
    
            for (int i = 0; i < ds.getNumCol(); i++) {
                pw.print('\t');
                pw.print(ds.getColumnName(i));
            }
            pw.println();
    
            for (int r = 0; r < ds.getNumRow(); r++) {
                String featname = ds.getRowName(r);
                pw.print(featname);
                pw.print('\t');
                String desc = ann.getNativeDesc(featname);
                if (desc != null) {
                    pw.print(desc);
                } else {
                    pw.print(Constants.NA);
                }
                pw.print('\t');
                pw.println(ds.getRow(r).toString('\t'));
            }
        } finally {
            doneExport();  // Handles pw.close()
        }
    }

    /**
     * Expects a TXT data format compatible store (no specific extension known)
     * Produces a Dataset and a DatasetAnnotation object.
     * <p/>
     * BASIC TXT FORMAT (DESCRIPTION field is optional -> auto detected)
     * <p/>
     * NAME       DESCRIPTION	spo0	spo30	spo2	spo5	spo7	spo9	spo11
     * YAL003W	    some	    0.23	-1.79	-1.29	-1.56	-2.12	-1.09	-1.12
     * YAL004W	    some        0.41	-0.38	-0.89	-1.06	-1.6	-1.84	-1.6
     * YAL005C	    some	    0.61	-0.07	-1.29	-1.29	-2	    -1.84	-2.25
     */

    public List parse(String sourcepath, InputStream is) throws Exception {
        startImport(sourcepath);

        try (BufferedReader bin = new BufferedReader(new InputStreamReader(is))) {
            String objName = NamingConventions.removeExtension(sourcepath);
            String currLine = nextLine(bin);
    
            // 1st  non-empty, non-comment line are the column names
            List<String> colnames = ParseUtils.string2stringsList(currLine, "\t"); // colnames can have spaces
    
            colnames.remove(0);                                 // first elem is always nonsense
    
            boolean hasDesc = false;
            String possibleDescToken = colnames.get(0).toString();
            if (possibleDescToken.equalsIgnoreCase(Constants.DESCRIPTION) || possibleDescToken.equalsIgnoreCase("DESC")) {
                colnames.remove(0);
                hasDesc = true;
            }
    
            log.debug("HAS DESC: {}", hasDesc);
    
            // At this point, currLine should contain the first data line
            // data line: <row name> <tab> <ex1> <tab> <ex2> <tab>
            List<String> lines = new ArrayList<String>();
    
            currLine = nextLineTrimless(bin);
    
            // save all rows so that we can determine how many rows exist
            while (currLine != null) {
                lines.add(currLine);
                //currLine = nextLine(bin);
                currLine = nextLineTrimless(bin); /// imp for mv datasets -> last col(s) can be a tab
            }
            return parseTextMatrixToDataset(objName, lines, colnames, hasDesc);
        }
    }
}
