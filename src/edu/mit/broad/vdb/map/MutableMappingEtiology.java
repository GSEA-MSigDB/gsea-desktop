/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.vdb.map;

import edu.mit.broad.genome.reports.NullSafeStringBuffer;
import edu.mit.broad.genome.utils.ImmutedException;
import edu.mit.broad.vdb.chip.Chip;

import java.util.*;

/**
 * @author Aravind Subramanian
 */
public class MutableMappingEtiology implements MappingEtiology {

    private boolean fImmuted;

    private List fOneMaps;

    private String fGeneSetName;
    private String fSourceChipName;
    private String fTargetChipName;
    private MappingDbType fMapDbType;

    /**
     * Class constructor
     *
     * @param sourceChipName
     * @param targetChipName
     * @param mdb
     */
    public MutableMappingEtiology(final String gsetName,
                                  final String sourceChipName,
                                  final String targetChipName,
                                  final MappingDbType mdb) {

        this.fGeneSetName = gsetName;
        this.fSourceChipName = sourceChipName;
        this.fTargetChipName = targetChipName;
        this.fMapDbType = mdb;
        this.fOneMaps = new ArrayList();
    }

    public MutableMappingEtiology(final String gsetName,
                                  final Chip sourceChip,
                                  final Chip targetChip,
                                  final MappingDbType mdb) {

        this(gsetName, sourceChip.getName(), targetChip.getName(), mdb);
    }

    public String getName() {
        return fGeneSetName;
    }

    public int getTotalNumOfSourceProbesActuallyMapped() {

        int cnt_with_at_least_one_match = 0;
        for (int i = 0; i < fOneMaps.size(); i++) {
            OneMap one = (OneMap) fOneMaps.get(i);
            if (one.mappedProbeNamesSetOrString.size() > 0) {
                cnt_with_at_least_one_match++;
            }
        }

        return cnt_with_at_least_one_match;
    }

    public void add(final String sourceProbeName, final Set targets, final boolean isValidSourceProbe) {
        checkImmuted();
        fOneMaps.add(new OneMap(sourceProbeName, targets, isValidSourceProbe));
    }

    public void add(final String sourceProbeName, final Object target, final boolean isValidSourceProbe) {
        checkImmuted();

        if (target == null) {
            fOneMaps.add(new OneMap(sourceProbeName, Collections.EMPTY_SET, isValidSourceProbe));
        } else if (target instanceof String) {
            add(sourceProbeName, target.toString(), isValidSourceProbe);
        } else if (target instanceof Set) {
            fOneMaps.add(new OneMap(sourceProbeName, (Set) target, isValidSourceProbe));
        } else {
            throw new IllegalStateException("Unnown target object: " + target + " class: " + target.getClass());
        }

    }

    public void add(final String sourceProbeName, final String target, final boolean isValidSourceProbe) {
        checkImmuted();
        fOneMaps.add(new OneMap(sourceProbeName, target, isValidSourceProbe));
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
    
        Set uniq = new HashSet();
        int cnt_with_at_least_one_match = 0;
        for (int i = 0; i < fOneMaps.size(); i++) {
            OneMap one = (OneMap) fOneMaps.get(i);
            uniq.addAll(one.mappedProbeNamesSetOrString.set());
            //cnt += one.mappedProbeNamesSetOrString.size();
            if (one.mappedProbeNamesSetOrString.size() > 0) {
                cnt_with_at_least_one_match++;
            }
        }
    
        NullSafeStringBuffer buf = new NullSafeStringBuffer(false);
        buf.append("Mapping for gene set: " + fGeneSetName).append('\n');
        buf.append("Mapping from: " + fSourceChipName + " to " + fTargetChipName).append('\n');
        buf.append("Mapping database used: " + fMapDbType.getName()).append('\n');
        buf.append("Total number of source probes to map: " + fOneMaps.size()).append('\n');
        buf.append("Total number of target probes got : " + uniq.size()).append('\n');
        buf.append("Total number of source probes actualy used : " + cnt_with_at_least_one_match).append('\n');
        buf.append('\n');
        buf.append('\n');
    
        buf.append("SOURCE_ID\tTARGET_ID(s)\n");
    
        for (int i = 0; i < fOneMaps.size(); i++) {
            OneMap one = (OneMap) fOneMaps.get(i);
            buf.append(one.sourceProbeName).append('\t').append(one.mappedProbeNamesSetOrString.toString()).append('\n');
        }
    
        return buf.toString();
    }

    /**
     * Internal class
     */
    static class OneMap {

        String sourceProbeName;

        State mappedProbeNamesSetOrString;

        OneMap(final String sourceProbeName,
               final String mappedProbeName,
               final boolean isValidSourceProbe) {
            this.sourceProbeName = sourceProbeName;
            this.mappedProbeNamesSetOrString = new State(mappedProbeName, isValidSourceProbe);
        }

        OneMap(final String probeName, final Set mappedProbeNames, final boolean isValidSourceProbe) {
            if (probeName == null) {
                throw new IllegalArgumentException("Parameter probeName cannot be null");
            }

            if (mappedProbeNames == null) {
                throw new IllegalArgumentException("Parameter mappedProbeNames cannot be null");
            }


            this.sourceProbeName = probeName;
            this.mappedProbeNamesSetOrString = new State(mappedProbeNames, isValidSourceProbe);
        }

    } // End class OneMap

    static class State {

        Object obj;

        State(Object targeProbeNameOrTargetProbeNameSet, boolean isValidSourceProbe) {
            if (targeProbeNameOrTargetProbeNameSet == null) {
                obj = Boolean.valueOf(isValidSourceProbe);
            } else
            if (targeProbeNameOrTargetProbeNameSet instanceof Set && ((Set) targeProbeNameOrTargetProbeNameSet).isEmpty()) {
                obj = Boolean.valueOf(isValidSourceProbe);
            } else {
                obj = targeProbeNameOrTargetProbeNameSet;
            }
        }

        public int size() {
            if (obj instanceof String) {
                return 1;
            } else if (obj instanceof Set) {
                return ((Set) obj).size();
            } else {
                return 0;
            }
        }

        public Set set() {
            Set set = new HashSet();
            if (obj instanceof String) {
                set.add(obj);
            } else if (obj instanceof Set) {
                set.addAll((Set) obj);
            }

            return set;
        }

        public String toString() {
            if (obj instanceof String) {
                return obj.toString();
            } else if (obj instanceof Set) {
                StringBuffer buf = new StringBuffer();
                for (Iterator iterator = ((Set) obj).iterator(); iterator.hasNext();) {
                    Object o = iterator.next();
                    buf.append(o.toString());
                    if (iterator.hasNext()) {
                        buf.append(',');
                    }
                }
                return buf.toString();
            } else if (obj instanceof Boolean) {
                boolean isValidSourceProbe = ((Boolean) obj).booleanValue();
                if (isValidSourceProbe) {
                    return "No matches";
                } else {
                    return "Invalid probe for source chip";
                }
            } else {
                throw new IllegalArgumentException("Unkniwn object: " + obj);
            }
        }
    }

} // End class MutableMappingEtiology
