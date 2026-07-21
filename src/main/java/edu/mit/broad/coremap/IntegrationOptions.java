/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.coremap;

import java.util.ArrayList;
import java.util.List;

import edu.mit.broad.coremap.CoreMapTypes.InteractomeSource;
import edu.mit.broad.coremap.CoreMapTypes.StringMode;

/** Integration / scoring options (ported from CoreMap {@code IntegrationOptions}). */
public final class IntegrationOptions {

    public double minInteractionScore = 0.4;
    public int maxPathLength = 12;
    public double pathLengthPenalty = 0.08;
    public int topKBridges = 10;
    public boolean includeExtraNeighbors = false;
    public int neighborLimit = 0;
    public InteractomeSource interactomeSource = InteractomeSource.FUSED;
    public StringMode stringMode = StringMode.INTEGRATED;
    public int signorLevel = 1;
    public String signorQueryType = "connect";
    public String signorOrganism = "9606";
    public boolean signorProteinOnly = true;
    public boolean signorDirectOnly = false;
    public List<String> signorPathways = new ArrayList<>();
    public double directionWeight = 0.6;
    public double sharedDriverDirectionWeight = 0.45;
    public boolean enableSourceEnrichment = true;
    public String msigdbPath;
    public int nullPermutations = 0;

    public static IntegrationOptions defaults() {
        return new IntegrationOptions();
    }

    public IntegrationOptions copy() {
        IntegrationOptions o = new IntegrationOptions();
        o.minInteractionScore = minInteractionScore;
        o.maxPathLength = maxPathLength;
        o.pathLengthPenalty = pathLengthPenalty;
        o.topKBridges = topKBridges;
        o.includeExtraNeighbors = includeExtraNeighbors;
        o.neighborLimit = neighborLimit;
        o.interactomeSource = interactomeSource;
        o.stringMode = stringMode;
        o.signorLevel = signorLevel;
        o.signorQueryType = signorQueryType;
        o.signorOrganism = signorOrganism;
        o.signorProteinOnly = signorProteinOnly;
        o.signorDirectOnly = signorDirectOnly;
        o.signorPathways = new ArrayList<>(signorPathways);
        o.directionWeight = directionWeight;
        o.sharedDriverDirectionWeight = sharedDriverDirectionWeight;
        o.enableSourceEnrichment = enableSourceEnrichment;
        o.msigdbPath = msigdbPath;
        o.nullPermutations = nullPermutations;
        return o;
    }
}
