/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xtools.api.param;

import edu.mit.broad.genome.alg.Metric;
import edu.mit.broad.genome.alg.Metrics;
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
public class MetricParam extends AbstractParam implements ActionListener {

    private GComboBoxField cbOptions;

    public MetricParam(final Metric[] metrics, final boolean reqd) {
        this(metrics[0], metrics, reqd);
    }

    public MetricParam(final Metric def, final Metric[] metrics, final boolean reqd) {
        super(METRIC, METRIC_ENGLISH, Metric.class, METRIC_DESC, def, metrics, reqd);
    }

    public boolean isFileBased() {
        return false;
    }

    public void setValue(Object value) {

        if (value == null) {
            super.setValue(null);
        } else {
            super.setValue(Metrics.lookupMetric(value));
        }
    }

    public void setValue(Metric metric) {
        super.setValue(metric);
    }

    public Metric getMetric() {

        Object val = super.getValue();

        if (val == null) {
            throw new NullPointerException("Null param value. Always check isSpecified() before calling");
        }

        return (Metric) val;
    }


    public GFieldPlusChooser getSelectionComponent() {

        if (cbOptions == null) {
            cbOptions = ParamHelper.createActionListenerBoundHintsComboBox(false, this, this);
            ParamHelper.safeSelectValueDefaultByString(cbOptions.getComboBox(), this);
        }

        return cbOptions;

    }

    public void actionPerformed(ActionEvent evt) {
        this.setValue((Metric) ((JComboBox) cbOptions.getComponent()).getSelectedItem());
    }
}