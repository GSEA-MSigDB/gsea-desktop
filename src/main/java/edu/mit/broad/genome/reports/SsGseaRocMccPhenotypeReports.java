/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.reports;

import edu.mit.broad.genome.math.StringMatrix;
import edu.mit.broad.genome.objects.StringDataframe;
import edu.mit.broad.genome.reports.api.ToolReport;
import edu.mit.broad.genome.reports.pages.HtmlPage;
import gnu.trove.TIntIntHashMap;
import org.apache.ecs.html.A;
import org.apache.ecs.html.IMG;
import org.apache.ecs.html.TD;
import org.apache.ecs.html.TR;
import org.apache.ecs.html.Table;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Splits 2-class ROC results by MCC sign and writes GSEA-style positive / negative "basic" HTML+TSV
 * (MCC is used like NES for which side a gene set belongs to and for sorting).
 */
public final class SsGseaRocMccPhenotypeReports {

    public static final String[] ROC_TSV_COLS = new String[]{
            "Name", "Description", "AUC", "Matthews Correlation (MCC)", "cutoff value (Youden Index)",
            "Sensitivity", "Specificity", "PPV", "NPV", "ssGSEA Score Wilcox pValue"
    };

    public static final class MccPhenoSplit {
        public final File posBasicHtml;
        public final File negBasicHtml;
        public final File posTsv;
        public final File negTsv;
        public final File posSnapshotHtml;
        public final File negSnapshotHtml;
        /** Counts in the MCC>0 / MCC<0 / MCC==0 buckets (MCC=0 in neither per-phenotype report). */
        public final int nMccPos;
        public final int nMccNeg;
        public final int nMccZero;

        MccPhenoSplit(final File posBasicHtml, final File negBasicHtml,
                final File posTsv, final File negTsv, final File posSnapshotHtml, final File negSnapshotHtml,
                int nMccPos, int nMccNeg, int nMccZero) {
            this.posBasicHtml = posBasicHtml;
            this.negBasicHtml = negBasicHtml;
            this.posTsv = posTsv;
            this.negTsv = negTsv;
            this.posSnapshotHtml = posSnapshotHtml;
            this.negSnapshotHtml = negSnapshotHtml;
            this.nMccPos = nMccPos;
            this.nMccNeg = nMccNeg;
            this.nMccZero = nMccZero;
        }
    }

    private static final class TsvRdf {
        private final File tsv;
        private final RichDataframe rdf;

        TsvRdf(final File tsv, final RichDataframe rdf) {
            this.tsv = tsv;
            this.rdf = rdf;
        }
    }

    private SsGseaRocMccPhenotypeReports() {
    }

    public static MccPhenoSplit write(final ToolReport report, final File reportDir, final String rocFilePrefix,
            final String coiClassName, final List<SsGseaRocAnalysis.ResultRow> rocRows, final File rocPlotsDir) {

        if (rocRows == null) {
            throw new IllegalArgumentException("rocRows cannot be null");
        }
        if (coiClassName == null) {
            throw new IllegalArgumentException("coiClassName cannot be null");
        }
        if (rocFilePrefix == null || reportDir == null) {
            throw new IllegalArgumentException("rocFilePrefix and reportDir are required");
        }
        if (rocRows.isEmpty()) {
            return new MccPhenoSplit(null, null, null, null, null, null, 0, 0, 0);
        }
        long ts = report.getTimestamp();
        String posName = "ssgsea_roc_mccpos_" + rocFilePrefix + "_" + ts;
        String negName = "ssgsea_roc_mccneg_" + rocFilePrefix + "_" + ts;
        if (posName.length() > 200) {
            posName = posName.substring(0, 200);
        }
        if (negName.length() > 200) {
            negName = negName.substring(0, 200);
        }
        String posPageTitle = "ssGSEA ROC &mdash; MCC &gt; 0 (aligned with " + coiClassName + ") [" + report.getTimestamp() + "]";
        String negPageTitle = "ssGSEA ROC &mdash; MCC &lt; 0 (opposite to " + coiClassName + ") [" + report.getTimestamp() + "]";

        final List<SsGseaRocAnalysis.ResultRow> pos = new ArrayList<SsGseaRocAnalysis.ResultRow>();
        final List<SsGseaRocAnalysis.ResultRow> neg = new ArrayList<SsGseaRocAnalysis.ResultRow>();
        for (SsGseaRocAnalysis.ResultRow row : rocRows) {
            if (row == null) {
                continue;
            }
            if (row.mcc > 0) {
                pos.add(row);
            } else if (row.mcc < 0) {
                neg.add(row);
            }
        }
        pos.sort(Comparator.comparingDouble((SsGseaRocAnalysis.ResultRow a) -> a.mcc).reversed());
        neg.sort(Comparator.comparingDouble((SsGseaRocAnalysis.ResultRow a) -> a.mcc));
        final int nMccPos = pos.size();
        final int nMccNeg = neg.size();
        final int nMccZero = Math.max(0, rocRows.size() - nMccPos - nMccNeg);

        TsvRdf pr = buildOneSideTsvRdf(report, reportDir, posName, pos);
        TsvRdf nr = buildOneSideTsvRdf(report, reportDir, negName, neg);

        HtmlPage posPage = new HtmlPage(posName, posPageTitle);
        if (pr != null) {
            posPage.addTable(pr.rdf, pr.tsv.getName(), false);
        } else {
            posPage.addHtml("<p>There are no gene sets with MCC &gt; 0 for this run (MCC=0 is excluded from both reports).</p>");
        }
        final File posBasicHtml = report.savePage(posPage, reportDir);

        HtmlPage negPage = new HtmlPage(negName, negPageTitle);
        if (nr != null) {
            negPage.addTable(nr.rdf, nr.tsv.getName(), false);
        } else {
            negPage.addHtml("<p>There are no gene sets with MCC &lt; 0 for this run (MCC=0 is excluded from both reports).</p>");
        }
        final File negBasicHtml = report.savePage(negPage, reportDir);

        final String rocRel = "ssgsea_roc_plots/";
        final File posSnap = (rocPlotsDir != null && pr != null) ? buildRocSnapshotPage(
                report, reportDir, "ssgsea_roc_mccpos_snapshot_" + ts, "MCC &gt; 0", pos, rocPlotsDir, rocRel) : null;
        final File negSnap = (rocPlotsDir != null && nr != null) ? buildRocSnapshotPage(
                report, reportDir, "ssgsea_roc_mccneg_snapshot_" + ts, "MCC &lt; 0", neg, rocPlotsDir, rocRel) : null;

        return new MccPhenoSplit(
                posBasicHtml, negBasicHtml,
                pr == null ? null : pr.tsv,
                nr == null ? null : nr.tsv,
                posSnap, negSnap,
                nMccPos, nMccNeg, nMccZero);
    }

    private static TsvRdf buildOneSideTsvRdf(final ToolReport report, final File reportDir, final String name,
            final List<SsGseaRocAnalysis.ResultRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        StringDataframe sdf = rowsToSdf(name, rows);
        File tsv = report.savePageTsv(sdf, name, reportDir);
        RichDataframe rdf = richDataframe(sdf, tsv.getName());
        return new TsvRdf(tsv, rdf);
    }

    private static StringDataframe rowsToSdf(final String name, final List<SsGseaRocAnalysis.ResultRow> rows) {
        int n = rows.size();
        final StringMatrix sm = new StringMatrix(n, ROC_TSV_COLS.length);
        for (int r = 0; r < n; r++) {
            fillTsvRow(sm, r, rows.get(r));
        }
        final String[] tsvRowNames = new String[n];
        for (int r = 0; r < n; r++) {
            tsvRowNames[r] = "r" + (r + 1);
        }
        return new StringDataframe(name, sm, tsvRowNames, ROC_TSV_COLS);
    }

    private static void fillTsvRow(final StringMatrix sm, int r, final SsGseaRocAnalysis.ResultRow row) {
        sm.setElement(r, 0, row.geneSetName);
        sm.setElement(r, 1, row.description == null ? "" : row.description);
        sm.setElement(r, 2, row.auc);
        sm.setElement(r, 3, row.mcc);
        sm.setElement(r, 4, row.youdenCutoff);
        sm.setElement(r, 5, row.sensitivity);
        sm.setElement(r, 6, row.specificity);
        sm.setElement(r, 7, row.ppv);
        sm.setElement(r, 8, row.npv);
        sm.setElement(r, 9, row.wilcoxP);
    }

    private static RichDataframe richDataframe(final StringDataframe sdf, final String tsvFileName) {
        final TIntIntHashMap colPrecision = new TIntIntHashMap();
        for (int c = 2; c < ROC_TSV_COLS.length; c++) {
            if (c == 9) {
                colPrecision.put(9, 4);
            } else {
                colPrecision.put(c, 3);
            }
        }
        RichDataframe.MetaData md = new RichDataframe.MetaData(tsvFileName, colPrecision);
        return new RichDataframe(sdf, md, null, null);
    }

    private static File buildRocSnapshotPage(final ToolReport report, final File reportDir, final String pageName,
            final String groupLabel, final List<SsGseaRocAnalysis.ResultRow> rows, final File rocDir, final String rocPathRel) {
        final String sn = pageName.length() > 200 ? pageName.substring(0, 200) : pageName;
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        SsGseaRocAnalysis.ResultRow[] rarr = rows.toArray(new SsGseaRocAnalysis.ResultRow[0]);
        Table table = new Table();
        int index = 0;
        for (int u = 0; u < rarr.length; u++) {
            TR tr = new TR();
            for (int c = 0; c < 3; c++) {
                if (index >= rarr.length) {
                    break;
                }
                SsGseaRocAnalysis.ResultRow rrow = rarr[index];
                final String safe = SsGseaRocChart.toFileSafeName(rrow.geneSetName);
                String png = rocPathRel + "ssgsea_roc_" + safe + ".png";
                File f = new File(rocDir, "ssgsea_roc_" + safe + ".png");
                if (f.isFile()) {
                    A a = new A();
                    a.setName("");
                    a.setHref(png);
                    IMG img = new IMG();
                    img.setSrc(png);
                    img.setWidth(200);
                    img.setHeight(200);
                    a.addElement(img);
                    tr.addElement(new TD(a));
                } else {
                    tr.addElement(new TD("(" + rrow.geneSetName + " &mdash; no PNG in ssgsea_roc_plots/)"));
                }
                index++;
            }
            table.addElement(tr);
            if (index >= rarr.length) {
                break;
            }
        }
        HtmlPage page = new HtmlPage(sn, "Snapshot: " + rarr.length + " ROC " + (rarr.length == 1 ? "plot" : "plots")
                + " for " + groupLabel);
        page.addHtml("<p>Thumbnail grid (three per row), same ordering as the MCC-sorted table for " + groupLabel
                + ". A cell is filled only when the corresponding PNG exists in <code>ssgsea_roc_plots/</code>.</p>");
        page.addTable("ROC plot snapshots: " + groupLabel, table);
        return report.savePage(page, reportDir);
    }
}