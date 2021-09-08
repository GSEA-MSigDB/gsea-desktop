/*
 * Copyright (c) 2003-2021 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.objects.esmatrix.db;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import edu.mit.broad.genome.alg.ComparatorFactory;
import edu.mit.broad.genome.alg.Metric;
import edu.mit.broad.genome.alg.gsea.EdbAlgs;
import edu.mit.broad.genome.alg.markers.PermutationTest;
import edu.mit.broad.genome.math.Order;
import edu.mit.broad.genome.math.SortMode;
import edu.mit.broad.genome.math.Vector;
import edu.mit.broad.genome.math.XMath;
import edu.mit.broad.genome.objects.AbstractObject;
import edu.mit.broad.genome.objects.Dataset;
import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.objects.LabelledVector;
import edu.mit.broad.genome.objects.RankedList;
import edu.mit.broad.genome.objects.Template;
import edu.mit.broad.genome.objects.strucs.FdrStruc;

/**
 * @author Aravind Subramanian, David Eby
 */
public class EnrichmentDb extends AbstractObject {
	private Metric fMetric;
	private Map<String, Boolean> fMetricParams;
	private SortMode fSortMode;
	private Order fOrder;
	private int fNumPerms;
	private File fEdbDir_opt;
    private PermutationTest fPermTest;

	private EnrichmentResult[] fResults;

	private Map<String, EnrichmentResult> fGeneSetNameResultMap;

	private RankedList fRankedList_shared;

	// @note na for pre-ranked list based rnds
	private Template fTemplate_opt_shared;

	private Dataset fDataset_shared;

	public EnrichmentDb(final String name, final RankedList ranked_list_shared,
			final Dataset ds_shared_opt, final Template template_shared_opt, final EnrichmentResult[] results,
			final Metric metric, final Map<String, Boolean> metricParams, final SortMode sort, final Order order, 
			final int numPerm, final File edb_dir_opt, final PermutationTest ptest_opt) {
		if (results == null) {
			throw new IllegalArgumentException("Param results cannot be null");
		}
		if (ranked_list_shared == null) {
			throw new IllegalArgumentException("Shared ranked list cannot be null");
		}
		if (metric == null) {
			throw new IllegalArgumentException("Param metric cannot be null");
		}
		if (sort == null) {
			throw new IllegalArgumentException("Param sort cannot be null");
		}
		if (order == null) {
			throw new IllegalArgumentException("Param order cannot be null");
		}

		super.initialize(name);

		this.fResults = results;
		this.fRankedList_shared = ranked_list_shared;
		this.fTemplate_opt_shared = template_shared_opt;
		this.fDataset_shared = ds_shared_opt;
		this.fMetric = metric;
		this.fMetricParams = metricParams;
		this.fSortMode = sort;
		this.fOrder = order;
		this.fNumPerms = numPerm;
		this.fEdbDir_opt = edb_dir_opt;
		this.fPermTest = ptest_opt;

		// Enforce that gene sets are unique (barf here)
		this.fGeneSetNameResultMap = EdbAlgs.hashByGeneSetName(getResults());
	}

	public EnrichmentDb cloneDeep(final EnrichmentResult[] results) {
		return new EnrichmentDb(getName(), getRankedList(), getDataset(), getTemplate(), results, getMetric(), 
		        getMetricParams(), getSortMode(), getOrder(), getNumPerm(), getEdbDir(), getPermutationTest());
	}

	public EnrichmentResult getResultForGeneSet(String gsetName_no_aux) {
		Object obj = fGeneSetNameResultMap.get(gsetName_no_aux);
		if (obj == null) {
			throw new IllegalArgumentException("No such gset result: " + gsetName_no_aux);
		} else {
			return (EnrichmentResult) obj;
		}
	}

	public int getNumNominallySig(final float npCutoffInclusive, final boolean pos) {
		int cnt = 0;
		if (pos) {
			for (int i = 0; i < fResults.length; i++) {
				EnrichmentResult res = fResults[i];
				if (res.getScore().getES() > 0) {
					if (res.getScore().getNP() <= npCutoffInclusive) { cnt++; }
				}
			}
		} else {
			for (int i = 0; i < fResults.length; i++) {
				EnrichmentResult res = fResults[i];
				if (res.getScore().getES() < 0) {
					if (res.getScore().getNP() <= npCutoffInclusive) { cnt++; }
				}
			}
		}

		return cnt;
	}

	public EnrichmentResult getResult(final int i) {
		return fResults[i];
	}

	public String getQuickInfo() {
		return null;
	}

	public GeneSet[] getGeneSets() {
		return EdbAlgs.getGeneSets(getResults());
	}

	public EnrichmentResult[] getResults(final Comparator<EnrichmentResult> comp) {
		List<EnrichmentResult> list = Arrays.asList(fResults);
		// TODO: eval for performance.  Could use Arrays.parallelSort()
		// especially since we just return the Results in a new array.
		Collections.sort(list, comp);
		return list.toArray(new EnrichmentResult[list.size()]);
	}

	public EnrichmentResult[] getResults(final boolean pos) {
		final Comparator<EnrichmentResult> comp = (pos) ?
				new ComparatorFactory.EnrichmentResultByNESComparator(Order.DESCENDING) : new ComparatorFactory.EnrichmentResultByNESComparator(Order.ASCENDING);
		final EnrichmentResult[] all = getResults(comp);
		final List<EnrichmentResult> sub = new ArrayList<EnrichmentResult>();
		if (pos) {
			for (int i = 0; i < all.length; i++) {
				float es = all[i].getScore().getES();
				if (XMath.isPositive(es)) { sub.add(all[i]); }
			}
		} else {
			for (int i = 0; i < all.length; i++) {
				float es = all[i].getScore().getES();
				if (XMath.isNegative(es)) { sub.add(all[i]); }
			}
		}

		return sub.toArray(new EnrichmentResult[sub.size()]);
	}

	public int getNumResults() {
		return fResults.length;
	}

	public int getNumScores(final boolean pos) {
		int cnt = 0;
		if (pos) {
			for (int i = 0; i < fResults.length; i++) {
				if (XMath.isPositive(fResults[i].getScore().getES())) { cnt++; }
			}
		} else {
			for (int i = 0; i < fResults.length; i++) {
				if (XMath.isNegative(fResults[i].getScore().getES())) { cnt++; }
			}
		}
		return cnt;
	}

	// lazy init
	private FdrStruc[] fFdrPos;
	private FdrStruc[] fFdrNeg;

	private void initFDR() {
		if (fFdrPos != null) return;

		final List<FdrStruc> pos = new ArrayList<FdrStruc>();
		final List<FdrStruc> neg = new ArrayList<FdrStruc>();

		for (int i = 0; i < fResults.length; i++) {
			final FdrStruc fdr = fResults[i].getFDR();
			if (fdr != null) {
				if (XMath.isPositive(fdr.getRealScore())) {
					pos.add(fdr);
				} else {
					neg.add(fdr);
				}
			}
		}

		this.fFdrPos = pos.toArray(new FdrStruc[pos.size()]);
		this.fFdrNeg = neg.toArray(new FdrStruc[neg.size()]);
	}

	public int getNumFDRSig(final float fdrCutoffInclusive, final boolean pos) {
		final FdrStruc[] fdrs = getFDR(pos);
	
		final List<FdrStruc> list = new ArrayList<FdrStruc>();
		for (int i = 0; i < fdrs.length; i++) {
			if (fdrs[i].getFdr() <= fdrCutoffInclusive) {
				list.add(fdrs[i]);
			}
		}
	
		return list.toArray(new FdrStruc[list.size()]).length;
	}

	public FdrStruc[] getFDR(boolean pos) {
		initFDR();
		
		// Unless we have to worry about thread-safety, the following should be unnecessary.
		// HOWEVER, if we DO need to worry about thread-safety, then we'd better protect these arrays.
		// TODO: need to evaluate this.
		if (pos && fFdrPos == null) {
			throw new IllegalStateException("Fdrs not yet calculated: " + pos);
		}

		if (!pos && fFdrNeg == null) {
			throw new IllegalStateException("Fdrs not yet calculated: " + pos);
		}

		if (pos) {
			return fFdrPos;
		} else {
			return fFdrNeg;
		}
	}

	// @todo using gse names for the lss is not always ok
	public LabelledVector getESS_lv() {
		final Vector ess = new Vector(fResults.length);
		for (int i = 0; i < fResults.length; i++) {
			ess.setElement(i, fResults[i].getScore().getES());
		}
		String[] names = EdbAlgs.getGeneSetNames(fResults);

		return new LabelledVector(getName() + "_ess", names, ess);
	}

	public Vector getNESS() {
		final Vector ness = new Vector(fResults.length);
		for (int i = 0; i < fResults.length; i++) {
			ness.setElement(i, fResults[i].getScore().getNES());
		}

		return ness;
	}

	public Vector getNPs() {
		final Vector nps = new Vector(fResults.length);
		for (int i = 0; i < fResults.length; i++) {
			nps.setElement(i, fResults[i].getScore().getNP());
		}

		return nps;
	}

	public Vector getFDRs() {
		final Vector fdrs = new Vector(fResults.length);
		for (int i = 0; i < fResults.length; i++) {
			fdrs.setElement(i, fResults[i].getScore().getFDR());
		}

		return fdrs;
	}

	public Metric getMetric() {
		return fMetric;
	}

	public Map<String, Boolean> getMetricParams() {
		return fMetricParams;
	}

	public SortMode getSortMode() {
		return fSortMode;
	}

	public Order getOrder() {
		return fOrder;
	}

	public int getNumPerm() {
		return fNumPerms;
	}

	public File getEdbDir() {
		return fEdbDir_opt;
	}

	// THESE because its a "shared" impl
	public Template getTemplate() {
		return fTemplate_opt_shared;
	}

	public Dataset getDataset() {
		return fDataset_shared;
	}

	public RankedList getRankedList() {
		return fRankedList_shared;
	}

	public EnrichmentResult[] getResults() {
		return fResults;
	}

    // na for tag rnd
    public PermutationTest getPermutationTest() {
        return fPermTest;
    }
}
