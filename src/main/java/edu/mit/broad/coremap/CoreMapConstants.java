/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.coremap;

/**
 * Shared numeric defaults for the CoreMap engine (ported from CoreMap TypeScript).
 */
public final class CoreMapConstants {

    public static final double RNK_SCALE = 2.0;
    public static final double LE_FLOOR = 0.55;
    public static final double EXT_SCALE = 0.6;
    public static final double EXT_CAP = 0.7;
    public static final double CLASS3_CAP = 0.25;
    public static final double CLASS3_INTERACTION_FACTOR = 0.35;
    public static final double CLASS3_HUB_MU = 0.02;

    public static final double DEFAULT_MIN_NES = 1.6;
    public static final double DEFAULT_MAX_NP = 0.05;
    public static final double DEFAULT_MAX_FDR = 0.25;

    public static final double JACCARD_CLUSTER_THRESHOLD = 0.5;

    public static final int MAX_PATHS_PER_SET_PAIR = 3;
    public static final double CASCADE_MULTIPLICITY_WEIGHT = 0.45;
    public static final int MAX_HOP_COMBINATIONS = 64;
    public static final double CLASS3_CLASS3_RELIABILITY = 0.85;
    public static final double ASSOCIATIVE_RELIABILITY_FLOOR = 0.15;
    public static final double ASSOCIATIVE_DIRECTION_SCALE = 0.5;
    public static final double RELIABILITY_EPS = 1e-6;
    public static final double MID_PATH_RESIDUAL_WEIGHT = 0.15;
    public static final double PATHWAY_COHERENCE_WEIGHT = 0.08;

    public static final double POLARITY_COMPAT_CONFLICT = 0.45;
    public static final double POLARITY_COMPAT_MATCH = 1.35;
    public static final double POLARITY_COMPAT_MISMATCH = 0.45;

    public static final double FETCH_MIN_SCORE_DEFAULT = 0.0;
    public static final int STRING_QUERY_CAP = 250;

    public static final int TOP_LAYER_HUBS_MIN = 15;
    public static final int TOP_LAYER_HUBS_MAX = 40;

    private CoreMapConstants() {
    }

    public static double clamp(double n, double lo, double hi) {
        return Math.min(hi, Math.max(lo, n));
    }

    public static double clamp01(double n) {
        return clamp(n, 0.0, 1.0);
    }

    public static double round4(double n) {
        return Math.round(n * 10000.0) / 10000.0;
    }

    public static int sign(double v) {
        if (v > 0) {
            return 1;
        }
        if (v < 0) {
            return -1;
        }
        return 0;
    }
}
