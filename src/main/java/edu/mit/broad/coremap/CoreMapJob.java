/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.coremap;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import edu.mit.broad.coremap.CoreMapTypes.IntegrationResult;

/**
 * Reloadable CoreMap job snapshot (provenance + options + integration result).
 */
public final class CoreMapJob {

    public static final int FORMAT_VERSION = 1;
    public static final String JOB_FILE = "coremap-job.json";
    public static final String RESULT_FILE = "coremap-result.json";
    public static final String BRIDGES_TSV = "coremap-bridges.tsv";
    public static final String PNG_FILE = "coremap.png";

    public int formatVersion = FORMAT_VERSION;
    public String savedAt;
    public String mechanisticPath;
    public String phenotypicPath;
    public String mechanisticName;
    public String phenotypicName;
    public double minNes = CoreMapConstants.DEFAULT_MIN_NES;
    public double maxNp = CoreMapConstants.DEFAULT_MAX_NP;
    public double maxFdr = CoreMapConstants.DEFAULT_MAX_FDR;
    public boolean separateLayerThresholds;
    public Double phenoMinNes;
    public Double phenoMaxNp;
    public Double phenoMaxFdr;
    public List<String> mechanisticSetIds = new ArrayList<>();
    public List<String> phenotypicSetIds = new ArrayList<>();
    public IntegrationOptions options = IntegrationOptions.defaults();
    public String viewMode = "connectome";
    /** cascade | mechanism | all — Genes filter for connectome/gene views. */
    public String geneVisibility = "cascade";
    public String providerKey;
    public Double fetchedMinScore;
    public String resultFile = RESULT_FILE;
    public IntegrationResult result;

    public Set<String> mechanisticSetIdSet() {
        return new LinkedHashSet<>(mechanisticSetIds != null ? mechanisticSetIds : List.of());
    }

    public Set<String> phenotypicSetIdSet() {
        return new LinkedHashSet<>(phenotypicSetIds != null ? phenotypicSetIds : List.of());
    }
}
