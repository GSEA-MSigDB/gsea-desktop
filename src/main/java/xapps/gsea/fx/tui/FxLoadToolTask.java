/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.tui;

import java.io.File;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.gsea_msigdb.gsea.runtime.AppServices;
import xapps.gsea.fx.widgets.FxProgressMonitorRead;
import xtools.api.Tool;
import xtools.api.param.ParamSet;

/**
 * Loads parameter files from a past run into the object cache.
 * Does not open a tool page or change the live form. Opening a tool is {@link FxToolRelaunch}.
 */
public final class FxLoadToolTask {
    private static final Logger klog = LoggerFactory.getLogger(FxLoadToolTask.class);

    private FxLoadToolTask() {
    }

    public static Runnable create(final Tool fillThisTool,
            final String rptName,
            final boolean loadFiles,
            final Properties sourceParams) {
        return () -> {
            try {
                ParamSet.FoundMissingFile fmf = fillThisTool.getParamSet().fileCheckingFill(sourceParams);
                if (fmf.missingFiles != null && fmf.missingFiles.length != 0) {
                    if (!confirmMissingFiles(fmf.missingFiles)) {
                        return;
                    }
                }

                StringBuilder errs = new StringBuilder("<html><body>There were parsing errors<pre>");
                boolean atleastoneerr = false;
                if (loadFiles && fmf.foundFiles != null) {
                    for (int i = 0; i < fmf.foundFiles.length; i++) {
                        try {
                            if (fmf.foundFiles[i].isFile()) {
                                String path = fmf.foundFiles[i].getPath();
                                klog.debug("Report {}: trying to parse {} for param: {}", rptName, path,
                                        fmf.foundFilesParamNames != null
                                                ? fmf.foundFilesParamNames[i] : "?");
                                FxProgressMonitorRead.read(fmf.foundFiles[i]);
                            }
                        } catch (Throwable t) {
                            klog.debug("Parsing error for file from param: {} file >{}",
                                    fmf.foundFilesParamNames != null
                                            ? fmf.foundFilesParamNames[i] : "?",
                                    fmf.foundFiles[i].getPath());
                            klog.debug(t.getMessage(), t);
                            atleastoneerr = true;
                            errs.append(t.getMessage()).append("<br>");
                        }
                    }
                    errs.append("</pre></body></html>");
                }

                if (atleastoneerr) {
                    AppServices.require().dialogs().showMessage(errs.toString());
                }
            } catch (Throwable t) {
                AppServices.require().dialogs().showError(t);
            }
        };
    }

    private static boolean confirmMissingFiles(File[] files) {
        StringBuilder buf = new StringBuilder(
                "The following file(s) were not found on the local file system:\n\n");
        for (File f : files) {
            buf.append(f).append('\n');
        }
        buf.append("\nContinue with the files that were found?");
        return AppServices.require().dialogs().showConfirm("Some Files Missing", buf.toString());
    }
}
