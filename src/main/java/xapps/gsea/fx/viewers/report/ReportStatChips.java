/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers.report;

import edu.mit.broad.genome.objects.esmatrix.db.EnrichmentDb;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

/**
 * Compact enrichment summary chips shared by GSEA report explorer and Analysis History.
 */
public final class ReportStatChips {

    private ReportStatChips() {
    }

    public static Node chip(String value, String label) {
        Label valueLbl = new Label(value);
        valueLbl.getStyleClass().add("gsea-stat-value");
        Label labelLbl = new Label(label);
        labelLbl.getStyleClass().add("gsea-stat-label");
        VBox chip = new VBox(1, valueLbl, labelLbl);
        chip.getStyleClass().add("gsea-stat-chip");
        return chip;
    }

    /**
     * Overview counts plus per-phenotype FDR / NOM significance chips for a loaded EDB.
     */
    public static Node gseaSummary(EnrichmentDb edb) {
        if (edb == null) {
            return new Label("");
        }
        PhenotypeLabels pos = PhenotypeLabels.from(edb, true);
        PhenotypeLabels neg = PhenotypeLabels.from(edb, false);
        PhenoStats posStats = PhenoStats.of(edb, true);
        PhenoStats negStats = PhenoStats.of(edb, false);
        int totalSets = edb.getNumResults();

        int genes = 0;
        try {
            if (edb.getRankedList() != null) {
                genes = edb.getRankedList().getSize();
            }
        } catch (Throwable ignored) {
            // ranked list optional for some loads
        }

        FlowPane overview = new FlowPane(8, 8);
        overview.getChildren().addAll(
                chip(String.valueOf(totalSets), "gene sets"),
                chip(String.valueOf(genes), "genes in ranking"),
                chip(String.valueOf(edb.getNumPerm()), "permutations"),
                chip(String.valueOf(posStats.enriched()), "↑ " + pos.shortName()),
                chip(String.valueOf(negStats.enriched()), "↑ " + neg.shortName()));

        VBox box = new VBox(10, overview,
                phenotypeRow(pos, posStats, totalSets),
                phenotypeRow(neg, negStats, totalSets));
        box.getStyleClass().add("gsea-history-summary");
        return box;
    }

    private static Node phenotypeRow(PhenotypeLabels pheno, PhenoStats stats, int totalSets) {
        Label title = new Label(pheno.longName());
        title.getStyleClass().addAll("gsea-pheno-title",
                pheno.positive() ? "gsea-pheno-pos" : "gsea-pheno-neg");
        title.setWrapText(true);

        FlowPane chips = new FlowPane(6, 6);
        chips.getChildren().addAll(
                chip(stats.enriched() + " / " + totalSets, "enriched in " + pheno.shortName()),
                chip(String.valueOf(stats.fdr25()), "FDR < 25%"),
                chip(String.valueOf(stats.fdr5()), "FDR < 5%"),
                chip(String.valueOf(stats.nom1()), "NOM p < 1%"),
                chip(String.valueOf(stats.nom5()), "NOM p < 5%"));

        VBox row = new VBox(4, title, chips);
        row.setPadding(new Insets(0, 0, 2, 0));
        return row;
    }

    private record PhenoStats(int enriched, int fdr25, int fdr5, int nom1, int nom5) {
        static PhenoStats of(EnrichmentDb edb, boolean pos) {
            return new PhenoStats(
                    edb.getNumScores(pos),
                    edb.getNumFDRSig(0.25f, pos),
                    edb.getNumFDRSig(0.05f, pos),
                    edb.getNumNominallySig(0.01f, pos),
                    edb.getNumNominallySig(0.05f, pos));
        }
    }
}
