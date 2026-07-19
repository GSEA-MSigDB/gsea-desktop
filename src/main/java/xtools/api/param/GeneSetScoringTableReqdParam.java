/*
 * Copyright (c) 2003-2021 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xtools.api.param;

import edu.mit.broad.genome.alg.gsea.*;


/**
 * Object to capture commandline params</p>
 *
 * @author Aravind Subramanian, David Eby
 */
public class GeneSetScoringTableReqdParam extends AbstractParam {
public GeneSetScoringTableReqdParam() {
        this(new GeneSetScoringTables.Weighted());
    }

    public GeneSetScoringTableReqdParam(GeneSetScoringTable def) {
        super(SCORING_SCHEME, SCORING_SCHEME_ENGLISH, GeneSetScoringTable.class, SCORING_SCHEME_DESC,
                def, GeneSetScoringTables.createAllScoringTables(), true);
    }

    public GeneSetCohort.Generator createGeneSetCohortGenerator(int geneSetMinSize, int geneSetMaxSize) {
        GeneSetScoringTable table = getGeneSetScoringTable();
        return new GeneSetCohort.Generator(table, geneSetMinSize, geneSetMaxSize);
    }

    public GeneSetScoringTable getGeneSetScoringTable() {
        return (GeneSetScoringTable) getValue();
    }

    public boolean isFileBased() {
        return false;
    }

    public void setValue(Object value) {
        if (value == null) {
            super.setValue(null);
        } else {
            super.setValue(GeneSetScoringTables.lookupGeneSetScoringTable(value));
        }
    }

    public void setValue(GeneSetScoringTable table) {
        super.setValue(table);
    }
}
