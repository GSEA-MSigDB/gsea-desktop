/*
 * Copyright (c) 2003-2021 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
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
    public RankedListParser() { super(RankedList.class); }

    /**
     * Export a Dataset to file in RNK format
     * Only works with Datasets
     *
     * @see "Above for format"
     */
    public void export(PersistentObject pob, File file) throws Exception {
        RankedList rl = (RankedList) pob;
        try (PrintWriter pw = startExport(pob, file)) {
            final int size = rl.getSize();
            for (int i = 0; i < size; i++) {
                pw.print(rl.getRankName(i));
                pw.print("\t");
                pw.println(rl.getScore(i));
            }
        } finally {
            doneExport();
        }
    }

    /**
     * @returns 1 Dataset object
     * Always DatasetAnnotation object produced , but if underlying df has none then na is used
     * @see above for format
     */
    public List parse(String sourcepath, InputStream is) throws Exception {
        startImport(sourcepath);

        try (BufferedReader bin = new BufferedReader(new InputStreamReader(is))) {
            String objname = NamingConventions.removeExtension(new File(sourcepath).getName());
    
            List<String> names = new ArrayList<String>();
            // TODO: remove trove
            TFloatArrayList floats = new TFloatArrayList();
            int skippedMissingRows = 0;
            boolean foundInfiniteValues = false;

            int row = 0;
            String currLine = nextLine(bin);
            if (currLine == null) { throw new ParserException("RNK file was empty!"); }
            
            // Check if the first line is a header; if so, skip it.
            List<String> fields = string2stringsV2(currLine, 2);
            if (fields.get(0).equalsIgnoreCase("Name") || fields.get(1).equalsIgnoreCase("Rank")) {
                currLine = nextLine(bin);
            }
            
            while (currLine != null) {
                fields = string2stringsV2(currLine, 2);
                String name = parseRowname(fields.get(0).trim(), row++);
                try {
                    float value = parseStringToFloat(fields.get(1), true);
                    if (! Float.isNaN(value)) {
                        names.add(name);
                        floats.add(value);
                        if (Float.isInfinite(value)) {
                        	foundInfiniteValues = true;
                            log.warn("Infinite values found in row " + (row+1) + " of the data matrix with Name '" + name + "'.");
                        }
                    } else {
                        skippedMissingRows++;
                        log.warn("Missing value found in row " + (row+1) + " of the data matrix with Name '" + name + "'.");
                    }
                } catch (NumberFormatException nfe) {
                    log.error("Could not parse '" + fields.get(1) + "' as a floating point number in row " + (row+1) + " of the data matrix with Name '" + name + "'.");
                    throw nfe;
                }
    
                currLine = nextLine(bin);
            }
            if (floats.isEmpty()) { throw new ParserException("Data was missing in all rows!"); }
    
            doneImport();
    
            // changed march 2006 for the sorting
            RankedList rl = RankedListGenerators.createBySorting(objname, names.toArray(new String[names.size()]), floats.toNativeArray(), SortMode.REAL, Order.DESCENDING);
            if (foundInfiniteValues) {
                String warning = "Infinite values detected in this RNK file. This may cause unexpected results in the calculations or failures in plotting.";
                log.warn(warning);
                rl.addWarning(warning + "  See the log for more details.");
            }
            if (skippedMissingRows > 0) {
                String warning = "There were " + skippedMissingRows + " row(s) in total of missing data in this RNK file.  These will be ignored.";
                log.warn(warning);
                rl.addWarning(warning + "  See the log for more details.");
            }
    
            return unmodlist(rl);
        }
    }
}
