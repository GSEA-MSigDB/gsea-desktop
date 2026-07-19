/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.params;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.broad.genome.parsers.DataFormat;
import edu.mit.broad.genome.reports.api.Report;
import edu.mit.broad.genome.utils.DateUtils;
import edu.mit.broad.xbench.core.api.Application;
import edu.mit.broad.xbench.tui.ReportStub;

/**
 * Lists GSEA reports from the local report cache that contain an {@code edb/} folder.
 */
public final class FxReportCacheSupport {
    private static final Logger klog = LoggerFactory.getLogger(FxReportCacheSupport.class);

    public static final class CachedReport {
        public final Report report;
        public final File reportDir;
        public final File edbDir;
        public final String displayName;

        CachedReport(Report report, File reportDir, File edbDir) {
            this.report = report;
            this.reportDir = reportDir;
            this.edbDir = edbDir;
            this.displayName = report.getName() + " [" + DateUtils.formatAsDayMonthYear(report.getDate()) + "]";
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private FxReportCacheSupport() {
    }

    public static List<CachedReport> listGseaReportsWithEdb() {
        List<CachedReport> out = new ArrayList<>();
        File dir;
        try {
            dir = Application.getVdbManager().getReportsCacheDir();
        } catch (Throwable t) {
            klog.warn("No report cache dir", t);
            return out;
        }
        if (dir == null || !dir.exists()) {
            return out;
        }
        File[] files = dir.listFiles(DataFormat.RPT_FORMAT.getFilenameFilter());
        if (files == null) {
            return out;
        }
        for (File file : files) {
            try {
                ReportStub stub = new ReportStub(file);
                if (stub.getName().indexOf("Gsea") < 0) {
                    continue;
                }
                Report report = stub.getReport(false);
                File rptDir = report.getReportDir();
                File edbDir = new File(rptDir, "edb");
                if (edbDir.exists()) {
                    out.add(new CachedReport(report, rptDir, edbDir));
                }
            } catch (Throwable t) {
                klog.debug("Skip report cache entry {}: {}", file, t.toString());
            }
        }
        out.sort(Comparator.comparing((CachedReport c) -> c.report.getDate()).reversed());
        return out;
    }
}
