/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.parsers;

import edu.mit.broad.genome.objects.PersistentObject;
import edu.mit.broad.genome.reports.api.Report;
import xtools.api.DefaultReport;

import java.io.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

/**
 * Parses a Report
 * Format:
 * <p/>
 * file\tfo/bar/path
 * file\tfoo/bar/path
 * ...
 * param\tname\tvalue
 * param\tname\tvalue
 * ...
 */
public class ReportParser extends AbstractParser {

    /**
     * Class Constructor.
     */
    public ReportParser() {
        super(Report.class);
    }

    /**
     * Only files and params are expoer
     *
     * @see "Above for format"
     */
    public void export(PersistentObject pob, File file) throws Exception {

        PrintWriter pw = startExport(pob, file);

        Report rep = (Report) pob;

        StringBuffer buf = new StringBuffer();
        buf.append(Report.PRODUCER_CLASS_ENTRY).append('\t').append(rep.getProducer().getName()).append('\n');
        buf.append(Report.TIMESTAMP_ENTRY).append('\t').append(rep.getTimestamp()).append('\n');

        Properties prp = rep.getParametersUsed();
        Enumeration en = prp.keys();
        while (en.hasMoreElements()) {
            String key = en.nextElement().toString();
            String val = prp.getProperty(key);
            buf.append(Report.PARAM_ENTRY).append('\t').append(key).append('\t').append(val).append('\n');
        }

        buf.append('\n');

        File[] files = rep.getFilesProduced();
        for (int f = 0; f < files.length; f++) {
            buf.append(Report.FILE_ENTRY).append('\t').append(files[f].getPath()).append('\n');
        }

        pw.print(buf.toString());

        pw.close();
        doneExport();

    }    // End export


    /**
     * @returns 1 Report object
     * NO ann buiisness
     * @see above for format
     */
    public List parse(final String sourcepath, final InputStream is) throws Exception {

        BufferedReader bin = new BufferedReader(new InputStreamReader(is));
        String currLine = nextLine(bin);

        List filesList = new ArrayList();
        Properties params = new Properties();
        Class cl = null;
        long ts = 0; // beginning of time if no ts info available

        while (currLine != null) {

            String[] fields = ParseUtils.string2strings(currLine, "\t", false); // no spaces -- valid for file names!

            if ((fields.length != 2) && (fields.length != 3)) {
                throw new ParserException("Bad Report format -- expect 2 or 3, found: " + fields.length + " line: " + currLine);
            }

            if (fields[0].equalsIgnoreCase(Report.PRODUCER_CLASS_ENTRY)) {
                if (fields.length != 2) {
                    throw new ParserException(">2 fields for " + Report.PRODUCER_CLASS_ENTRY + " line: " + currLine);
                }

                cl = Class.forName(fields[1]);
            }

            if (fields[0].equalsIgnoreCase(Report.TIMESTAMP_ENTRY)) {
                if (fields.length != 2) {
                    throw new ParserException(">2 fields for " + Report.TIMESTAMP_ENTRY + " line: " + currLine);
                }

                ts = Long.parseLong(fields[1]);
            }

            if (fields[0].equalsIgnoreCase(Report.FILE_ENTRY)) {
                if (fields.length != 2) {
                    throw new ParserException(">2 fields for " + Report.FILE_ENTRY + " line: " + currLine);
                }
                filesList.add(new File(fields[1]));
            } else if (fields[0].equalsIgnoreCase(Report.PARAM_ENTRY)) {
                if (fields.length != 3) {
                    throw new ParserException("Insufficient fields for " + Report.PARAM_ENTRY + " line: " + currLine);
                }
                params.put(fields[1], fields[2]);
            }

            currLine = nextLine(bin);
        }

        bin.close();

        File[] files = (File[]) filesList.toArray(new File[filesList.size()]);

        Report report = new DefaultReport(sourcepath, ts, cl, files, params, false);

        return unmodlist(report);

    }


}    // End of class ReportParser
