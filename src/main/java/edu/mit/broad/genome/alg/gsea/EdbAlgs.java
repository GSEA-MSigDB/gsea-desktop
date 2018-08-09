/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.alg.gsea;

import edu.mit.broad.genome.Errors;
import edu.mit.broad.genome.MismatchedSizeException;
import edu.mit.broad.genome.math.Matrix;
import edu.mit.broad.genome.math.Vector;
import edu.mit.broad.genome.objects.*;
import edu.mit.broad.genome.objects.esmatrix.db.EnrichmentResult;

import java.util.*;
import java.util.List;

/**
 * @author Aravind Subramanian
 */
public class EdbAlgs {

    public static LabelledVector createRealES(final EnrichmentResult[] results) {
        _nonNull(results);

        Vector v = new Vector(results.length);
        List labels = new ArrayList();
        for (int i = 0; i < results.length; i++) {
            labels.add(results[i].getGeneSetName());
            v.setElement(i, results[i].getScore().getES());
        }

        return new LabelledVector(labels, v, true);
    }

    public static Dataset createRndESDataset(final String name, final EnrichmentResult[] results) {

        _nonNull(results);

        // each gset on a row, cols are perms
        int numPerms = enforceSameNumOfPerms(results);
        Matrix m = new Matrix(results.length, numPerms);

        List rowNames = new ArrayList(results.length);

        for (int r = 0; r < results.length; r++) {
            Vector v = results[r].getRndESS();
            m.setRow(r, v);
            rowNames.add(results[r].getGeneSetName());
        }

        return new DefaultDataset(name + "_rnd_es", m, rowNames, _permColNames(numPerms), true, null);
    }

    public static GeneSet[] getGeneSets(final EnrichmentResult[] results) {
        _nonNull(results);

        final GeneSet[] gsets = new GeneSet[results.length];
        for (int r = 0; r < results.length; r++) {
            gsets[r] = results[r].getGeneSet();
            //gsets[r] = gsets[r].cloneShallow(gsets[r].getName(true));
        }

        return gsets;
    }

    private static void _nonNull(final EnrichmentResult[] results) {
        if (results == null || results.length == 0) {
            throw new IllegalArgumentException("Param results cannot be null nor zero length: " + results);
        }
    }

    /**
     * key -> gsetName
     * value -> a single er (barfs if there are duplicates)
     *
     * @param results
     * @return
     */
    public static Map hashByGeneSetName(final EnrichmentResult[] results) {

        _nonNull(results);

        final Map map = new HashMap();

        Errors errors = new Errors();
        for (int i = 0; i < results.length; i++) {
            String name = results[i].getGeneSet().getName(true);
            if (map.containsKey(name)) {
                errors.add("Duplicated gene set: " + name);
            } else {
                map.put(name, results[i]);
            }
        }


        errors.barfIfNotEmptyRuntime();

        return map;
    }

    public static List getGeneSetNames(final EnrichmentResult[] results) {

        _nonNull(results);

        List list = new ArrayList(results.length);

        for (int i = 0; i < results.length; i++) {
            list.add(results[i].getGeneSet().getName(true)); // note strip aux
        }

        return list;
    }

    public static int enforceSameNumOfPerms(final EnrichmentResult[] results) {
        if (results.length == 0) {
            return 0;
        }

        int num = results[0].getNumPerms();
        for (int i = 0; i < results.length; i++) {

            if (results[i].getNumPerms() != num) {
                throw new MismatchedSizeException("enrichment result: " + results[0].getGeneSetName(), num, "enrichment result: " + results[i].getGeneSetName(), results[i].getNumPerms());
            }
        }

        return num;
    }

    private static List _permColNames(int nperms) {
        List colNames = new ArrayList();
        for (int c = 0; c < nperms; c++) {
            colNames.add("perm_" + (c + 1));
        }

        return colNames;
    }

} // End EdbAlgs
