/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.alg;

import edu.mit.broad.genome.Headers;

import java.util.Map;

/**
 * @author Aravind Subramanian
 */
public class AlgMap {

    public static boolean isMedian(Map<String, Boolean> params) {
        return AlgMap.getParam(Headers.USE_MEDIAN, params, true);     // default
    }

    public static boolean isMean(Map<String, Boolean> params) {
        return !AlgMap.getParam(Headers.USE_MEDIAN, params, true);
    }

    public static boolean isBiased(Map<String, Boolean> params) {
        return getParam(Headers.USE_BIASED, params, false);
    }

    public static boolean isFixLowVar(Map<String, Boolean> params) {
        return getParam(Headers.FIX_LOW, params, true); // default
    }

    private static boolean getParam(String paramName, Map<String, Boolean> params, boolean def) {

        if (params == null) return def;

        if (params.get(paramName) != null) {
            return params.get(paramName);
        }

        return def;
    }
}