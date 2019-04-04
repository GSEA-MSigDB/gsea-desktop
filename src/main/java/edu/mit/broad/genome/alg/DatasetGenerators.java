/*******************************************************************************
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.alg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import xtools.api.param.BadParamException;
import edu.mit.broad.genome.NamingConventions;
import edu.mit.broad.genome.alg.distrib.RangeFactory;
import edu.mit.broad.genome.math.ColorScheme;
import edu.mit.broad.genome.math.Matrix;
import edu.mit.broad.genome.math.Order;
import edu.mit.broad.genome.math.Range;
import edu.mit.broad.genome.math.SortMode;
import edu.mit.broad.genome.math.Vector;
import edu.mit.broad.genome.math.XMath;
import edu.mit.broad.genome.objects.Annot;
import edu.mit.broad.genome.objects.AnnotImpl;
import edu.mit.broad.genome.objects.ColorDataset;
import edu.mit.broad.genome.objects.ColorDatasetImpl;
import edu.mit.broad.genome.objects.Dataset;
import edu.mit.broad.genome.objects.DefaultDataset;
import edu.mit.broad.genome.objects.FSet;
import edu.mit.broad.genome.objects.FeatureAnnotImpl;
import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.objects.RankedList;
import edu.mit.broad.genome.objects.ScoredDataset;
import edu.mit.broad.genome.objects.Template;
import edu.mit.broad.genome.objects.TemplateFactory;
import edu.mit.broad.genome.objects.strucs.DatasetTemplate;
import edu.mit.broad.vdb.VdbRuntimeResources;
import edu.mit.broad.vdb.chip.Chip;
import edu.mit.broad.vdb.chip.NullSymbolMode;

/**
 * Methods to generate datasets in various ways including:
 * 1) from artificial fdr
 * 2) by splitting in pre-existing datasets
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class DatasetGenerators {

    private final Logger log = Logger.getLogger(DatasetGenerators.class);

    /**
     * Class constructor
     */
    public DatasetGenerators() {
    }


    public ColorDataset createColorDataset(final int numRanges, final RankedList rl, final ColorScheme cs) {

        final Range[] ranges = RangeFactory.createRanges(numRanges, 0, rl.getSize());

        final Vector v_full = rl.getScoresV(false);

        // Generate a color dataset
        Matrix m = new Matrix(1, ranges.length);
        for (int c = 0; c < ranges.length; c++) {
            m.setElement(0, c, (float) v_full.mean((int) ranges[c].getMin(), (int) ranges[c].getMax()));
        }


        return new ColorDatasetImpl(new DefaultDataset("foo", m, null), cs);
    }

    public Dataset collapse(final Dataset origDs,
                            final Chip chip,
                            final boolean includeOnlySymbols,
                            final int collapse_gex_mode) {

        return collapse_core(origDs, chip, includeOnlySymbols, collapse_gex_mode).symbolized;
    }

    // collapse_gex_mode -> 0 max vector
    // 1 -> median
    // @note IMP: because the chip files are already processed to remove aliases etc we dont have to do a alias lookup
    // simply do it against the gene symbol chip

    /**
     * Converts an uncollapsed dataset, where rows correspond to probes, to collapsed dataset where row
     * corresponds to a gene.
     *
     * @param origDs
     * @param chip
     * @param includeOnlySymbols  whether to omit features with no symbol match
     * @param collapse_gex_mode   collapsing mode for when multiple probes map to a single gene.  0 is max_probe,
     *                            1 is median_of_probes.
     * @return
     */
    public CollapsedDataset collapse_core(final Dataset origDs,
                                          final Chip chip,
                                          final boolean includeOnlySymbols,
                                          final int collapse_gex_mode) {
        if (origDs == null) {
            throw new IllegalArgumentException("Param ds cannot be null");
        }

        if (chip == null) {
            throw new IllegalArgumentException("Param chip cannot be null");
        }

        final Map symbolStrucMap = new HashMap();

        NullSymbolMode nm;
        if (includeOnlySymbols) {
            nm = Chip.OMIT_NULLS;
        } else {
            nm = Chip.REPLACE_WITH_PROBEID;
        }

        for (int r = 0; r < origDs.getNumRow(); r++) {
            String ps = origDs.getRowName(r);
            String symbol = chip.getSymbol(ps, nm);
            String title = chip.getTitle(ps, nm);

            if (symbol != null && includeOnlySymbols && ps.equals(symbol)) {
                // chip returned a matching symbol, we include only symbols, but that
                // symbol is equal to the probe id
                // @todo hack as the symbol lookup isn't always removed
            } else {

                if (symbol != null) {
                    Object obj = symbolStrucMap.get(symbol);
                    if (obj == null) {
                        // Note: we only save the *first* title, so if they differ the subsequent
                        // ones are ignored.
                        obj = new CollapseStruc(symbol, title);
                    }
                    ((CollapseStruc) obj).add(ps);
                    symbolStrucMap.put(symbol, obj);
                }
            }

        }

        // symbolStructMap is a mapping of present symbol names to CollapseStruc objects, where
        // CollapseStruc object identifies the collection of probes in the original dataset that
        // were found to map to the symbol.

        Matrix m = new Matrix(symbolStrucMap.size(), origDs.getNumCol());
        List rowNames = new ArrayList();
        List<String> rowDescs = new ArrayList<String>();
        Iterator it = symbolStrucMap.keySet().iterator();
        int row = 0;
        while (it.hasNext()) {
            Object o = it.next();
            CollapseStruc collapseStruc = (CollapseStruc) symbolStrucMap.get(o);
            rowNames.add(collapseStruc.symbol);
            rowDescs.add(collapseStruc.title);

            final String[] pss = collapseStruc.getProbes();
            if (pss.length == 1) {
                String ps = pss[0];
                //System.out.println("checking for: " + ps);
                m.setRow(row, origDs.getRow(ps));
            } else {
                // multiple probes mapped to this symbol
                Vector[] vss = origDs.getRows(new FSet("foo", "foo", pss));
                if (collapse_gex_mode == 0) {
                    // use max of probe values
                    m.setRow(row, XMath.maxVector(vss));
                } else {
                    // use median of probe values
                    m.setRow(row, XMath.medianVector(vss));
                }
            }
            row++;
        }

        String name = origDs.getName() + "_collapsed_to_symbols";
        Annot annot = new AnnotImpl(new FeatureAnnotImpl(name, rowNames, rowDescs,
                chip), origDs.getAnnot().getSampleAnnot_global());

        CollapsedDataset cds = new CollapsedDataset();
        cds.orig = origDs;
        cds.symbolized = new DefaultDataset(name, m, rowNames, origDs.getColumnNames(), true, annot);
        cds.symbolCollapseStrucMap = symbolStrucMap;
        return cds;
    }

    // collapse_gex_mode -> 0 max vector
    // 1 -> median
    public RankedList collapse(final RankedList origRL,
                               final Chip chip,
                               final boolean includeOnlySymbols,
                               final int collapse_gex_mode) {
        if (origRL == null) {
            throw new IllegalArgumentException("Param origRL cannot be null");
        }

        if (chip == null) {
            throw new IllegalArgumentException("Param chip cannot be null");
        }

        final Map symbolStrucMap = new HashMap();

        NullSymbolMode nm;
        if (includeOnlySymbols) {
            nm = Chip.OMIT_NULLS;
        } else {
            nm = Chip.REPLACE_WITH_PROBEID;
        }

        for (int r = 0; r < origRL.getSize(); r++) {
            final String ps = origRL.getRankName(r);
            final String symbol = chip.getSymbol(ps, nm);
            String title = chip.getTitle(ps, nm);

            if (symbol != null && includeOnlySymbols && ps.equals(symbol)) {
                // @todo hack as the symbol lookup isnt always removed
            } else {

                if (symbol != null) {
                    Object obj = symbolStrucMap.get(symbol);
                    if (obj == null) {
                        // Note: we only save the *first* title, so if they differ the subsequent
                        // ones are ignored.
                        obj = new CollapseStruc(symbol, title);
                    }
                    ((CollapseStruc) obj).add(ps);
                    symbolStrucMap.put(symbol, obj);
                }
            }
        }

        final Vector cl_scores = new Vector(symbolStrucMap.size());
        final List cl_rowNames = new ArrayList();
        final Iterator it = symbolStrucMap.keySet().iterator();

        int row = 0;
        while (it.hasNext()) {
            final Object o = it.next();
            final CollapseStruc collapseStruc = (CollapseStruc) symbolStrucMap.get(o);
            cl_rowNames.add(collapseStruc.symbol);
            final String[] pss = collapseStruc.getProbes();
            if (pss.length == 1) {
                String ps = pss[0];
                //System.out.println("checking for: " + ps);
                cl_scores.setElement(row, origRL.getScore(ps));
            } else {
                float[] fss = origRL.getScores(new FSet("foo", "foo", pss));
                //m.setRow(row, XMath.meanVector(vss));
                //m.setRow(row, XMath.medianVector(vss));
                if (collapse_gex_mode == 0) {
                    cl_scores.setElement(row, XMath.max(fss));
                } else {
                    cl_scores.setElement(row, XMath.median(fss));
                }
            }
            row++;
        }

        String newName = origRL.getName() + "_collapsed";

        if (cl_scores.getSize() == 0) {
            throw new BadParamException("The collapsed dataset was empty when used with chip:" + chip.getName(), 1005);
        }

        return RankedListGenerators.sortByVectorAndGetRankedList(cl_scores, SortMode.REAL, Order.DESCENDING, cl_rowNames).cloneShallowRL(newName);
        // cant do this ad the scores might have got re-arranged
        //return new DefaultRankedList(name, cl_rowNames, cl_scores, true, true);
    }

    public static class CollapsedDataset {
        public Dataset symbolized;
        public Dataset orig;
        public Map symbolCollapseStrucMap;
    }

    public static class CollapseStruc {

        String symbol;
        String title;
        Set probes;

        CollapseStruc(String symbol, String title) {
            this.symbol = symbol;
            this.title = title;
            this.probes = new HashSet();
        }

        private void add(String ps) {
            this.probes.add(ps);
        }

        public String[] getProbes() {
            return (String[]) probes.toArray(new String[probes.size()]);
        }

        public String toString() {
            return symbol;
        }

        public int hashCode() {
            return symbol.hashCode();
        }

        public boolean equals(Object obj) {
            return symbol.equals(obj);
        }

    } // End class CollapseStruc

    public DatasetTemplate extract(final Dataset fullDs, final Template template) {
        return extract(fullDs, template, true);
    }

    public static synchronized DatasetTemplate extract(final Dataset fullDs,
                                                       final Template origT,
                                                       final boolean verbose) {
        return TemplateFactory.extract(fullDs, origT, verbose);
    }

    /**
     * Create a new dataset with profile data only for specified probes
     */
    public Dataset extractRows(final String newName, final Dataset ds, final List rowNames) {

        if (newName == null) {
            throw new IllegalArgumentException("Parameter newName cannot be null");
        }

        if (rowNames == null) {
            throw new IllegalArgumentException("Parameter probenames cannot be null");
        }

        if (ds == null) {
            throw new IllegalArgumentException("Parameter fullDs cannot be null");
        }

        DatasetBuilder builder = new DatasetBuilder(newName, ds.getColumnNames());
        int misscnt = 0;
        List hitNames = new ArrayList();

        for (int i = 0; i < rowNames.size(); i++) {
            String probeName = (String) rowNames.get(i);

            //System.out.println("### looking up probe name>" + (String)probenames.get(i) + "<");
            int index = ds.getRowIndex(probeName);
            //System.out.println("GOT =" + index);

            if (index == -1) {
                //log.debug("No match in dataset for probe: " + probenames.get(i));
                //System.out.println("Missing: " + probenames.get(i));
                misscnt++;
            } else {
                hitNames.add(probeName);
                builder.addRow(index, ds);
            }
        }

        if (misscnt != 0) {
            log.warn("Not all probes had matches. Total probes:" + rowNames.size()
                    + " missing number:" + misscnt + " hits:" + hitNames.size());
        }

        // need tro

        return builder.generate(ds.getAnnot());
    }

    public Dataset extractRows(final Dataset fullDs, final GeneSet gset) {
        if (gset == null) {
            throw new IllegalArgumentException("Parameter gset cannot be null");
        }

        if (fullDs == null) {
            throw new IllegalArgumentException("Parameter fullDs cannot be null");
        }

        String name = NamingConventions.generateName(fullDs, gset, true);
        return extractRows(name, fullDs, gset.getMembers());
    }

    public Dataset extractRows(final Dataset ds, final List rowNames) {
        if (rowNames == null) {
            throw new IllegalArgumentException("Parameter names cannot be null");
        }

        if (ds == null) {
            throw new IllegalArgumentException("Parameter fullDs cannot be null");
        }

        String name = ds.getName() + "_nrows" + rowNames.size();
        return extractRows(name, ds, rowNames);
    }

    public Dataset extractRowsSorted(final ScoredDataset fullSds, final GeneSet gset) {

        if (gset == null) {
            throw new IllegalArgumentException("Parameter fset cannot be null");
        }

        if (fullSds == null) {
            throw new IllegalArgumentException("Parameter fullSds cannot be null");
        }

        // the trick is to order the gene set by the scores in the sds
        // then the usual extract method picks up rows in the same order as the fset
        GeneSet ofset = new FSet(gset, fullSds);

        //System.out.println(">>>> " + ofset.getMembers());

        String name = NamingConventions.generateName(fullSds, gset, true);
        return extractRows(name, fullSds, ofset.getMembers());
    }

}    // End DatasetGenerators
