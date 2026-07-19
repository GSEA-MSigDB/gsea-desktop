/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Consumer;

import org.gsea_msigdb.gsea.ui.api.ViewPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.broad.genome.reports.api.Report;
import edu.mit.broad.xbench.core.api.Application;
import edu.mit.broad.xbench.tui.ReportStub;
import edu.mit.broad.xbench.tui.TaskManager;
import xapps.gsea.fx.viewers.FxReportViewer;

/**
 * Shared report opening for the process table, home, and analysis history.
 * All report kinds open {@link FxReportViewer}; Results content is kind-specific
 * via {@link xapps.gsea.fx.viewers.report.ReportExplorerRegistry}.
 */
public final class FxReportOpen {

    private static final Logger klog = LoggerFactory.getLogger(FxReportOpen.class);

    private FxReportOpen() {
    }

    /** In-app report viewer (Results / Parameters / Files) for any finished analysis. */
    public static ViewPage viewPageFor(Report report, Consumer<ViewPage> openPage) {
        return new FxReportViewer(report, openPage);
    }

    public static void openInApp(Report report, Consumer<ViewPage> openPage) {
        if (report == null || openPage == null) {
            return;
        }
        openPage.accept(viewPageFor(report, openPage));
    }

    public static void openInBrowser(Report report) {
        if (report == null) {
            Application.getWindowManager().showMessage("No report produced");
            return;
        }
        try {
            URI index = report.getReportIndex();
            if (index == null) {
                Application.getWindowManager().showMessage("No report produced");
                return;
            }
            FxDesktopUtil.openUri(index);
        } catch (Exception e) {
            Application.getWindowManager().showError("Could not open report", e);
        }
    }

    /**
     * Process-table path: prefer in-app viewer when an {@code .rpt} can be loaded;
     * otherwise open the HTML index in the browser.
     */
    public static void openFromRun(String runId, File reportDir, Consumer<ViewPage> openPage) {
        try {
            File dir = reportDir != null ? reportDir : TaskManager.getInstance().getReportDir(runId);
            Report report = tryLoadReport(dir);
            if (report != null && openPage != null) {
                openInApp(report, openPage);
                return;
            }
            URI index = TaskManager.getInstance().getReportIndex(runId);
            if (index != null) {
                FxDesktopUtil.openUri(index);
                return;
            }
            Application.getWindowManager().showMessage("No report produced");
        } catch (Exception e) {
            Application.getWindowManager().showError("Could not open report", e);
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
