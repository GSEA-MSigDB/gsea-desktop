/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.tui;

import java.io.File;
import java.util.Properties;
import java.util.function.Consumer;

import org.gsea_msigdb.gsea.ui.api.ViewPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.broad.genome.reports.api.Report;
import edu.mit.broad.xbench.core.api.Application;
import edu.mit.broad.xbench.tui.TaskManager;
import javafx.application.Platform;
import xapps.gsea.fx.FxProgressMonitorRead;
import xtools.api.Tool;
import xtools.api.param.ParamSet;

/**
 * Relaunch a tool from a past report's parameters,
 * or from a saved {@link ParamSet} (FX process table Name-column click).
 */
public final class FxToolRelaunch {
    private static final Logger klog = LoggerFactory.getLogger(FxToolRelaunch.class);

    private FxToolRelaunch() {
    }

    public static void showInToolRunner(Report report, boolean loadData, Consumer<ViewPage> openPage) {
        if (report == null) {
            Application.getWindowManager().showError("No report selected");
            return;
        }
        if (openPage == null) {
            Application.getWindowManager().showError("No workspace available to open the tool");
            return;
        }
        javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() throws Exception {
                Class<?> producer = report.getProducer();
                if (producer == null) {
                    throw new IllegalStateException("Report has no producer tool class");
                }
                Tool tool = TaskManager.createTool(producer.getName());
                Properties params = report.getParametersUsed();
                relaunch(tool, params, report.getName(), loadData, true, openPage);
                return null;
            }
        };
        task.setOnFailed(e -> {
            Throwable t = task.getException();
            klog.error("Show in ToolRunner failed", t);
            Application.getWindowManager().showError("Could not open tool from report", t);
        });
        xapps.gsea.fx.FxWorkers.start(task, "gsea-tool-relaunch");
    }

    /** Process-table Name column.getName}. */
    public static void showInToolRunner(Tool sourceTool, Properties paramSnapshot, Consumer<ViewPage> openPage) {
        if (sourceTool == null || paramSnapshot == null) {
            Application.getWindowManager().showError("No saved parameters available for this run");
            return;
        }
        if (openPage == null) {
            Application.getWindowManager().showError("No workspace available to open the tool");
            return;
        }
        javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() throws Exception {
                Tool tool = TaskManager.createTool(sourceTool.getClass().getName());
                relaunch(tool, paramSnapshot, null, false, false, openPage);
                return null;
            }
        };
        task.setOnFailed(e -> {
            Throwable t = task.getException();
            klog.error("Show in ToolRunner failed", t);
            Application.getWindowManager().showError("Could not reopen tool with saved parameters", t);
        });
        xapps.gsea.fx.FxWorkers.start(task, "gsea-tool-relaunch");
    }

    /**
     * @param tabTitleOpt custom tab title (report name), or null → {@code tool.getName}
     * @param showToast toast vs silent process-table relaunch
     */
    private static void relaunch(Tool tool, Properties params, String tabTitleOpt, boolean loadData,
            boolean showToast, Consumer<ViewPage> openPage) throws Exception {
        ParamSet.FoundMissingFile fmf = tool.getParamSet().fileCheckingFill(params);

        StringBuilder missing = new StringBuilder();
        if (fmf.missingFiles != null) {
            for (File f : fmf.missingFiles) {
                if (f != null) {
                    missing.append(f.getAbsolutePath()).append('\n');
                }
            }
        }

        final String missingText = missing.toString();
        if (!missingText.isEmpty()) {
            boolean proceed = Application.getWindowManager().showConfirm("Some Files Missing",
                    "Some parameter files were not found:\n" + missingText
                            + "\n\nContinue opening the tool with available parameters?");
            if (!proceed) {
                return;
            }
        }

        if (loadData && fmf.foundFiles != null) {
            StringBuilder errs = new StringBuilder();
            for (File f : fmf.foundFiles) {
                if (f == null || !f.isFile()) {
                    continue;
                }
                try {
                    klog.debug("Loading file from report params: {}", f);
                    FxProgressMonitorRead.read(f);
                } catch (Throwable t) {
                    klog.warn("Could not load {}", f, t);
                    errs.append(t.getMessage()).append('\n');
                }
            }
            if (errs.length() > 0) {
                Platform.runLater(() -> Application.getWindowManager().showMessage(
                        "Load warnings", "Some files could not be loaded:\n" + errs));
            }
        }

        final String tabTitle = tabTitleOpt != null ? tabTitleOpt : tool.getName();
        Platform.runLater(() -> {
            openPage.accept(FxToolLauncherPane.forTool(tool, tabTitle));
            if (showToast) {
                Application.getWindowManager().showMessage(
                        "Created a new ToolRunner with parameters from the earlier run. "
                                + "Data files (when found) were automagically imported");
            }
        });
    }
}
