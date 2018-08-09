/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xtools.api.param;

import edu.mit.broad.genome.math.Order;
import edu.mit.broad.genome.swing.fields.GComboBoxField;
import edu.mit.broad.genome.swing.fields.GFieldPlusChooser;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * <p> Object to capture commandline params</p>
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class OrderParam extends AbstractParam implements ActionListener {

    private GComboBoxField cbOptions;

    /**
     * Class constructor
     *
     * @param name
     * @param desc
     */
    public OrderParam(boolean reqd) {
        this(Order.DESCENDING, reqd);
    }

    public OrderParam(Order def, boolean reqd) {
        super(ORDER, ORDER_ENGLISH, Order.class, ORDER_DESC, def, Order.ALL, reqd);
    }

    public boolean isFileBased() {
        return false;
    }

    public void setValue(Object value) {

        if (value == null) {
            super.setValue(null);
        } else {
            super.setValue(Order.lookup(value));
        }
    }

    public void setValue(Order order) {
        super.setValue(order);
    }

    public Order getOrder() {

        Object val = super.getValue();

        if (val == null) {
            throw new NullPointerException("Null param value. Always check isSpecified() before calling");
        }

        return (Order) val;
    }


    public GFieldPlusChooser getSelectionComponent() {

        if (cbOptions == null) {
            cbOptions = ParamHelper.createActionListenerBoundHintsComboBox(false, this, this);
            ParamHelper.safeSelectValueDefaultByString(cbOptions.getComboBox(), this);
        }

        return cbOptions;

    }

    public void actionPerformed(ActionEvent evt) {
        this.setValue((Order) ((JComboBox) cbOptions.getComponent()).getSelectedItem());

    }

}    // End class OrderParam
