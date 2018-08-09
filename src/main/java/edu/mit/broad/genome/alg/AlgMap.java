/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.alg;

import edu.mit.broad.genome.Headers;

import java.util.Map;

/**
 * @author Aravind Subramanian
 */
public class AlgMap {

    public static boolean isMedian(Map params) {
        return AlgMap.getParam(Headers.USE_MEDIAN, params, true);     // default
    }

    public static boolean isMean(Map params) {
        return !AlgMap.getParam(Headers.USE_MEDIAN, params, true);
    }

    public static boolean isBiased(Map params) {
        return getParam(Headers.USE_BIASED, params, false);
    }

    public static boolean isFixLowVar(Map params) {
        return getParam(Headers.FIX_LOW, params, true); // default
    }

    private static boolean getParam(String paramName, Map params, boolean def) {

        if (params == null) {
            return def;
        }

        boolean ret = def;    // default

        if (params.get(paramName) != null) {
            ret = ((Boolean) params.get(paramName)).booleanValue();
        }

        return ret;
    }

} // End class AlgMap
