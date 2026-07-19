/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers.report;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

import org.gsea_msigdb.gsea.ui.api.ViewPage;

import edu.mit.broad.genome.reports.api.Report;
import javafx.scene.Node;
import javafx.scene.control.Tab;

/**
 * Results explorer for LeadingEdgeTool reports (heatmap + HTML Report tabs).
 */
public final class LeadingEdgeReportExplorer implements ReportExplorer {

    @Override
    public Node create(Report report, Consumer<ViewPage> openPage) {
        File dir = ReportExplorerSupport.reportDir(report);
        List<Tab> tabs = LeadingEdgeOutputs.buildTabs(dir, false);
        if (tabs.isEmpty()) {
            return new GenericFilesExplorer().create(report, openPage);
        }
        return ReportExplorerSupport.tabPane(tabs.toArray(Tab[]::new));
    }
}
