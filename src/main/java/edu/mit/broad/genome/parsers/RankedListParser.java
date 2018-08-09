/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.parsers;

import edu.mit.broad.genome.NamingConventions;
import edu.mit.broad.genome.alg.RankedListGenerators;
import edu.mit.broad.genome.math.Order;
import edu.mit.broad.genome.math.SortMode;
import edu.mit.broad.genome.objects.PersistentObject;
import edu.mit.broad.genome.objects.RankedList;
import gnu.trove.TFloatArrayList;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class RankedListParser extends AbstractParser {

    /**
     * Class Constructor.
     */
    public RankedListParser() {
        super(RankedList.class);
    }

    /**
     * Export a Dataset to file in gct format
     * Only works with Datasets
     *
     * @see "Above for format"
     */
    public void export(PersistentObject pob, File file) throws Exception {

        RankedList rl = (RankedList) pob;
        PrintWriter pw = startExport(pob, file);

        for (int i = 0; i < rl.getSize(); i++) {
            //System.out.println(">>>> " + m.getMember(i));
            pw.println(rl.getRankName(i) + "\t" + rl.getScore(i));
        }

        pw.close();

        doneExport();
    }    // End export

    /**
     * @returns 1 Dataset object
     * Always DatasetAnnotation object produced , but if underlying df has none then na is used
     * @see above for format
     */
    public List parse(String sourcepath, InputStream is) throws Exception {
        startImport(sourcepath);

        BufferedReader bin = new BufferedReader(new InputStreamReader(is));
        return _parse(sourcepath, bin);

    }

    /// does the real parsing
    // expects the bin to be untouched
    private List _parse(String objname, BufferedReader buf) throws Exception {

        String currLine = nextLine(buf);
        objname = NamingConventions.removeExtension(new File(objname).getName());

        List names = new ArrayList();
        TFloatArrayList floats = new TFloatArrayList();
        int cnt = 0;


        while (currLine != null) {

            String[] fields = ParseUtils.string2strings(currLine, "\t", false); // DONT USE SPACES
            if (fields.length != 2) {
                throw new ParserException("Bad rnk file format exception - expected 2 fields but got: " + fields.length + " line>" + currLine + "<");
                //floats.add(cnt++);
            }

            boolean doParse = true;


            if (fields[0].equalsIgnoreCase("Name") || fields[1].equalsIgnoreCase("Rank")) {
                doParse = false;
            }

            if (cnt == 0) { // @note sometimes the first line is a header -- ignore that error
                try {
                    Float.parseFloat(fields[1]);
                } catch (Throwable t) {
                    doParse = false; // skip line on error
                } finally {
                    cnt++;
                }
            }

            if (doParse) {

                names.add(fields[0]);
                floats.add(Float.parseFloat(fields[1]));
            }

            currLine = nextLine(buf);
        }

        //klog.debug("### of lines: " + names.size() + " from objname: " + objname);

        buf.close();

        doneImport();

        // changed march 2006 for the sorting
        RankedList rl = RankedListGenerators.createBySorting(objname, (String[]) names.toArray(new String[names.size()]), floats.toNativeArray(), SortMode.REAL, Order.DESCENDING);

        return unmodlist(rl);
        // return unmodlist(new DefaultRankedList(objname, (String[]) names.toArray(new String[names.size()]), floats));
    }


}    // End of class RankedListParser
