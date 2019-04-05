/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.math;

/**
 * enum construct
 */
public class Order {

    public static final Order ASCENDING = new Order("ascending");
    public static final Order DESCENDING = new Order("descending");

    /**
     * @maint manually keep array in sync with declarations above and below
     */
    public static final Order[] ALL = new Order[]{ASCENDING, DESCENDING};
    private final String fType;

    /**
     * @maint manually keep array in sync with declarations above and below
     * <p/>
     * Privatized class constructor.
     * <p/>
     * Privatized class constructor.
     */

    /**
     * Privatized class constructor.
     */
    private Order(String type) {
        this.fType = type;
    }

    public int hashCode() {
        return fType.hashCode();
    }

    public String toString() {
        return fType;
    }

    public boolean isAscending() {

        return this.fType.equalsIgnoreCase(ASCENDING.fType);

    }

    public boolean equals(Object obj) {

        if (obj instanceof Order) {
            if (((Order) obj).fType.equals(this.fType)) {
                return true;
            }
        }

        return false;
    }

    /**
     * a lookup metod for dir
     */
    public static Order lookup(Object obj) {

        if (obj == null) {
            throw new NullPointerException("Null dir not allowed");
        }

        if (obj instanceof Order) {
            return (Order) obj;
        }

        if (obj instanceof String) {
            if (obj.toString().equalsIgnoreCase(ASCENDING.fType)) {
                return ASCENDING;

                //else if (obj.toString().equalsIgnoreCase("ascending")) return ASCENDING;
            } else if (obj.toString().equalsIgnoreCase(DESCENDING.fType)) {
                return DESCENDING;
            }

            //else if (obj.toString().equalsIgnoreCase("descending")) return DESCENDING;
        }

        throw new IllegalArgumentException("Unable to lookup Order for: " + obj);
    }
}    // End Order
