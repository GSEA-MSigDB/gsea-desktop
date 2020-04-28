/*
 * Copyright (c) 2003-2020 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.alg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import xtools.api.param.BadParamException;
import edu.mit.broad.genome.NamingConventions;
import edu.mit.broad.genome.alg.distrib.RangeFactory;
import edu.mit.broad.genome.math.ColorSchemes.ColorScheme;
import edu.mit.broad.genome.math.Matrix;
import edu.mit.broad.genome.math.Order;
import edu.mit.broad.genome.math.Range;
import edu.mit.broad.genome.math.SortMode;
import edu.mit.broad.genome.math.StringMatrix;
import edu.mit.broad.genome.math.Vector;
import edu.mit.broad.genome.math.XMath;
import edu.mit.broad.genome.objects.Annot;
import edu.mit.broad.genome.objects.ColorDataset;
import edu.mit.broad.genome.objects.ColorDatasetImpl;
import edu.mit.broad.genome.objects.Dataset;
import edu.mit.broad.genome.objects.DefaultDataset;
import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.objects.FeatureAnnot;
import edu.mit.broad.genome.objects.RankedList;
import edu.mit.broad.genome.objects.ScoredDataset;
import edu.mit.broad.genome.objects.StringDataframe;
import edu.mit.broad.genome.objects.Template;
import edu.mit.broad.genome.objects.TemplateFactory;
import edu.mit.broad.genome.objects.strucs.DatasetTemplate;
import edu.mit.broad.vdb.chip.Chip;
import edu.mit.broad.vdb.chip.NullSymbolMode;
import edu.mit.broad.vdb.chip.NullSymbolModes;

/**
 * Methods to generate datasets in various ways including:
 * 1) from artificial fdr
 * 2) by splitting in pre-existing datasets
 *
 * @author Aravind Subramanian
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


        return new ColorDatasetImpl(new DefaultDataset("foo", m), cs);
    }

    /**
     * Converts an uncollapsed dataset, where rows correspond to probes, to collapsed dataset where row
     * corresponds to a gene.
     *
     * @param origDs
     * @param chip
     * @param includeOnlySymbols  whether to omit features with no symbol match
     * @param collapse_gex_mode   collapsing mode for when multiple probes map to a single gene.  0 is max_probe,
     *                            1 is median_of_probes, 2 is mean_of_probes, 3 is sum_of_probes, 4 is remap_only.
     * @return
     */
    public CollapsedDataset collapse(final Dataset origDs, final Chip chip, final boolean includeOnlySymbols,
                                     final int collapse_gex_mode) {
        if (origDs == null) {
            throw new IllegalArgumentException("Param ds cannot be null");
        }

        CollapsedDataset cds = new CollapsedDataset();
        cds.orig = origDs;

        // symbolStructMap is a mapping of present symbol names to CollapseStruc objects, where
        // CollapseStruc object identifies the collection of probes in the original dataset that
        // were found to map to the symbol.
        final Map<String, CollapseStruc> symbolCollapseStrucMap = cds.symbolCollapseStrucMap;
        populateCollapseStrucMap(origDs.getRowNames(), symbolCollapseStrucMap, chip, includeOnlySymbols, collapse_gex_mode);

        Matrix m = new Matrix(symbolCollapseStrucMap.size(), origDs.getNumCol());
        List<String> rowNames = new ArrayList<String>();
        List<String> rowDescs = new ArrayList<String>();

        int row = 0;
        for (String symbol: symbolCollapseStrucMap.keySet()) {
            CollapseStruc collapseStruc = symbolCollapseStrucMap.get(symbol);
            rowNames.add(collapseStruc.symbol);
            rowDescs.add(collapseStruc.title);

            final String[] pss = collapseStruc.getProbes();
            if (pss.length == 1) {
                String ps = pss[0];
                m.setRow(row, origDs.getRow(ps));
            } else {
                // multiple probes mapped to this symbol
                Vector[] vss = origDs.getRows(new GeneSet("foo", "foo", pss));
                // TODO: This should really be done with an Enum rather than hard-coded index values
                if (collapse_gex_mode == 0) {
                    // use max of probe values
                    m.setRow(row, XMath.maxVector(vss));
                } else if (collapse_gex_mode == 1) {
                    // use median of probe values
                    m.setRow(row, XMath.medianVector(vss));
                } else if (collapse_gex_mode == 2) {
                    // use mean of probe values
                    m.setRow(row, XMath.meanVector(vss));
                } else if (collapse_gex_mode == 3) {
                    // use sum of probe values
                    m.setRow(row, XMath.sumVector(vss));
                } else {
                    // Remapping only.  We consider it an error if multiple probes map when in this mode
                    throw new BadParamException("Multiple rows mapped to the symbol ''" + collapseStruc.symbol
                            + "'.  This is not allowed in Remap_only mode.", 1020);
                }
            }
            row++;
        }

        String name = origDs.getName() + getExtendedName(collapse_gex_mode) + "_to_symbols";
        log.info("Creating collapsed dataset " + name + ", chosen mode " + collapse_gex_mode);
        Annot annot = new Annot(new FeatureAnnot(name, rowNames, rowDescs,
                chip), origDs.getAnnot().getSampleAnnot_global());

        cds.symbolized = new DefaultDataset(name, m, rowNames, origDs.getColumnNames(), annot);
        return cds;
    }
    
	public CollapsedRL collapse(final RankedList origRL,
	                            final Chip chip,
	                            final boolean includeOnlySymbols,
	                            final int collapse_gex_mode) {
	    if (origRL == null) {
	        throw new IllegalArgumentException("Param origRL cannot be null");
	    }
	
		CollapsedRL collapsedRL = new CollapsedRL();
	    collapsedRL.orig = origRL;
	
	    final Map<String, CollapseStruc> symbolCollapseStrucMap = collapsedRL.symbolCollapseStrucMap;
	    populateCollapseStrucMap(origRL.getRankedNames(), symbolCollapseStrucMap, chip, includeOnlySymbols, collapse_gex_mode);
	
	    final Vector cl_scores = new Vector(symbolCollapseStrucMap.size());
	    final List<String> cl_rowNames = new ArrayList<String>();
	
	    int row = 0;
	    for (String symbol: symbolCollapseStrucMap.keySet()) {
	        final CollapseStruc collapseStruc = symbolCollapseStrucMap.get(symbol);
	        cl_rowNames.add(collapseStruc.symbol);
	        final String[] pss = collapseStruc.getProbes();
	        if (pss.length == 1) {
	            String ps = pss[0];
	            cl_scores.setElement(row, origRL.getScore(ps));
	        } else {
	            // TODO: This should really be done with an Enum rather than hard-coded index values
	            float[] fss = origRL.getScores(new GeneSet("foo", "foo", pss));
	            if (collapse_gex_mode == 0) {
	                cl_scores.setElement(row, XMath.max(fss));
	            } else if (collapse_gex_mode == 1) {
	                cl_scores.setElement(row, XMath.median(fss));
	            } else if (collapse_gex_mode == 2) {
	                cl_scores.setElement(row, XMath.mean(fss));
	            } else if (collapse_gex_mode == 3) {
	                cl_scores.setElement(row, XMath.sum(fss));
	            } else {
	                // Remapping only.  We consider it an error if multiple probes map when in this mode
	                throw new BadParamException("Multiple rows mapped to the symbol ''" + collapseStruc.symbol
	                        + "'.  This is not allowed in Remap_only mode.", 1020);
	            }
	        }
	        row++;
	    }
	
	    if (cl_scores.getSize() == 0) {
	        throw new BadParamException("The collapsed dataset was empty when used with chip:" + chip.getName(), 1005);
	    }
	
	    String newName = origRL.getName() + getExtendedName(collapse_gex_mode);
	    RankedList sortedRL = RankedListGenerators.sortByVectorAndGetRankedList(cl_scores, SortMode.REAL, Order.DESCENDING, cl_rowNames).cloneShallowRL(newName);
		collapsedRL.symbolized = sortedRL;
		return collapsedRL;
	}

	private void populateCollapseStrucMap(List<String> origRowNames, final Map<String, CollapseStruc> symbolCollapseStrucMap,
    		final Chip chip, final boolean includeOnlySymbols,
            final int collapse_gex_mode) {

        if (chip == null) {
            throw new IllegalArgumentException("Param chip cannot be null");
        }

        NullSymbolMode nm = (includeOnlySymbols) ? NullSymbolModes.OmitNulls : NullSymbolModes.ReplaceWithProbeId;

        for (String name: origRowNames) {
            String symbol = chip.getSymbol(name, nm);
            if (StringUtils.isNotEmpty(symbol)) {
            	CollapseStruc struc = symbolCollapseStrucMap.get(symbol);
                if (struc == null) {
                    // Note: we only save the *first* title, so if they differ the subsequent
                    // ones are ignored.
                    struc = new CollapseStruc(symbol, chip.getTitle(name, nm));
                    symbolCollapseStrucMap.put(symbol, struc);
                }
                struc.add(name);
            }
        }
    }
    
    private String getExtendedName(final int collapse_gex_mode) {
		return (collapse_gex_mode <= 3) ? "_collapsed" : "_remapped";
	}

	public static class CollapsedDataset {
        public Dataset symbolized;
        public Dataset orig;
        
        final Map<String, CollapseStruc> symbolCollapseStrucMap = new HashMap<String, CollapseStruc>();

    	public StringDataframe makeEtiologySdf() {
            return DatasetGenerators.makeEtiologySdf(symbolCollapseStrucMap, symbolized.getRowNames());
    	}
    }
    
    public static class CollapsedRL {
    	public RankedList symbolized;
    	public RankedList orig;
        public final Map<String, CollapseStruc> symbolCollapseStrucMap = new HashMap<String, CollapseStruc>();

    	public StringDataframe makeEtiologySdf() {
            return DatasetGenerators.makeEtiologySdf(symbolCollapseStrucMap, symbolized.getRankedNames());
    	}
    }

    public static class CollapseStruc {

        String symbol;
        String title;
        Set<String> probes;

        CollapseStruc(String symbol, String title) {
            this.symbol = symbol;
            this.title = title;
            this.probes = new HashSet<String>();
        }

        private void add(String ps) {
            this.probes.add(ps);
        }

        public String[] getProbes() {
            return probes.toArray(new String[probes.size()]);
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

        public String getTitle() {
            return title;
        }
    }

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
    private Dataset extractRows(final String newName, final Dataset ds, final List<String> rowNames) {

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
        List<String> hitNames = new ArrayList<String>();

        for (int i = 0; i < rowNames.size(); i++) {
            String probeName = (String) rowNames.get(i);

            int index = ds.getRowIndex(probeName);

            if (index == -1) {
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

    public Dataset extractRows(final Dataset ds, final List<String> rowNames) {
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
        GeneSet ofset = new GeneSet(gset, fullSds);

        String name = NamingConventions.generateName(fullSds, gset, true);
        return extractRows(name, fullSds, ofset.getMembers());
    }

	private static StringDataframe makeEtiologySdf(Map<String, CollapseStruc> symbolCollapseStrucMap, List<String> names) {
		final String[] colNames = new String[]{"# MATCHING PROBE SETS", "MATCHING PROBE SET(S)"};
        final String[] rowNames = new String[names.size()];
        final StringMatrix sm = new StringMatrix(rowNames.length, colNames.length);
        int r = 0;
        for (String name: names) {
            rowNames[r] = name;
            CollapseStruc cs = symbolCollapseStrucMap.get(name);
            sm.setElement(r, 0, cs.getProbes().length);
            sm.setElement(r, 1, cs.getProbes());
        	r++;
		}

        return new StringDataframe("Symbol_to_probe_set_mapping_details", sm, rowNames, colNames);
	}
}