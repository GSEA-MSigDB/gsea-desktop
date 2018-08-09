/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.alg.gsea;

import edu.mit.broad.genome.objects.GeneSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author: Justin Guinney
 */
public class GeneToGeneSetMap {

    private Map _map;

    private GeneToGeneSetMap(Map map) {
        _map = map;
    }

    public int[] getGenesetIndicesForGene(String name) {
        return (int[]) _map.get(name);
    }

    public static GeneToGeneSetMap generateGeneToGenesetMap(final GeneSet[] gsets) {
        HashMap<String, ArrayList<Integer>> map = new HashMap<String, ArrayList<Integer>>();

        for (int gsIdx = 0; gsIdx < gsets.length; ++gsIdx) {
            String[] genes = gsets[gsIdx].getMembersArray();
            for (int i = 0; i < genes.length; ++i) {
                ArrayList<Integer> l = map.get(genes[i]);
                if (l == null) {
                    l = new ArrayList<Integer>();
                    map.put(genes[i], l);
                }
                l.add(gsIdx);
            }
        }

        HashMap<String, int[]> indexMap = new HashMap<String, int[]>();
        Iterator<String> i = map.keySet().iterator();
        while (i.hasNext()) {
            String gene = i.next();
            ArrayList<Integer> genesets = map.get(gene);
            int[] indexArr = new int[genesets.size()];
            for (int j = 0; j < indexArr.length; ++j)
                indexArr[j] = genesets.get(j);
            indexMap.put(gene, indexArr);
        }
        return new GeneToGeneSetMap(indexMap);
    }
}



