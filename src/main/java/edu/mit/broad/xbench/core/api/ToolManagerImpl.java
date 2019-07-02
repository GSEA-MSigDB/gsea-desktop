/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.xbench.core.api;

import edu.mit.broad.genome.NamingConventions;
import edu.mit.broad.genome.parsers.DataFormat;
import edu.mit.broad.genome.utils.ClassUtils;
import edu.mit.broad.xbench.prefs.XPreferencesFactory;
import edu.mit.broad.xbench.tui.ReportStub;

import org.apache.log4j.Logger;

import xtools.api.Tool;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Aravind Subramanian
 */
public class ToolManagerImpl implements ToolManager {

    private static final Logger klog = Logger.getLogger(ToolManagerImpl.class);

    // both for opt and because we dont want to pick up this instances report stub
    private static Map kToolNameReporStubMap;

    /**
     * Class constructor
     */
    public ToolManagerImpl() {

    }

    public String getLastToolName() {
        return XPreferencesFactory.kLastToolName.getString();
    }

    public void setLastToolName(final Tool tool) {
        XPreferencesFactory.kLastToolName.setValue(tool.getClass().getName());
    }

    // Might return null if none
    public ReportStub getLastReportStub(String toolName) {
        if (kToolNameReporStubMap == null) {
            kToolNameReporStubMap = new HashMap();
        }

        toolName = ClassUtils.shorten(toolName);

        if (kToolNameReporStubMap.containsKey(toolName)) {
            return (ReportStub) kToolNameReporStubMap.get(toolName);
        }

        final File[] files = _getReportFiles();
        File lastFile = null;
        for (int i = 0; i < files.length; i++) {
            try {
                String name = NamingConventions.removeExtension(files[i]);
                if (name.indexOf(toolName) != -1) { // dont check for exact match as the rpt has prefixes, time stamps ect
                    if (lastFile == null) {
                        lastFile = files[i];
                    } else if (lastFile.lastModified() < files[i].lastModified()) {
                        lastFile = files[i];
                    }
                }
            } catch (Throwable t) {
                // silently suppress
            }
        }

        if (lastFile != null) {
            // dont! we want to lazily load
            //Report rpt = ParserFactory.readReport(files[i], true);
            ReportStub rs = new ReportStub(lastFile);
            kToolNameReporStubMap.put(toolName, rs);
            return rs;
        } else {
            return null;
        }
    }

    public ReportStub[] getReportsInCache() {
        final File[] files = _getReportFiles();
        List reports = new ArrayList();
        for (int i = 0; i < files.length; i++) {
            //klog.debug("Found reports: " + files[i]);
            try {
                // dont! we want to lazily load
                //Report rpt = ParserFactory.readReport(files[i], true);
                ReportStub stub = new ReportStub(files[i]);
                reports.add(stub);
            } catch (Throwable t) {
                // silently suppress
            }
        }

        return (ReportStub[]) reports.toArray(new ReportStub[reports.size()]);
    }

    private File[] _getReportFiles() {
        final File dir = Application.getVdbManager().getReportsCacheDir();

        if (dir.exists() == false) {
            klog.warn("Report cache: " + dir + " not found");
            return new File[]{};
        }

        return dir.listFiles(DataFormat.RPT_FORMAT.getFilenameFilter());
        //klog.info("Report cache: " + dir + " and # found: " + files.length);
        //return files;
    }

} // End class ToolManagerImpl
