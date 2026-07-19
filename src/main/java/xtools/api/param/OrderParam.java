/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xtools.api.param;

import edu.mit.broad.genome.math.Order;


/**
 * <p> Object to capture commandline params</p>
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class OrderParam extends AbstractParam {
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
}    // End class OrderParam
