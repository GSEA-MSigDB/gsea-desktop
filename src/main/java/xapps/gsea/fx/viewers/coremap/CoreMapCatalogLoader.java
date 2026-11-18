/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers.coremap;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import edu.mit.broad.coremap.CoreMapTypes.EnrichmentParseResult;
import edu.mit.broad.coremap.CoreMapTypes.GeneSetSummary;
import edu.mit.broad.coremap.EdbEnrichmentParser;
import edu.mit.broad.coremap.EnrichmentMath;

/**
 * Parses one or two GSEA report folders into gene-set catalogs.
 * The pane applies the result on the FX thread.
 */
final class CoreMapCatalogLoader {

    static final class Layers {
        final EnrichmentParseResult mech;
        final EnrichmentParseResult pheno;
        final List<GeneSetSummary> mechSets;
        final List<GeneSetSummary> phenoSets;

        private Layers(EnrichmentParseResult mech, EnrichmentParseResult pheno,
                List<GeneSetSummary> mechSets, List<GeneSetSummary> phenoSets) {
            this.mech = mech;
            this.pheno = pheno;
            this.mechSets = mechSets;
            this.phenoSets = phenoSets;
        }
    }

    private CoreMapCatalogLoader() {
    }

    static Layers load(File mechDir, File phenoDir) throws Exception {
        CompletableFuture<EnrichmentParseResult> mechFut = mechDir != null
                ? CompletableFuture.supplyAsync(() -> parseDir(mechDir))
                : CompletableFuture.completedFuture(null);
        CompletableFuture<EnrichmentParseResult> phenoFut = phenoDir != null
                ? CompletableFuture.supplyAsync(() -> parseDir(phenoDir))
                : CompletableFuture.completedFuture(null);
        EnrichmentParseResult mech;
        EnrichmentParseResult pheno;
        try {
            mech = mechFut.join();
            pheno = phenoFut.join();
        } catch (RuntimeException ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            if (cause instanceof Exception e) {
                throw e;
            }
            throw ex;
        }
        List<GeneSetSummary> mechSets = mech != null
                ? EnrichmentMath.summarizeGenesets(mech.rows, 0, 1, 1)
                : List.of();
        List<GeneSetSummary> phenoSets = pheno != null
                ? EnrichmentMath.summarizeGenesets(pheno.rows, 0, 1, 1)
                : List.of();
        return new Layers(mech, pheno, mechSets, phenoSets);
    }

    private static EnrichmentParseResult parseDir(File dir) {
        try {
            return EdbEnrichmentParser.parseReportDirectory(dir, 0, 1, 1, false);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
