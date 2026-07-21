/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xtools.gsea;

import java.util.Properties;

import edu.mit.broad.genome.reports.api.ReportIndexState;
import xtools.api.AbstractTool;
import xtools.api.ToolCategory;

/**
 * Marker / report producer for saved CoreMap jobs. Executable only via
 * {@link edu.mit.broad.coremap.CoreMapJobStore#save}; declareParams is empty.
 */
public class CoreMapTool extends AbstractTool {

    public CoreMapTool(final Properties properties) {
        super.init(properties, "");
    }

    public CoreMapTool(final Properties properties, String paramFilePath) {
        super.init(properties, paramFilePath);
    }

    public CoreMapTool(final String[] args) {
        super.init(args);
    }

    /** Param-set interrogation only — not executable from ToolRunner. */
    public CoreMapTool() {
        declareParams();
    }

    @Override
    public String getName() {
        return "CoreMapTool";
    }

    @Override
    public ToolCategory getCategory() {
        return ToolCategory.GSEA;
    }

    @Override
    public void declareParams() {
        // Shared AbstractTool params (out, rpt_label, gui) only.
    }

    @Override
    public void execute() {
        throw new UnsupportedOperationException(
                "CoreMapTool reports are created from the CoreMap workspace (Save CoreMap Job).");
    }

    /** Open a ToolReport for CoreMapJobStore writers. */
    public void startJobReport(ReportIndexState indexState) throws java.io.IOException {
        super.startExec(indexState);
    }

    /** Finalize report + copy .rpt into reports cache. */
    public void finishReport() {
        doneExec();
    }
}
