/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.parsers;

import edu.mit.broad.genome.objects.FSet;
import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.objects.GeneSetMatrix;
import edu.mit.broad.genome.objects.PersistentObject;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses a data store in FSet format to produce a single FSet object.
 * <p/>
 * Format Supported:
 * <p/>
 * Type I: One FSet file -> 1 FSet object
 * <br><pre>
 * feature_name_1
 * feature_name_1
 * feature_name_1
 * ...
 * <pre>
 * <br>
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class FSetParser extends AbstractParser {

    /**
     * Class Constructor.
     */
    public FSetParser() {
        super(GeneSet.class);
    }

    /**
     * Only accepts FSet objects OR a GeneSetMatrix object
     */
    public void export(PersistentObject gsetorgm, File file) throws Exception {


        if (gsetorgm instanceof GeneSetMatrix) {

            GeneSetMatrix gm = (GeneSetMatrix) gsetorgm;
            File baseFile = file.getParentFile();
            for (int i = 0; i < gm.getNumGeneSets(); i++) {
                String name = AuxUtils.getAuxNameOnlyNoHash(gm.getGeneSet(i).getName());
                _export(gm.getGeneSet(i), new File(baseFile, name));
            }

        } else {
            _export((GeneSet) gsetorgm, file);
        }

        doneExport();

    }

    private void _export(GeneSet gset, File file) throws Exception {
        PrintWriter pw = startExport(gset, file);
        for (int i = 0; i < gset.getNumMembers(); i++) {
            //System.out.println(">>>> " + m.getMember(i));
            pw.println(gset.getMember(i));
        }

        pw.close();

    }

    /**
     * Parses in FSet files.
     */
    public List parse(String sourcepath, InputStream is) throws Exception {
        startImport(sourcepath);
        FSet fset = parse(sourcepath, new BufferedReader(new InputStreamReader(is)));
        doneImport();
        return unmodlist(fset);

    }    // End parse()

    protected FSet parse(String sourcepath, BufferedReader buf) throws IOException {
        String currLine = nextLine(buf);

        List lines = new ArrayList();
        while (currLine != null) {
            lines.add(currLine);
            currLine = nextLine(buf);
        }


        buf.close();

        final String[] members = (String[]) lines.toArray(new String[lines.size()]);

        // assume no desc for fset
        return new FSet(sourcepath, null, members);
    }

}    // End FSetParser
