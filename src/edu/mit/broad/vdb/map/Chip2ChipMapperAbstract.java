/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.vdb.map;

import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.objects.GeneSetMatrix;
import edu.mit.broad.genome.objects.Id;
import edu.mit.broad.vdb.VdbRuntimeResources;
import edu.mit.broad.vdb.chip.Chip;
import gnu.trove.THashMap;

import java.util.Collections;
import java.util.Set;

/**
 * Keep slim and not many refs as this object is serialized
 */
// dont extend abstract object as want to keep this self contained
abstract class Chip2ChipMapperAbstract implements Chip2ChipMapper {

    private static final long serialVersionUID = 3328221101996725699L;

    // @note IMP: DONT USE LOG
    // @note IMP: as this is serialized leep the objects it refers to not very many in number

    // key -> source probe name, value -> set of target probes
    private THashMap fSourceProbeTargetProbesMap;

    private MappingDbType fMappingDbType;

    private String fSourceChipName;

    private String fTargetChipName;

    private String fName;

    /**
     * @param mc
     */
    protected void init(final Chip2ChipMapper mc) {
        Chip2ChipMapperAbstract amc = (Chip2ChipMapperAbstract) mc; // @note assumption
        this.init(amc.fSourceChipName, amc.fTargetChipName, amc.fMappingDbType, amc.fSourceProbeTargetProbesMap);
    }

    /**
     * @param sourceChipName
     * @param targetChipName
     * @param db
     * @param sourceProbeTargetProbesMap
     */
    // subclasses must call
    protected void init(final String sourceChipName,
                        final String targetChipName,
                        final MappingDbType db,
                        final THashMap sourceProbeTargetProbesMap) {

        if (sourceChipName == null) {
            throw new IllegalArgumentException("Param sourceChipName cannot be null");
        }

        if (targetChipName == null) {
            throw new IllegalArgumentException("Param targetChipName cannot be null");
        }

        if (db == null) {
            throw new IllegalArgumentException("Param db cannot be null");
        }

        this.fSourceChipName = sourceChipName;
        this.fTargetChipName = targetChipName;
        this.fMappingDbType = db;
        this.fSourceProbeTargetProbesMap = sourceProbeTargetProbesMap;
        this.fName = MapUtils.createId(fSourceChipName, fTargetChipName, fMappingDbType);

        //TraceUtils.showTrace("fSourceChipName: " + fSourceChipName + " fTargetChipName: " + fTargetChipName);

    }

    public String getNameEnglish() {
        return null;
    }

    // subclasses must implement custom initing if needed
    protected abstract void checkAndInit();


    public String getChipsId() {
        return MapUtils.createId(getSourceChip().getName(), getTargetChip().getName());
    }

    public String getQuickInfo() {
        return null;
    }

    public String getName() {
        return fName;
    }

    public MGeneSet map(final GeneSet sourceGeneSet, final boolean maintainEtiology) throws Exception {
        return new MGeneSetImpl(sourceGeneSet, maintainEtiology, getSourceChip().getName(),
                getTargetChip().getName(), getMappingDbType(), this);
    }

    public MGeneSetMatrix map(final GeneSetMatrix sourceGm, final boolean maintainEtiology) throws Exception {
        return new MGeneSetMatrixImpl(sourceGm, maintainEtiology, getSourceChip().getName(), getTargetChip().getName(), getMappingDbType(), this);
    }

    public Set map(final String sourceProbeName) {
        Object obj = fSourceProbeTargetProbesMap.get(sourceProbeName);
        if (obj == null) {
            return Collections.EMPTY_SET;
        }
        return Collections.unmodifiableSet((java.util.Set) obj);
    }

    public String[] getSourceProbes() {
        return (String[]) fSourceProbeTargetProbesMap.keySet().toArray(new String[fSourceProbeTargetProbesMap.size()]);
    }

    public MappingDbType getMappingDbType() {
        return fMappingDbType;
    }

    public Chip getSourceChip() {
        return VdbRuntimeResources.getChip(fSourceChipName);
    }

    public Chip getTargetChip() {
        return VdbRuntimeResources.getChip(fTargetChipName);
    }

    public int getNumSourceProbes() {
        return fSourceProbeTargetProbesMap.size();
    }

    public boolean equals(final Chip sourceChip, final Chip targetChip, final MappingDbType db) {
        return MapUtils.equals(sourceChip, targetChip, db, getSourceChip(), getTargetChip(), getMappingDbType().getName());
    }

    public boolean equals(final Chip sourceChip, final Chip targetChip) {
        return MapUtils.equals(sourceChip, targetChip, getSourceChip(), getTargetChip());
    }

    /**
     * @return Object id
     */
    public Id getId() {
        return null;
    }

    /**
     * @return The comment, if any, associated with the Dataset
     */
    public String getComment() {
        return "";
    }

    public void addComment(String comment) {
    }

    public boolean hasProperty(String key) {
        return false;
    }

    public String getProperty(String key) {
        return null;
    }

    public void setProperty(String key, String value) {
    }

} // End class Chip2ChipMapperAbstract