/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.parsers;

import edu.mit.broad.genome.Constants;
import edu.mit.broad.genome.objects.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Parses a file in GeneSetMatrix format to produce a single GeneSetMatrix object and several
 * FSets
 * <p/>
 * Format Supported:
 * <p/>
 * fset_name color gene_a gene_b ...
 * fset_name color genea gene_e gene_t ...
 * <p/>
 * Need NOT be equal number of members in each row
 * Comments allowed as usual with the # sign
 * Color fields is NOT suppported (cant do that easily)
 * <pre>
 * <br>
 * <p/>
 * ...
 * <pre>
 * <br>
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class GmtParser extends AbstractParser {

    /**
     * Class Constructor.
     */
    public GmtParser() {
        super(GeneSetMatrix.class);
    }

    /**
     * Only accepts GeneSetMatrix
     */
    public void export(PersistentObject gmpob, File file) throws Exception {

        final PrintWriter pw = startExport(gmpob, file);

        final GeneSetMatrix gm = (GeneSetMatrix) gmpob;

        for (int i = 0; i < gm.getNumGeneSets(); i++) {
            GeneSet gset = gm.getGeneSet(i);

            StringBuffer buf = new StringBuffer(gset.getName()).append('\t');

            String ne = gset.getNameEnglish();
            if (isNullorNa(ne)) {
                ne = Constants.NA;
            }

            buf.append(ne).append('\t');

            for (int f = 0; f < gset.getNumMembers(); f++) {
                buf.append(gset.getMember(f));

                if (f < gset.getNumMembers() - 1) {
                    buf.append('\t');
                }
            }

            buf.append('\n');
            pw.print(buf.toString());
        }

        pw.close();

        doneExport();
    }

    /**
     * Parses in a GeneSetMatrix files.
     * First col are fset names
     * second col is assumed to be colors
     * third line onwards gene names data -- need NOT be equal number of cols
     */
    public List parse(String sourcepath, InputStream is) throws Exception {

        startImport(sourcepath);

        final BufferedReader bin = new BufferedReader(new InputStreamReader(is));
        String currLine = nextLine(bin);

        int row = 0;
        final List gsets = new ArrayList();

        while (currLine != null) {
            StringTokenizer tok = new StringTokenizer(currLine, "\t"); // dont split on whitespace??
            int cnt = tok.countTokens();

            if (cnt <= 1) {
                throw new ParserException("Empty gene line: " + currLine + " at row: " + row);
            }

            String gsetName = tok.nextToken().trim().toUpperCase(); // @note the UC'ing

            String gsetname_english = tok.nextToken().trim();

            List geneNames = new ArrayList();

            while (tok.hasMoreTokens()) {
                String geneName = tok.nextToken().trim();

                if (isNull(geneName)) {
                    continue;    // dont really expect, but for consistency
                } else {
                    geneNames.add(geneName);
                }
            }

            //@note convention
            String fname = sourcepath.concat("#").concat(gsetName);
            GeneSet gset = new GeneSet(fname, gsetname_english, geneNames, true);

            gsets.add(gset);

            row++;

            currLine = nextLine(bin);
        }

        bin.close();
        doneImport();

        return unmodlist(new DefaultGeneSetMatrix(sourcepath, gsets));

    }                                // End parse()

}    // End GmtParser
