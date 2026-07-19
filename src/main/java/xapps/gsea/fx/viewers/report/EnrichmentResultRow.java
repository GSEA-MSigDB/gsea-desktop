/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers.report;

import edu.mit.broad.genome.objects.esmatrix.db.EnrichmentResult;
import edu.mit.broad.genome.objects.esmatrix.db.EnrichmentScore;
import edu.mit.broad.genome.reports.EnrichmentReports;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * Table row for one enrichment result (shared by Leading Edge and GSEA report explorer).
 */
public final class EnrichmentResultRow {

    private final EnrichmentResult result;
    private final SimpleStringProperty name = new SimpleStringProperty();
    private final SimpleIntegerProperty size = new SimpleIntegerProperty();
    private final SimpleFloatProperty es = new SimpleFloatProperty();
    private final SimpleFloatProperty nes = new SimpleFloatProperty();
    private final SimpleFloatProperty nomP = new SimpleFloatProperty();
    private final SimpleFloatProperty fdr = new SimpleFloatProperty();
    private final SimpleFloatProperty fwer = new SimpleFloatProperty();
    private final SimpleIntegerProperty rankAtMax = new SimpleIntegerProperty();
    private final SimpleStringProperty leadingEdge = new SimpleStringProperty();

    public EnrichmentResultRow(EnrichmentResult result) {
        this.result = result;
        EnrichmentScore score = result.getScore();
        this.name.set(result.getGeneSet().getName(true));
        this.size.set(result.getGeneSet().getNumMembers());
        this.es.set(score.getES());
        this.nes.set(score.getNES());
        this.nomP.set(score.getNP());
        this.fdr.set(score.getFDR());
        this.fwer.set(score.getFWER());
        this.rankAtMax.set(result.getSignal().getRankAtMax());
        this.leadingEdge.set(EnrichmentReports.getLeadingEdge(result));
    }

    public EnrichmentResult getResult() {
        return result;
    }

    public SimpleStringProperty nameProperty() {
        return name;
    }

    public SimpleIntegerProperty sizeProperty() {
        return size;
    }

    public SimpleFloatProperty esProperty() {
        return es;
    }

    public SimpleFloatProperty nesProperty() {
        return nes;
    }

    public SimpleFloatProperty nomPProperty() {
        return nomP;
    }

    public SimpleFloatProperty fdrProperty() {
        return fdr;
    }

    public SimpleFloatProperty fwerProperty() {
        return fwer;
    }

    public SimpleIntegerProperty rankAtMaxProperty() {
        return rankAtMax;
    }

    public SimpleStringProperty leadingEdgeProperty() {
        return leadingEdge;
    }
}
