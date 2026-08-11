/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers.report;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

import org.gsea_msigdb.gsea.ui.api.FeatureHost;
import org.gsea_msigdb.gsea.ui.api.ViewPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.broad.genome.reports.api.Report;
import edu.mit.broad.xbench.tui.ReportStub;
import xapps.gsea.fx.FxDesktopUtil;
import xapps.gsea.fx.jobs.JobRecord;
import xapps.gsea.fx.viewers.FxReportViewer;
import org.gsea_msigdb.gsea.runtime.AppServices;

/**
 * Shared report opening for the Jobs panel, home, and analysis history.
 * All report kinds open {@link FxReportViewer}; Results content is kind-specific
 * via {@link xapps.gsea.fx.viewers.report.ReportExplorerRegistry}.
 */
public final class FxReportOpen {

    private static final Logger klog = LoggerFactory.getLogger(FxReportOpen.class);

    private FxReportOpen() {
    }

    /** In-app report viewer (Results / Parameters / Files) for any finished analysis. */
    public static ViewPage viewPageFor(Report report, FeatureHost host) {
        return new FxReportViewer(report, Objects.requireNonNull(host, "host"));
    }

    public static void openInApp(Report report, FeatureHost host) {
        if (report == null || host == null) {
            return;
        }
        host.openPage(viewPageFor(report, host));
    }

    public static void openInBrowser(Report report) {
        if (report == null) {
            AppServices.require().dialogs().showMessage("No report produced");
            return;
        }
        try {
            URI index = report.getReportIndex();
            if (index == null) {
                AppServices.require().dialogs().showMessage("No report produced");
                return;
            }
            FxDesktopUtil.openUri(index);
        } catch (Exception e) {
            AppServices.require().dialogs().showError("Could not open report", e);
        }
    }

    /**
     * Jobs panel path: prefer in-app viewer when an {@code .rpt} can be loaded;
     * otherwise open the HTML index in the browser.
     */
    public static void openFromJob(JobRecord job, FeatureHost host) {
        if (job == null) {
            AppServices.require().dialogs().showMessage("No report produced");
            return;
        }
        Objects.requireNonNull(host, "host");
        try {
            Report report = tryLoadReport(job.getReportDir());
            if (report != null) {
                openInApp(report, host);
                return;
            }
            URI index = job.getReportIndex();
            if (index != null) {
                FxDesktopUtil.openUri(index);
                return;
            }
            AppServices.require().dialogs().showMessage("No report produced");
        } catch (Exception e) {
            AppServices.require().dialogs().showError("Could not open report", e);
        }
    }

    private static Report tryLoadReport(File reportDir) {
        if (reportDir == null || !reportDir.isDirectory()) {
            return null;
        }
        try {
            File[] files = reportDir.listFiles((d, name) -> name != null && name.toLowerCase().endsWith(".rpt"));
            if (files == null || files.length == 0) {
                return null;
            }
            Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
            return new ReportStub(files[0]).getReport(false);
        } catch (Throwable t) {
            klog.debug("Could not load Report from {}: {}", reportDir, t.toString());
            return null;
        }
    }
}
