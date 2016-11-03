/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.vdb.map;

import edu.mit.broad.genome.objects.AbstractObject;
import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.objects.GeneSetMatrix;
import edu.mit.broad.vdb.chip.Chip;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Aravind Subramanian
 */
public class Chip2ChipMapperSymbol extends AbstractObject implements Chip2ChipMapper {

    private Chip fSourceChip;

    private Chip fTargetChip;

    /**
     * Class constructor
     *
     * @param sourceChip
     * @param targetChip
     */
    public Chip2ChipMapperSymbol(final Chip sourceChip, final Chip targetChip) {

        if (sourceChip == null) {
            throw new IllegalArgumentException("Param sourceChip cannot be null");
        }
        if (targetChip == null) {
            throw new IllegalArgumentException("Param targetChip cannot be null");
        }

        System.out.println("##### source: " + sourceChip.getName() + " target: " + targetChip.getName());


        this.fSourceChip = sourceChip;
        this.fTargetChip = targetChip;

        super.initialize(MapUtils.createId(fSourceChip, fTargetChip, getMappingDbType()));
    }

    public String getQuickInfo() {
        return null;
    }

    public String getChipsId() {
        return MapUtils.createId(fSourceChip, fTargetChip);
    }

    public int getNumSourceProbes() {
        try {
            return fSourceChip.getNumProbes();
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public MappingDbType getMappingDbType() {
        return MappingDbTypes.GENE_SYMBOL;
    }

    public Chip getSourceChip() {
        return fSourceChip;
    }

    public Chip getTargetChip() {
        return fTargetChip;
    }

    public String[] getSourceProbes() {
        try {
            return fSourceChip.getProbeNamesArr();
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public boolean equals(Chip sourceChip, Chip targetChip, MappingDbType db) {
        return MapUtils.equals(sourceChip, targetChip, db,
                fSourceChip, fTargetChip, MappingDbTypes.GENE_SYMBOL.getName());
    }

    public boolean equals(Chip sourceChip, Chip targetChip) {
        return MapUtils.equals(sourceChip, targetChip, fSourceChip, fTargetChip);
    }

    // ------------------------------------------------------------------------------ //
    // ---------------------------- MAPPING RELATED --------------------------------- //
    // ------------------------------------------------------------------------------ //


    public Set map(final String sourceProbeName) throws Exception {

        Set targetProbes = new HashSet();
        // first add directly if a direct probe match
        if (fTargetChip.isProbe(sourceProbeName)) {
            targetProbes.add(sourceProbeName); // should i simply return here ?
        }

        // Then do a symbol based lookup
        String symbol = fSourceChip.getSymbol(sourceProbeName, Chip.OMIT_NULLS);
        if (symbol != null) {
            Set set = fTargetChip.getProbeNames(symbol);
            targetProbes.addAll(set);
        }

        //System.out.println(">>> MAPPING " + sourceProbeName + " >" + symbol + " >" + fTargetChip.getProbeNames(symbol) + " source >" + fSourceChip.getName() + "< target>" + fTargetChip.getName());

        return targetProbes;
    }

    public MGeneSet map(final GeneSet sourceGeneSet, final boolean maintainEtiology) throws Exception {
        return new MGeneSetImpl(sourceGeneSet, maintainEtiology, fSourceChip.getName(), fTargetChip.getName(), MappingDbTypes.GENE_SYMBOL, this);
    }

    public MGeneSetMatrix map(final GeneSetMatrix sourceGm, final boolean maintainEtiology) throws Exception {
        return new MGeneSetMatrixImpl(sourceGm, maintainEtiology, fSourceChip.getName(), fTargetChip.getName(), MappingDbTypes.GENE_SYMBOL, this);
    }

} // End class SymbolMappedChip
