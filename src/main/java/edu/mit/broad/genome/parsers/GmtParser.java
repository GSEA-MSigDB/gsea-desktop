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
import java.util.StringTokenizer;

import org.apache.commons.lang3.StringUtils;

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
 * @author Aravind Subramanian, David Eby
 */
public class GmtParser extends AbstractParser {
    public GmtParser() { super(GeneSetMatrix.class); }

    /**
     * Only accepts GeneSetMatrix
     */
    public void export(PersistentObject gmpob, File file) throws Exception {
        try (final PrintWriter pw = startExport(gmpob, file)) {
            final GeneSetMatrix gm = (GeneSetMatrix) gmpob;

            for (int i = 0; i < gm.getNumGeneSets(); i++) {
                GeneSet gset = gm.getGeneSet(i);
                StringBuilder buf = new StringBuilder(gset.getName()).append('\t');
                String ne = gset.getNameEnglish();
                if (isNullorNa(ne)) { ne = Constants.NA; }
                buf.append(ne).append('\t');

                for (int f = 0; f < gset.getNumMembers(); f++) {
                    buf.append(gset.getMember(f));
                    if (f < gset.getNumMembers() - 1) { buf.append('\t'); }
                }
                buf.append('\n');
                pw.print(buf.toString());
            }
            doneExport();
        }
    }

    /**
     * Parses in a GeneSetMatrix files.
     * First col are fset names
     * second col is assumed to be colors
     * third line onwards gene names data -- need NOT be equal number of cols
     */
    public List parse(String sourcepath, InputStream is) throws Exception {
        startImport(sourcepath);
        MSigDBVersion msigDBVersion;
        String pathLC = sourcepath.toLowerCase();
        if (StringUtils.containsAny(pathLC, "ftp.broadinstitute.org", "data.broadinstitute.org",
                "data.gsea-msigdb.org", "datasets.genepattern.org")) {
            // Create a version object and assign it to the GeneSetMatrix.  We can only safely track
            // the version of files that we know have been downloaded in the session, at least for now.
            String versionStr = NamingConventions.extractVersionFromFileName(sourcepath, ".symbols.gmt");
            // We make an assumption here that any non-Mouse GMT from the our servers is Human.
            // This is valid for now.
            MSigDBSpecies msigDBSpecies = (versionStr.contains("Mm")) ? MSigDBSpecies.Mouse : MSigDBSpecies.Human;
            msigDBVersion = new MSigDBVersion(msigDBSpecies, versionStr);
        } else {
            msigDBVersion = MSigDBVersion.createUnknownTrackingVersion(sourcepath);
        }

        String fileName = new File(sourcepath).getName();
        try (final BufferedReader bin = new BufferedReader(new InputStreamReader(is))) {
            String currLine = nextLine(bin);

            int row = 0;
            final List<GeneSet> gsets = new ArrayList<GeneSet>();

            while (currLine != null) {
                StringTokenizer tok = new StringTokenizer(currLine, "\t"); // dont split on whitespace??
                int cnt = tok.countTokens();
                if (cnt <= 1) { throw new ParserException("Empty gene line: " + currLine + " at row: " + row); }

                // TODO: is it really necessary to force Gene Set names to uppercase?
                String gsetName = tok.nextToken().trim().toUpperCase(); // @note the UC'ing
                String gsetname_english = tok.nextToken().trim();
                List<String> geneNames = new ArrayList<String>();

                while (tok.hasMoreTokens()) {
                    String geneName = tok.nextToken().trim();

                    // dont really expect null, but for consistency
                    if (!isNull(geneName)) { geneNames.add(geneName); }
                }

                //@note convention
                String fname = fileName.concat("#").concat(gsetName);
                GeneSet gset = new GeneSet(fname, gsetname_english, geneNames, true, msigDBVersion);

                gsets.add(gset);
                row++;
                currLine = nextLine(bin);
            }

            DefaultGeneSetMatrix geneSetMatrix = new DefaultGeneSetMatrix(fileName, gsets, msigDBVersion);
            return unmodlist(geneSetMatrix);
        } finally {
            doneImport();
        }
    }
}
