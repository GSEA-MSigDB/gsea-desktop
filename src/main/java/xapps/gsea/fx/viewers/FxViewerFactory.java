/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers;

import org.gsea_msigdb.gsea.ui.api.FeatureHost;
import org.gsea_msigdb.gsea.ui.api.ViewPage;

import edu.mit.broad.genome.objects.Dataset;
import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.objects.GeneSetMatrix;
import edu.mit.broad.genome.objects.RankedList;
import edu.mit.broad.genome.objects.Template;
import edu.mit.broad.genome.reports.api.Report;
import edu.mit.broad.vdb.chip.Chip;
import xapps.gsea.fx.viewers.report.FxReportOpen;

/**
 * Dispatches PersistentObjects to the matching JavaFX viewer page.
 */
public final class FxViewerFactory {

    private FxViewerFactory() {
    }

    public static ViewPage open(Object pob) {
        return open(pob, null);
    }

    public static ViewPage open(Object pob, FeatureHost host) {
        if (pob == null) {
            throw new IllegalArgumentException("Object cannot be null");
        }
        if (pob instanceof Dataset) {
            return new FxDatasetViewer((Dataset) pob);
        }
        if (pob instanceof RankedList) {
            return new FxRankedListViewer((RankedList) pob);
        }
        if (pob instanceof GeneSetMatrix) {
            return new FxGeneSetMatrixViewer((GeneSetMatrix) pob);
        }
        if (pob instanceof GeneSet) {
            return new FxGeneSetViewer((GeneSet) pob);
        }
        if (pob instanceof Template) {
            return new FxPhenotypeViewer((Template) pob);
        }
        if (pob instanceof Chip) {
            return new FxChipViewer((Chip) pob);
        }
        if (pob instanceof Report) {
            if (host == null) {
                throw new IllegalStateException("FeatureHost is required to open a Report");
            }
            return FxReportOpen.viewPageFor((Report) pob, host);
        }
        throw new IllegalArgumentException("No viewer for type: " + pob.getClass().getName());
    }
}
