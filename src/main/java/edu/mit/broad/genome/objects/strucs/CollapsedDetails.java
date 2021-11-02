/*
 * Copyright (c) 2003-2021 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.objects.strucs;

import java.util.Map;

import edu.mit.broad.genome.alg.DatasetGenerators.CollapseStruc;
import edu.mit.broad.genome.objects.Dataset;
import edu.mit.broad.genome.objects.PersistentObject;
import edu.mit.broad.genome.objects.RankedList;
import edu.mit.broad.vdb.chip.Chip;

/**
 * @author Aravind Subramanian, David Eby
 */
public class CollapsedDetails {
    public PersistentObject orig;
    public PersistentObject collapsed;
    public boolean wasCollapsed;
    public Chip chip;

    public String getChipName() {
        if (chip != null) {
            return chip.getName();
        } else {
            return "NA";
        }
    }

    public int getNumRow_orig() {
        if (orig instanceof Dataset) {
            return ((Dataset) orig).getNumRow();
        } else {
            return ((RankedList) orig).getSize();
        }
    }

    public int getNumRow_collapsed() {
        if (collapsed instanceof Dataset) {
            return ((Dataset) collapsed).getNumRow();
        } else {
            return ((RankedList) collapsed).getSize();
        }
    }

    public static class Data extends CollapsedDetails {
        public Dataset getDataset() {
            if (wasCollapsed) {
                return (Dataset) collapsed;
            } else {
                return (Dataset) orig;
            }
        }
    }

    public static class Ranked extends CollapsedDetails {
        public Map<String, CollapseStruc> collapseStrucMap;
        
        public RankedList getRankedList() {
            if (wasCollapsed) {
                return (RankedList) collapsed;
            } else {
                return (RankedList) orig;
            }
        }
    }
}