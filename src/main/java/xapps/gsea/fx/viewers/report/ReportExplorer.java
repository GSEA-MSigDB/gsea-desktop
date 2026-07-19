/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers.report;

import java.util.function.Consumer;

import org.gsea_msigdb.gsea.ui.api.ViewPage;

import edu.mit.broad.genome.reports.api.Report;
import javafx.scene.Node;

/**
 * Native results UI for one {@link ReportKind}.
 */
@FunctionalInterface
public interface ReportExplorer {

    Node create(Report report, Consumer<ViewPage> openPage);
}
