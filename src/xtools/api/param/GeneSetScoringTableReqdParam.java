/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xtools.api.param;

import edu.mit.broad.genome.alg.gsea.*;
import edu.mit.broad.genome.swing.fields.GComboBoxField;
import edu.mit.broad.genome.swing.fields.GFieldPlusChooser;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Object to capture commandline params</p>
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class GeneSetScoringTableReqdParam extends AbstractParam implements ActionListener {

    private GComboBoxField cbOptions;

    /**
     * Class constructor
     *
     * @param name
     * @param desc
     */
    public GeneSetScoringTableReqdParam() {
        this(new GeneSetScoringTables.Weighted());
    }

    public GeneSetScoringTableReqdParam(GeneSetScoringTable def) {
        super(SCORING_SCHEME, SCORING_SCHEME_ENGLISH, GeneSetScoringTable.class, SCORING_SCHEME_DESC,
                def, GeneSetScoringTables.createAllScoringTables(), true);
    }

    public GeneSetCohortGenerator createGeneSetCohortGenerator(boolean silent) {
        GeneSetScoringTable table = getGeneSetScoringTable();
        return new DefaultGeneSetCohort.Generator(table, silent);
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


    public GFieldPlusChooser getSelectionComponent() {

        if (cbOptions == null) {
            cbOptions = ParamHelper.createActionListenerBoundHintsComboBox(false, this, this);
            ParamHelper.safeSelectValueDefaultByString(cbOptions.getComboBox(), this);
        }

        return cbOptions;

    }

    public void actionPerformed(ActionEvent evt) {
        this.setValue((GeneSetScoringTable) ((JComboBox) cbOptions.getComponent()).getSelectedItem());
    }


}    // End class GeneSetCohortReqdParam
