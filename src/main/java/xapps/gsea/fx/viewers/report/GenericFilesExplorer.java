/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers.report;

import org.gsea_msigdb.gsea.ui.api.FeatureHost;

import edu.mit.broad.genome.reports.api.Report;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import xapps.gsea.fx.viewers.FxReportFilesList;

/**
 * Fallback explorer: the classic report files list.
 */
public final class GenericFilesExplorer implements ReportExplorer {

    @Override
    public Node create(Report report, FeatureHost host) {
        ListView<java.io.File> filesList = FxReportFilesList.create(report, host::openPage);
        Label hint = new Label("All files in the report folder (double-click to view)");
        VBox box = new VBox(8, hint, filesList);
        box.setPadding(new Insets(8, 12, 12, 12));
        VBox.setVgrow(filesList, Priority.ALWAYS);
        return box;
    }
}
