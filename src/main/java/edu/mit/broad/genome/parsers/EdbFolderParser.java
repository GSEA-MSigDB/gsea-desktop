/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.parsers;

import edu.mit.broad.genome.Errors;
import edu.mit.broad.genome.Headers;
import edu.mit.broad.genome.NamingConventions;
import edu.mit.broad.genome.Printf;
import edu.mit.broad.genome.alg.Metric;
import edu.mit.broad.genome.alg.Metrics;
import edu.mit.broad.genome.math.*;
import edu.mit.broad.genome.math.Vector;
import edu.mit.broad.genome.objects.*;
import edu.mit.broad.genome.objects.esmatrix.db.*;
import edu.mit.broad.vdb.VdbRuntimeResources;
import edu.mit.broad.vdb.chip.Chip;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.*;
import java.util.*;

/**
 * Called a "folder" parser because works / makes a folder
 */
public class EdbFolderParser extends AbstractParser {

    // cant have tab (would jave been nice as easier to use in excel as when read _in_
    // the dom library seems to convert tabs into spaces
    private static final char DELIM = ' ';

    private static final String EDB = "EDB";

    private static final String DTG = "DTG";

    private static final String ES = "ES";
    private static final String RND_ES = "RND_ES";
    private static final String NP = "NP";
    private static final String FDR = "FDR";
    private static final String NES = "NES";
    private static final String FWER = "FWER";

    private static final String CHIP = "CHIP";
    private static final String HIT_INDICES = "HIT_INDICES";
    private static final String RANK_AT_ES = "RANK_AT_ES";
    private static final String RANK_SCORE_AT_ES = "RANK_SCORE_AT_ES";
    private static final String ES_PROFILE = "ES_PROFILE";

    // well known file names
    private static final String EDB_FILE_NAME = "results.edb";

    public EdbFolderParser() {
        super(EnrichmentDb.class);
    }

    // @note duplicated code below
    protected EnrichmentDb parseEdb(final File gseaResultDir) throws Exception {

        final File edb_dir = _getEdbDir(gseaResultDir);

        final File edb_file = new File(edb_dir, EDB_FILE_NAME);
        if (edb_file.exists() == false) {
            throw new IllegalArgumentException("edb file not found: " + edb_file);
        }

        InputStream is = new FileInputStream(edb_file);
        SAXReader reader = new SAXReader();
        Document document = reader.read(is);
        Element root = document.getRootElement();

        // first ensure that the meg exists
        LabelledVectorProcessor lvp = LabelledVectorProcessors.lookupProcessor(root.attribute(Headers.LV_PROC).getValue());
        SortMode sort = SortMode.lookup(root.attribute(Headers.SORT_MODE).getValue());
        Order order = Order.lookup(root.attribute(Headers.ORDER).getValue());
        Metric metric = Metrics.lookupMetric(root.attribute(Headers.METRIC).getValue());
        int numPerms = Integer.parseInt(root.attribute(Headers.NUM_PERMS).getValue());

        // then onto the elements
        List<EnrichmentResult> dtgs = new ArrayList<EnrichmentResult>();

        int cnt = 0;
        // each element is converted into a Edb.Data Object
        for (Iterator<Element> i = getDomIterator(root); i.hasNext();) {
            Element el = (Element) i.next();

            // @note template na if pre-ranked
            Template template_opt = null;

            try {
                template_opt = ParserFactory.readTemplate(_toFile(Headers.TEMPLATE, el, edb_dir), true, false, true); // small so save in cache
            } catch (Throwable t) {

            }

            GeneSet gset = ParserFactory.readGeneSet(_toFile(Headers.GENESET, el, edb_dir), true, false); // small so save in cache
            RankedList rl = _readRankedList(el, edb_dir);

            float es = Float.parseFloat(el.attribute(ES).getValue());
            float nes = Float.parseFloat(el.attribute(NES).getValue());
            float np = Float.parseFloat(el.attribute(NP).getValue());
            float fdr = Float.parseFloat(el.attribute(FDR).getValue());
            float fwer = Float.parseFloat(el.attribute(FWER).getValue());
            float corrAtES = Float.parseFloat(el.attribute(RANK_SCORE_AT_ES).getValue());
            int rankAtES = (int) Float.parseFloat(el.attribute(RANK_AT_ES).getValue());
            //System.out.println(">> " + rankAtES + " " + corrAtES);
            final Vector rndESS = _toVectorReqd(RND_ES, el);
            final Vector esProfile = _toVectorReqd(ES_PROFILE, el);
            final int[] hitIndices = ParseUtils.string2ints(el.attribute(HIT_INDICES).getValue(), DELIM);

            Chip chip = null;
            Attribute chip_name = el.attribute(CHIP);
            if (chip_name != null && chip_name.getValue() != null && chip_name.getValue().length() > 0) {
                chip = VdbRuntimeResources.getChip(chip_name.getValue());
            }

            EnrichmentScore score = new EnrichmentScoreImpl(es, rankAtES,
                    corrAtES, nes, np, fdr, fwer, hitIndices.length, hitIndices, esProfile, null);

            dtgs.add(new EnrichmentResult(rl, template_opt, gset, chip, score, rndESS, null));

            if (cnt % 500 == 0) {
                System.out.println("read in from edb dtg: " + (cnt + 1));
            }

            cnt++;
        }

        Map<String, Boolean> mps = new HashMap<String, Boolean>();

        Boolean use_median = _boolean(root, Headers.USE_MEDIAN);
        if (use_median != null) {
            mps.put(Headers.USE_MEDIAN, use_median);
        }

        Boolean fix_low = _boolean(root, Headers.FIX_LOW);
        if (fix_low != null) {
            mps.put(Headers.FIX_LOW, fix_low);
        }

        Boolean use_biased = _boolean(root, Headers.USE_BIASED);
        if (use_biased != null) {
            mps.put(Headers.USE_BIASED, use_biased);
        }

        final EnrichmentResult[] results = dtgs.toArray(new EnrichmentResult[dtgs.size()]);
        // Verify the loaded results.
        RankedList rankedList = _rl_shared(results);
        Template template = template_shared(results);
        final EnrichmentDb edb = new EnrichmentDb(NamingConventions.removeExtension(edb_file.getName()), 
        		rankedList, null, template, results, lvp, metric, mps, sort, order, numPerms, edb_dir, null);
        edb.addComment(fComment.toString());
        doneImport();
        return edb;
    }

    // Lifted this call just to isolate the unchecked warning
    @SuppressWarnings("unchecked")
	private final Iterator<Element> getDomIterator(Element root) {
		return root.elementIterator(DTG);
	}

	private RankedList _rl_shared(final EnrichmentResult[] results) {

		final Errors errors = new Errors();
		final String theName = results[0].getRankedList().getName();
		final int theSize = results[0].getRankedList().getSize();

		for (int r = 0; r < results.length; r++) {
			String name = results[r].getRankedList().getName();
			int size = results[r].getRankedList().getSize();
			if (!name.equals(theName)) {
				errors.add("Mismatched rl theName: " + theName + " name: " + name + " at r: " + r + " # rls: "
						+ results.length);
			}

			if (size != theSize) {
				errors.add("Mismatched rl theName: " + theName + " name: " + name + " at r: " + r + " # rls: "
						+ results.length);
			}
		}

		errors.barfIfNotEmptyRuntime();

		return results[0].getRankedList();
	}

	private Template template_shared(final EnrichmentResult[] results) {
		final Errors errors = new Errors();

		if (results[0].getTemplate() == null) {
			return null;
		}

		final String theName = results[0].getTemplate().getName();

		for (int r = 0; r < results.length; r++) {
			String name = results[r].getTemplate().getName();
			if (!name.equals(theName)) {
				errors.add("Mismatched template theName: " + theName + " name: " + name + " at r: " + r + " # results: "
						+ results.length);
			}
		}

		errors.barfIfNotEmptyRuntime();

		return results[0].getTemplate();
	}

    private static File _getEdbDir(final File gseaResultDir) throws ParserException {
        if (gseaResultDir.exists() == false || gseaResultDir.isDirectory() == false) {
            throw new ParserException("Invalid gsea dir for parsing ... expecting a dir, got: " + gseaResultDir);
        }

        File edb_dir;

        if (gseaResultDir.getName().equals("edb")) {
            edb_dir = gseaResultDir;
        } else {
            edb_dir = new File(gseaResultDir, "edb");
            if (edb_dir.exists() == false) {
                throw new IllegalArgumentException("edb dir not found: " + edb_dir);
            }
        }

        return edb_dir;
    }

    private static edu.mit.broad.genome.math.Vector _toVectorReqd(String attName, Element el) throws ParserException {
        barfIfMissing(attName, el);
        return ParseUtils.string2Vector((el.attribute(attName).getValue()), DELIM);
    }

    private static void barfIfMissing(String attName, Element el) throws ParserException {
        if (el.attribute(attName).getValue() == null || el.attribute(attName).getValue().length() == 0) {
            throw new ParserException("Missing attribute " + attName + " in element: " + el.getName());
        }
    }

    // The top level gsea result dir
    public List parse(String sourcepath, InputStream fis) throws Exception {
        startImport(sourcepath);
        EnrichmentDb edb = parseEdb(new File(sourcepath));
        return unmodlist(edb);
    }

    public void export(final PersistentObject pob_edb, final File gseaResultDir) throws Exception {

        if (pob_edb == null) {
            throw new IllegalArgumentException("Param pob_edb cannot be null");
        }

        if (gseaResultDir == null) {
            throw new IllegalArgumentException("Param foo cannot be null: " + gseaResultDir);
        }

        if (gseaResultDir.exists() == false) {
            boolean made = gseaResultDir.mkdir();
            if (!made) {
                throw new IllegalArgumentException("Could not make gsea result dir: " + gseaResultDir);
            }
        }

        // now the sub dir
        final File edb_dir = new File(gseaResultDir, "edb");
        if (edb_dir.exists() == false) {
            boolean made = edb_dir.mkdir();
            if (!made) {
                throw new IllegalArgumentException("Could not make edb dir: " + edb_dir);
            }
        }

        export(pob_edb, edb_dir, EDB_FILE_NAME, null, true, true);
    }

    // dont make reports (thats not my job)
    public void export(final PersistentObject pob_edb,
                       final File saveInThisDir,
                       String edb_file_name,
                       String force_this_rnk_name_opt,
                       final boolean exportTemplateIfAvailable,
                       final boolean exportTheGeneSetMatrix) throws Exception {

        if (pob_edb == null) {
            throw new IllegalArgumentException("Param pob_edb cannot be null");
        }

        final EnrichmentDb edb = (EnrichmentDb) pob_edb;

        // STEP 1
        // scroll through the edb and save all ranked lists, templates and gene sets
        // as many might be shared, dchck before writing
        // consolidate all gsets into a gmt for saving at the end
        // IMP: make sure teh # part is removed before saving
        final Struc struc = new Struc(edb.getNumResults());

        if (exportTheGeneSetMatrix) {
            Set<String> names = new HashSet<String>();
            List<GeneSet> gsets = new ArrayList<GeneSet>();
            for (int i = 0; i < edb.getNumResults(); i++) {
                GeneSet gset = edb.getResult(i).getGeneSet();
                String name = gset.getName(true);
                if (!names.contains(name)) {
                    GeneSet cgset = gset.cloneShallow(name);
                    gsets.add(cgset);
                    names.add(name);
                }
            }
            final GeneSetMatrix gm = new DefaultGeneSetMatrix("gene_sets", gsets);
            struc.gmFile = NamingConventions.createSafeFile(saveInThisDir, gm.getName() + ".gmt");
            ParserFactory.saveGmt(gm, struc.gmFile, false);
        } else {
            struc.gmFile = new File(saveInThisDir, "gene_sets.gmt"); // pseudo file not saved
        }

        // STEP2: Now make the edb xml file and save it
        final Document document = DocumentHelper.createDocument();
        final Element root = document.addElement(EDB);

        root.addAttribute(Headers.LV_PROC, edb.getRankedListProcessor().toString());
        root.addAttribute(Headers.SORT_MODE, edb.getSortMode().toString());
        root.addAttribute(Headers.ORDER, edb.getOrder().toString());
        root.addAttribute(Headers.METRIC, edb.getMetric().toString());
        root.addAttribute(Headers.NUM_PERMS, edb.getNumPerm() + "");

        // metric params
        Map<String, Boolean> map = edb.getMetricParams();
        Object use_median = map.get(Headers.USE_MEDIAN);
        if (use_median != null) {
            root.addAttribute(Headers.USE_MEDIAN, use_median.toString());
        }

        Object fix_low = map.get(Headers.FIX_LOW);
        if (fix_low != null) {
            root.addAttribute(Headers.FIX_LOW, fix_low.toString());
        }

        Object use_biased = map.get(Headers.USE_BIASED);
        if (use_biased != null) {
            root.addAttribute(Headers.USE_BIASED, use_biased.toString());
        }

        for (int i = 0; i < edb.getNumResults(); i++) {
            final EnrichmentResult dtg = edb.getResult(i);
            EnrichmentScore score = dtg.getScore();
            final Element el = root.addElement(DTG);

            //save rnk
            String fname;

            if (force_this_rnk_name_opt != null) {
                fname = _fixExt(force_this_rnk_name_opt, "rnk");
            } else {
                fname = _fixExt(dtg.getRankedList().getName(), "rnk");
            }
            struc.rankedListFiles[i] = saveIfNeeded(fname, dtg.getRankedList(), saveInThisDir);
            el.addAttribute(Headers.RANKED_LIST, fname);

            // save template
            if (exportTemplateIfAvailable && dtg.getTemplate() != null) {
                String bn = AuxUtils.getBaseNameOnly(dtg.getTemplate().getName()); // @todo is this correct??
                fname = _fixExt(bn, "cls");
                struc.templateFiles[i] = saveIfNeeded(fname, dtg.getTemplate(), saveInThisDir);
                el.addAttribute(Headers.TEMPLATE, fname);
            } else {
                el.addAttribute(Headers.TEMPLATE, "na_as_pre_ranked");
            }

            // gset already saved
            el.addAttribute(Headers.GENESET, struc.gmFile.getName() + "#" + dtg.getGeneSet().getName(true));
            el.addAttribute(ES, "" + Printf.format(score.getES()));
            el.addAttribute(NES, "" + Printf.format(score.getNES()));
            el.addAttribute(NP, "" + Printf.format(score.getNP()));
            el.addAttribute(FDR, "" + Printf.format(score.getFDR()));
            el.addAttribute(FWER, "" + Printf.format(score.getFWER()));

            // @note IMP optional
            if (dtg.getChip() != null) {
                el.addAttribute(CHIP, dtg.getChip().getName());
            }

            el.addAttribute(RND_ES, Printf.format(dtg.getRndESS(), DELIM));
            el.addAttribute(HIT_INDICES, Printf.format(score.getHitIndices(), DELIM));
            el.addAttribute(ES_PROFILE, Printf.format(score.getESProfile(), DELIM));
            el.addAttribute(RANK_AT_ES, "" + Printf.format(score.getRankAtES()));
            el.addAttribute(RANK_SCORE_AT_ES, "" + Printf.format(score.getRankScoreAtES()));
        }

        if (edb_file_name.endsWith("edb") == false) {
            edb_file_name = edb_file_name + ".edb";
        }

        File edb_file = new File(saveInThisDir, edb_file_name);
        PrintWriter pw = new PrintWriter(new FileOutputStream(edb_file));
        //OutputFormat format = OutputFormat.createCompactFormat();
        //Make sure the XML file is UTF-8 encoding --> issue loading edb file into EM 
        OutputFormat format = OutputFormat.createPrettyPrint();
        format.setEncoding("UTF-8");
        XMLWriter writer = new XMLWriter(new FileOutputStream(edb_file), format);
        writer.write(document);
        writer.close();
        pw.close();

        doneExport();
    }

    private static String _fixExt(String name, String expectedExt) {
        if (!name.endsWith("." + expectedExt)) {
            name = name + "." + expectedExt;
        }

        return name;
    }

    private Map<String, RankedList> rankedListNameRankedListObject;

    private RankedList _readRankedList(final Element el, final File edb_dir) throws Exception {
        if (rankedListNameRankedListObject == null) {
            rankedListNameRankedListObject = new HashMap<String, RankedList>();
        }

        String name = el.attribute(Headers.RANKED_LIST).getValue();
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("No ranked list element in the xml: " + el);
        }

        RankedList rankedList = rankedListNameRankedListObject.get(name);
        if (rankedList == null) {
            File file = new File(edb_dir, NamingConventions.createSafeFileName(name));
            rankedList = new RankedListJITImpl(file);
        }
        rankedListNameRankedListObject.put(name, rankedList);

        return rankedList;
    }

	static class Struc {
        File[] templateFiles;
        File[] rankedListFiles;
        File gmFile;

        Struc(int numResults) {
            templateFiles = new File[numResults];
            rankedListFiles = new File[numResults];
        }
    }
	
	// key -> pob id, value -> file in which it has been saved
	private Map<String, File> fPobidFileMap;

    private File saveIfNeeded(final String name, final PersistentObject pob, final File inDir) throws Exception {

        if (fPobidFileMap == null) {
            this.fPobidFileMap = new HashMap<String, File>();
        }

        final String id = name + "." + pob.getClass().getName();
        if (fPobidFileMap.containsKey(id)) {
            return fPobidFileMap.get(id);
        }

        File file = NamingConventions.createSafeFile(inDir, name);
        if (file.exists()) {
            log.warn("Overwriting extant file: " + file);
        }

        if (pob instanceof RankedList) {
            ParserFactory.save((RankedList) pob, file);
        } else if (pob instanceof Template) {
            ParserFactory.save((Template) pob, file, false);
        } else {
            throw new IllegalArgumentException("Unknown object: " + pob);
        }

        fPobidFileMap.put(id, file);
        return file;
    }

    private static Boolean _boolean(final Element el, final String attrName) {
        final Attribute attr = el.attribute(attrName);
        if (attr == null) {
            return null;
        } else {
            return Boolean.valueOf(attr.getValue());
        }
    }


    private static File _toFile(String attName, Element el, File inDir) {
        return new File(inDir, el.attribute(attName).getValue());
    }

}    // End of class EdbParser
