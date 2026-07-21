/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers.coremap;

import edu.mit.broad.coremap.CoreMapTypes.GeneSetSummary;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

/** Row model for CoreMap gene-set selection tables. */
public final class CoreMapGeneSetRow {
    public final SimpleStringProperty setId = new SimpleStringProperty();
    public final SimpleDoubleProperty nes = new SimpleDoubleProperty();
    public final SimpleDoubleProperty pValue = new SimpleDoubleProperty();
    public final SimpleDoubleProperty fdr = new SimpleDoubleProperty();
    public final SimpleIntegerProperty leCount = new SimpleIntegerProperty();
    public final SimpleBooleanProperty passed = new SimpleBooleanProperty();
    public final SimpleBooleanProperty included = new SimpleBooleanProperty();

    public static CoreMapGeneSetRow from(GeneSetSummary s) {
        CoreMapGeneSetRow r = new CoreMapGeneSetRow();
        r.setId.set(s.setId);
        r.nes.set(s.nes != null ? s.nes : Double.NaN);
        r.pValue.set(s.pValue != null ? s.pValue : Double.NaN);
        r.fdr.set(s.fdr != null ? s.fdr : Double.NaN);
        r.leCount.set(s.leadingEdgeCount);
        r.passed.set(s.passedFilter);
        r.included.set(s.passedFilter);
        return r;
    }
}
