/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xtools.api.param;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Specialized param comparator
 * Would like (some) things to be sorted in an intuitive way (rather than alphabetic etc)
 * <p/>
 * Hence layer a (non-generic) comparison mechanism on top of alphabet based thing.
 *
 * @maint Add a new param and you should check to see if it needs to be sorted in a particular way
 */
public class ParamComparator implements Comparator {

    private static final List kSortOrder;

    static {

        kSortOrder = new ArrayList();

        kSortOrder.add(Param.RES);
        kSortOrder.add(Param.CLS);
        kSortOrder.add(Param.GRP);
        kSortOrder.add(Param.GMX);

        kSortOrder.add(Param.OUT); // keep these together  -> last reqd
        kSortOrder.add(Param.RPT); // keep me together -> 1st optional

        kSortOrder.add(Param.METRIC);
        kSortOrder.add(Param.CMETRIC);

        kSortOrder.add(Param.SORT);
        kSortOrder.add(Param.ORDER);

        // keep me last
        kSortOrder.add(Param.GUI);

    }

    public ParamComparator() {


    }

    /**
     * Return -1 if o1 is less than o2, 0 if they're equal, +1 if o1 is greater than o2.
     */
    public int compare(Object pn1, Object pn2) {

        Param p1 = (Param) pn1;
        Param p2 = (Param) pn2;

        String s1 = p1.getName();
        String s2 = p2.getName();

        int pos1 = kSortOrder.indexOf(s1);
        int pos2 = kSortOrder.indexOf(s2);

        if ((pos1 == -1) && (pos2 == -1)) { // do alphabet based sorting
            return s1.compareTo(s2);
        } else {

            //GUI is always last in opts
            if (p1 instanceof GuiParam) {
                return +1;
            }

            if (p2 instanceof GuiParam) {
                return -1;
            }

            //dir is always last in reqds
            if (p1 instanceof ReportDirParam) {
                return +1;
            }

            if (p2 instanceof ReportDirParam) {
                return -1;
            }

            // the rest
            if ((pos1 != -1) && (pos2 == -1)) {
                return -1;
            }

            if ((pos1 == -1) && (pos2 != -1)) {
                return +1;
            }


            if (pos1 < pos2) {
                return -1;
            } else if (pos1 == pos2) {
                return 0;
            } else {
                return +1;
            }
        }

    }

    /**
     * Return true if this equals o2.
     */
    public boolean equals(Object o2) {
        return false;
    }

}    // End ParamComparator

