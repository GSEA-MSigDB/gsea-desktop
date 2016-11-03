/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package org.genepattern.data.expr;

/**
 * @author Joshua Gould
 */
public class Util {

    /**
     * @param data
     * @param name
     * @return
     */
    public static boolean containsData(IExpressionData data, String name) {
        for (int i = 0, size = data.getDataCount(); i < size; i++) {
            if (data.getDataName(i).equals(name)) {
                return true;
            }
        }
        return false;
    }

}
