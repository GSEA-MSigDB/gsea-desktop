/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.vdb.map;

import edu.mit.broad.genome.objects.PersistentObject;
import edu.mit.broad.vdb.chip.Chip;

/**
 * @author Aravind Subramanian
 */
public interface Chip2ChipMapper extends PersistentObject, Mapper {

    public String getChipsId();

    public int getNumSourceProbes();

    public MappingDbType getMappingDbType();

    public Chip getSourceChip();

    public Chip getTargetChip();

    public boolean equals(final Chip sourceChip, final Chip targetChip, final MappingDbType db);

    public boolean equals(final Chip sourceChip, final Chip targetChip);

    public String[] getSourceProbes();

} // End interface Chip2ChipMapper
