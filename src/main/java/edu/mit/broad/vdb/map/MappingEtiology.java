/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.vdb.map;

import edu.mit.broad.genome.reports.NullSafeStringBuffer;
import edu.mit.broad.genome.utils.ImmutedException;

import java.util.*;

/**
 * @author Aravind Subramanian
 */
public class MappingEtiology {

    private boolean fImmuted;

    private List<OneMap> fOneMaps = new ArrayList<OneMap>();

    private String fGeneSetName;
    private String fTargetChipName;

    public MappingEtiology(final String gsetName,
                           final String targetChipName) {
        this.fGeneSetName = gsetName;
        this.fTargetChipName = targetChipName;
    }

    public int getTotalNumOfSourceMembersActuallyMapped() {

        int cnt_with_at_least_one_match = 0;
        for (OneMap one : fOneMaps) {
            if (one.size() > 0) {
                cnt_with_at_least_one_match++;
            }
        }

        return cnt_with_at_least_one_match;
    }

    public void add(final String sourceMember, final Set<String> targets, final boolean isValidTargetSymbol) {
        checkImmuted();
        fOneMaps.add(new OneMap(sourceMember, targets, isValidTargetSymbol));
    }

    public void setImmutable() {
        this.fImmuted = true;
    }

    private void checkImmuted() {
        if (fImmuted) {
            throw new ImmutedException();
        }
    }

    public String getStory() {
    
        Set<String> uniq = new HashSet<String>();
        int cnt_with_at_least_one_match = 0;
        for (int i = 0; i < fOneMaps.size(); i++) {
            OneMap one = fOneMaps.get(i);
            uniq.addAll(one.mappedProbeNames);
            if (one.size() > 0) {
                cnt_with_at_least_one_match++;
            }
        }
    
        NullSafeStringBuffer buf = new NullSafeStringBuffer(false);
        buf.append("Mapping for gene set: " + fGeneSetName).append('\n');
        buf.append("Mapping to " + fTargetChipName).append('\n');
        buf.append("Total number of source probes to map: " + fOneMaps.size()).append('\n');
        buf.append("Total number of target probes got : " + uniq.size()).append('\n');
        buf.append("Total number of source probes actualy used : " + cnt_with_at_least_one_match).append('\n');
        buf.append('\n');
        buf.append('\n');
    
        buf.append("SOURCE_ID\tTARGET_ID(s)\n");
    
        for (OneMap one : fOneMaps) {
            buf.append(one.sourceProbeName).append('\t').append(one.toString()).append('\n');
        }
    
        return buf.toString();
    }

    /**
     * Internal class
     */
    static class OneMap {

        String sourceProbeName;

        Set<String> mappedProbeNames;
        
        boolean isValidTargetSymbol;

        OneMap(final String probeName, final Set<String> mappedProbeNames, final boolean isValidTargetSymbol) {
            if (probeName == null) {
                throw new IllegalArgumentException("Parameter probeName cannot be null");
            }

            if (mappedProbeNames == null) {
                throw new IllegalArgumentException("Parameter mappedProbeNames cannot be null");
            }


            this.sourceProbeName = probeName;
            this.mappedProbeNames = mappedProbeNames;
            this.isValidTargetSymbol = isValidTargetSymbol;
        }

        public int size() {
            if (mappedProbeNames != null) {
                return mappedProbeNames.size();
            } else {
                return 0;
            }
        }

        public String toString() {
            if (!isValidTargetSymbol) {
                return "Invalid probe for source chip";
            }
            if (mappedProbeNames == null || mappedProbeNames.isEmpty()) {
                return "Invalid probe for source chip";
            }

            String sep = "";
            StringBuilder buf = new StringBuilder();
            for (Object o : mappedProbeNames) {
                buf.append(sep).append(o.toString());
                sep = ",";
            }
            return buf.toString();
        }
    }
}