/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.parsers;

import edu.mit.broad.genome.*;
import edu.mit.broad.genome.io.FtpResultInputStream;
import edu.mit.broad.genome.io.FtpSingleUrlTransferCommand;
import edu.mit.broad.genome.objects.*;
import edu.mit.broad.genome.objects.esmatrix.db.EnrichmentDb;
import edu.mit.broad.genome.reports.api.Report;
import edu.mit.broad.genome.utils.FileUtils;
import edu.mit.broad.vdb.chip.Chip;
import edu.mit.broad.xbench.core.api.Application;

import org.apache.log4j.Logger;

import xapps.gsea.GseaWebResources;

import java.io.*;
import java.net.URL;
import java.util.*;

/**
 * Facade pattern to easily use the parser classes and methods
 * for simple read in / write out stuff that is used 95% of the time.
 * <p/>
 * Also implements a static cache so that Files are parsed only once (by default)
 * for each jvm invocation.
 *
 * @author Aravind Subramanian
 * @author David Eby
 * @version %I%, %G%
 */
public class ParserFactory implements Constants {
    
    // These probably belong elsewhere if we make a broader file cache than just for special CHIPs
    private static final File fileCacheDir = new File(Application.getVdbManager().getRuntimeHomeDir(), "file_cache");
    private static final File chipCacheDir = new File(fileCacheDir, "chip");
    static {
        // Make sure the cache dirs exist.
        if (!chipCacheDir.exists()) {
            chipCacheDir.mkdirs();
        }
    }
    
    private static final Logger klog = Logger.getLogger(ParserFactory.class);

    /**
     * Privatized Class constructor
     * static methods only.
     */
    private ParserFactory() {
    }

    private static final ObjectCache kDefaultObjectCache = new ObjectCache();

    // The default one is the generic NON-application related cache
    //additionally there are application specific classes
    static ObjectCache _getCache() {
        return kDefaultObjectCache;
    }

    /**
     * @return The Parsers object cache
     */
    public static ObjectCache getCache() {
        return _getCache();
    }

    public static void extractGeneSets(GeneSetMatrix gm) {
        _getCache().makeVisible(gm.getGeneSets(), GeneSet.class);
    }

    public static GeneSet combineIntoOne(final GeneSetMatrix gm) {
        final Set names = gm.getAllMemberNamesOnlyOnceS();
        final GeneSet gset = new GeneSet("combo_" + names.size() + "_" + gm.getName(), names);
        _getCache().makeVisible(gset, GeneSet.class);
        return gset;
    }

    public static Dataset readDataset(File file, boolean useCache, boolean add2cache) throws Exception {
        return readDataset(file.getPath(), createInputStream(file), useCache, add2cache);
    }

    private static Dataset readDataset(String path, InputStream is, boolean useCache) throws Exception {
        return readDataset(path, is, useCache, true);
    }

    private static Dataset readDataset(String path, InputStream is, boolean useCache,
                                      boolean add2cache) throws Exception {
        if (path == null) {
            throw new IllegalArgumentException("Param file cannot be null");
        }

        if (useCache && (_getCache().isCached(path, Dataset.class))) {
            // TODO: very likely should be handled in try/finally
            is.close();
            return (Dataset) _getCache().get(path, Dataset.class);
        }

        // as a help to make datasets ext agnostic
        String ext = NamingConventions.getExtension(path);
        if (ext.equals(Constants.GCT)) {
            return readDatasetGct(path, is, useCache, add2cache);
        }

        if (ext.equals(Constants.PCL)) {
            return readDatasetPcl(path, is, useCache, add2cache);
        }

        Dataset ds;
        Parser parser = new ResParser();
        parser.setSilentMode(false);
        List list = parser.parse(toName(path), is);
        ds = (Dataset) list.get(0);

        // sometimes might not want to for memory reasons
        if (add2cache) {
            _getCache().add(path, ds, Dataset.class);
        }

        // TODO: very likely should be handled in try/finally
        is.close();
        return ds;
    }

    private static Dataset readDatasetGct(String path, InputStream is, boolean useCache) throws Exception {
        return readDatasetGct(path, is, useCache, true);
    }

    private static Dataset readDatasetGct(String path, InputStream is,
                                         boolean useCache,
                                         boolean add2cache) throws Exception {
        if (path == null) {
            throw new IllegalArgumentException("Param file cannot be null");
        }

        if (is == null) {
            throw new IllegalArgumentException("Param is cannot be null");
        }

        if (useCache && (_getCache().isCached(path, Dataset.class))) {
            return (Dataset) _getCache().get(path, Dataset.class);
        }

        // as a help to make datasets ext agnostic
        String ext = NamingConventions.getExtension(path);
        if (ext.equals(Constants.RES)) {
            return readDataset(path, is, useCache);
        }

        if (ext.equals(Constants.PCL)) {
            return readDatasetPcl(path, is, useCache);
        }

        GctParser parser = new GctParser();
        parser.setSilentMode(false);
        List list = parser.parse(toName(path), is);

        Dataset ds = (Dataset) list.get(0);

        if (add2cache) {
            _getCache().add(path, ds, Dataset.class);
        }

        // TODO: very likely should be handled in try/finally
        is.close();
        return ds;
    }

    private static Dataset readDatasetTXT(String path, InputStream is, boolean useCache) throws Exception {
        if (path == null) {
            throw new IllegalArgumentException("Param file cannot be null");
        }
        
        if (is == null) {
            throw new IllegalArgumentException("Param is cannot be null");
        }
        
        if (useCache && (_getCache().isCached(path, Dataset.class))) {
            return (Dataset) _getCache().get(path, Dataset.class);
        }
        
        // as a help to make datasets ext agnostic
        String ext = NamingConventions.getExtension(path);
        if (ext.equals(Constants.RES)) {
            return readDataset(path, is, useCache);
        }
        
        if (ext.equals(Constants.GCT)) {
            return readDatasetGct(path, is, useCache);
        }
        
        if (ext.equals(Constants.PCL)) {
            return readDatasetPcl(path, is, useCache);
        }
        
        TxtDatasetParser parser = new TxtDatasetParser();
        parser.setSilentMode(false);
        List list = parser.parse(toName(path), is);
        Dataset ds = (Dataset) list.get(0);
        
        _getCache().add(path, ds, Dataset.class);
        
        // TODO: very likely should be handled in try/finally
        is.close();
        return ds;
    }

    private static Dataset readDatasetPcl(String path, InputStream is, boolean useCache) throws Exception {
        return readDatasetPcl(path, is, useCache, true);
    }

    private static Dataset readDatasetPcl(String path, InputStream is, boolean useCache, boolean add2cache) throws Exception {

        if (path == null) {
            throw new IllegalArgumentException("Param file cannot be null");
        }

        if (is == null) {
            throw new IllegalArgumentException("Param is cannot be null");
        }

        if (useCache && (_getCache().isCached(path, Dataset.class))) {
            return (Dataset) _getCache().get(path, Dataset.class);
        }

        // as a help to make datasets ext agnostic
        String ext = NamingConventions.getExtension(path);
        if (ext.equals(Constants.RES)) {
            return readDataset(path, is, useCache);
        }

        if (ext.equals(Constants.GCT)) {
            return readDatasetGct(path, is, useCache);
        }

        Parser parser = new PclParser();
        List list = parser.parse(path, is); // @note IMP special hack for PCL parser
        Dataset ds = (Dataset) list.get(0);

        if (add2cache) {
            _getCache().add(path, ds, Dataset.class);
        }

        // TODO: very likely should be handled in try/finally
        is.close();
        return ds;
    }

    /**
     * Note the aux parsing business, so we might actually end of adding
     * more than 1 template
     *
     * @param file
     * @return
     * @throws Exception
     */
    public static Template readTemplate(File file) throws Exception {
        File f = AuxUtils.getBaseFileFromAuxFile(file);
        return readTemplate(file.getPath(), createInputStream(f), true, true, false); // use cache when possible
    }

    public static Template readTemplate(File file, boolean useCache, boolean silentMode) throws Exception {
        File f = AuxUtils.getBaseFileFromAuxFile(file);
        return readTemplate(file.getPath(), createInputStream(f), useCache, true, silentMode);
    }

    public static Template readTemplate(File file, boolean useCache, boolean add2cache, boolean silentMode) throws Exception {
        File f = AuxUtils.getBaseFileFromAuxFile(file);
        return readTemplate(file.getPath(), createInputStream(f), useCache, add2cache, silentMode);
    }

    /**
     * @param file
     * @param useCache
     * @return
     * @throws Exception
     */
    public static Template[] readTemplates(File file) throws Exception {
        // Only used for continuous templates (they are not all in cache as cache clobbers multi cls from same file)
        File f = AuxUtils.getBaseFileFromAuxFile(file);
        return _readTemplates(file.getPath(), createInputStream(f), false, false, true);
    }

    private static Template readTemplate(String path_and_aux, InputStream is, boolean useCache, boolean add2cache, boolean silentMode) throws Exception {
        if (path_and_aux == null) {
            throw new IllegalArgumentException("Param path_and_aux cannot be null");
        }

        final Template[] templates = _readTemplates(path_and_aux, is, useCache, add2cache, silentMode);

        if (AuxUtils.isAux(path_and_aux)) {
            // TODO: very likely should be handled in try/finally
            is.close();
            return findTemplate(toName(path_and_aux), templates);
        } else {
            // TODO: very likely should be handled in try/finally
            is.close();
            return templates[0];// our main template is always the first one (auxs are hidden unless specifically asked for)
        }
    }

    // does the real stuff
    private static Template[] _readTemplates(final String path,
                                            final InputStream is,
                                            final boolean useCache,
                                            final boolean add2cache,
                                            final boolean silentMode) throws Exception {

        if (path == null) {
            throw new IllegalArgumentException("Param path cannot be null");
        }

        if (is == null) {
            throw new IllegalArgumentException("Param is cannot be null");
        }

        Set baseTemplates = new HashSet();
        File basefile;
        String base_path = AuxUtils.getBasePathFromAuxPath(path);
        basefile = new File(base_path);

        if (useCache && _getCache().isCached(base_path, Template.class)) {
            Object obj = _getCache().get(base_path, Template.class);
            baseTemplates.add(obj);
            // TODO: very likely should be handled in try/finally
            is.close();
        } else {

            Parser parser = new ClsParser();
            parser.setSilentMode(silentMode);

            if (AuxUtils.isAux(path)) {
                basefile = AuxUtils.getBaseFileFromAuxFile(new File(path));
                List list = parser.parse(basefile.getName(), basefile);
                // multiple only for numeric templates
                baseTemplates.addAll(list);
            } else {
                List list = parser.parse(toName(path), is);
                baseTemplates.addAll(list); // multiple only for numeric templates
                // just one
                basefile = new File(path);
            }
        }

        // TODO: very likely should be handled in try/finally
        is.close();

        final List allTemplates = new ArrayList();
        Template[] allTss = (Template[]) baseTemplates.toArray(new Template[baseTemplates.size()]);

        for (int j = 0; j < allTss.length; j++) { // Typically for non-continuous templates, only done once

            final Template[] tss = TemplateFactory.extractAllPossibleTemplates(allTss[j], true); // @note here s where we turn a template into many subtemplates

            for (int i = 0; i < tss.length; i++) {
                allTemplates.add(tss[i]);
            }

            if (!silentMode) {
                klog.debug("From: " + path + " (and its supers & auxes) total # of templates made: " + allTemplates.size());
            }

            if (add2cache) {
                boolean fire = false;
                for (int t = 0; t < allTemplates.size(); t++) {
                    Template tm = (Template) allTemplates.get(t);
                    if (t == allTemplates.size() - 1) {
                        fire = true;
                    }

                    if (silentMode) {
                        fire = false; // @note
                    }

                    if (tm.isAux()) {
                        File pseudo = new File(basefile.getParentFile(), tm.getName()); // note how a pseudo file is made
                        _getCache().add(pseudo, tm, Template.class, fire);
                    } else {
                        _getCache().add(basefile, tm, Template.class, fire);
                    }
                }
            }
        }

        return (Template[]) allTemplates.toArray(new Template[allTemplates.size()]);
    }

    /**
     * gsets can also be within a gmx/gmt file via the aux mechanism
     *
     * @param file
     * @return
     * @throws Exception
     */
    public static GeneSet readGeneSet(File file, boolean useCache) throws Exception {
        return readGeneSet(file, useCache, true);
    }

    public static GeneSet readGeneSet(File file, boolean useCache, boolean add2Cache) throws Exception {
        InputStream is = createInputStream(file);
        return readGeneSet(file.getPath(), is, useCache, add2Cache);
    }

    // IMP: dont name this r..geneset as it needs to indicate that the parsing is specifically for a f gset formatted file
    // however the returned object (and cached object) are GeneSets (just like gct -> Dataset)
    private static GeneSet readGeneSet(String path, InputStream is, boolean useCache) throws Exception {
        return readGeneSet(path, is, useCache, true);
    }

    private static GeneSet readGeneSet(String path, InputStream is, boolean useCache, boolean add2Cache) throws Exception {

        if (path == null) {
            throw new IllegalArgumentException("Param path cannot be null");
        }

        if (is == null) {
            throw new IllegalArgumentException("Param is cannot be null");
        }

        if ((useCache) && (_getCache().isCached(path, GeneSet.class))) {
            // TODO: very likely should be handled in try/finally
            is.close();
            return (GeneSet) _getCache().get(path, GeneSet.class);
        }

        GeneSet gset;

        if (AuxUtils.isAux(path)) {
            String gsetName = AuxUtils.getAuxNameOnlyNoHash(path);
            //System.out.println("was an aux");
            File f = AuxUtils.getBaseFileFromAuxFile(new File(path));
            GeneSetMatrix gm = readGeneSetMatrix(f.getPath(), createInputStream(f), useCache, true, add2Cache);
            return gm.getGeneSet(gsetName);
        } else {
            Parser parser = new GeneSetParser();
            gset = (GeneSet) parser.parse(toName(path), is).get(0);
            // for convenience make a genesetmatrix too (its not expensive)
            // but be careful where you are adding -- dont want call to be recursive
            //kObjectCache.add(path, new DefaultGeneSetMatrix(gset.getName(), new GeneSet[]{gset}), GeneSetMatrix.class);
            _getCache().addInvisibly(path, new DefaultGeneSetMatrix(gset.getName(), new GeneSet[]{gset}));
        }

        if (add2Cache) {
            _getCache().add(path, gset, GeneSet.class);
        }
        // TODO: very likely should be handled in try/finally
        is.close();
        return gset;
    }


    /**
     * @param file
     * @return
     * @throws Exception
     */
    public static Report readReport(File file, boolean useCache) throws Exception {

        if (file.isDirectory()) {
            file = FileUtils.findFile(file, "rpt"); // @note hack to read the rpt
        }

        return readReport(file.getPath(), createInputStream(file), useCache);
    }

    private static Report readReport(String path, InputStream is, boolean useCache) throws Exception {

        if (path == null) {
            throw new IllegalArgumentException("Param path cannot be null");
        }

        if (is == null) {
            throw new IllegalArgumentException("Param is cannot be null");
        }


        if ((useCache) && (_getCache().isCached(path, Report.class))) {
            // TODO: very likely should be handled in try/finally
            is.close();
            return (Report) _getCache().get(path, Report.class);
        }

        //log.debug("Parsing Report from: " + path);
        ReportParser parser = new ReportParser();
        Report rpt = (Report) parser.parse(toName(path), is).get(0);

        if (useCache) { // note special -- dont add
            _getCache().add(path, rpt, Report.class);
        }

        // TODO: very likely should be handled in try/finally
        is.close();
        return rpt;
    }


    public static Chip readChip(String sourcePath) throws Exception {
        return readChip(sourcePath, createInputStream(sourcePath), true);
    }

    private static Chip readChip(String path, InputStream is, boolean useCache) throws Exception {

        if (path == null) {
            throw new IllegalArgumentException("Param path cannot be null");
        }

        if (is == null) {
            throw new IllegalArgumentException("Param is cannot be null");
        }

        if ((useCache) && (_getCache().isCached(path, Chip.class))) {
            // TODO: very likely should be handled in try/finally
            is.close();
            return (Chip) _getCache().get(path, Chip.class);
        }

        //log.debug("Parsing Report from: " + path);
        Parser parser = new ChipParser();
        Chip chip = (Chip) parser.parse(path, is).get(0);

        if (useCache) {
            _getCache().add(path, chip, Chip.class);
        }

        // TODO: very likely should be handled in try/finally
        is.close();

        return chip;
    }

    public static RankedList readRankedList(File file) throws Exception {
        return readRankedList(file.getPath(), createInputStream(file), false);
    }

    private static RankedList readRankedList(String path, InputStream is, boolean useCache) throws Exception {
        if (path == null) {
            throw new IllegalArgumentException("Param path cannot be null");
        }
        
        if (is == null) {
            throw new IllegalArgumentException("Param is cannot be null");
        }
        
        if (useCache && (_getCache().isCached(path, RankedList.class))) {
            // TODO: very likely should be handled in try/finally
            is.close();
            return (RankedList) _getCache().get(path, RankedList.class);
        }
        
        //log.debug("Parsing Report from: " + path);
        RankedListParser parser = new RankedListParser();
        parser.setSilentMode(false);
        RankedList rl = (RankedList) parser.parse(path, is).get(0);
        
        _getCache().add(path, rl, RankedList.class);
        
        // TODO: very likely should be handled in try/finally
        is.close();
        
        return rl;
    }

    public static EnrichmentDb readEdb(final File gseaResultDir, final boolean useCache) throws Exception {
        return readEdb(gseaResultDir, useCache, false);
    }
    
    public static EnrichmentDb readEdb(final File gseaResultDir, final boolean useCache, boolean silentMode) throws Exception {
        if (gseaResultDir == null) {
            throw new IllegalArgumentException("Param gseaResultDir cannot be null");
        }

        if (useCache && (_getCache().isCached(gseaResultDir, EnrichmentDb.class))) {
            return (EnrichmentDb) _getCache().get(gseaResultDir, EnrichmentDb.class);
        }

        EdbFolderParser folderParser = new EdbFolderParser();
        folderParser.setSilentMode(silentMode);

        final EnrichmentDb edb = new EdbFolderParser().parseEdb(gseaResultDir);
        _getCache().add(gseaResultDir, edb, EnrichmentDb.class);
        return edb;
    }

    /**
     * supports aux mechanism
     *
     * @param file
     * @return
     * @throws Exception
     */
    public static GeneSetMatrix readGeneSetMatrix(File file, boolean useCache) throws Exception {
        return readGeneSetMatrix(file.getPath(), createInputStream(file), useCache);
    }

    public static GeneSetMatrix readGeneSetMatrix(String path, InputStream is, boolean useCache) throws Exception {
        return readGeneSetMatrix(path, is, useCache, true, true);
    }

    private static GeneSetMatrix readGeneSetMatrix(String path,
                                                   final InputStream is,
                                                   final boolean useCache,
                                                   final boolean checkforduplicates,
                                                   final boolean add2Cache) throws Exception {

        if (path == null) {
            throw new IllegalArgumentException("Param file cannot be null");
        }

        if (is == null) {
            throw new IllegalArgumentException("Param is cannot be null");
        }

        // aux mechanism is a bit diff and much simpler from templates' we just strip it off
        // and register the gsetnames with the gmx -- no new objects are created
        if (AuxUtils.isAux(path)) {
            path = AuxUtils.getBasePathFromAuxPath(path);
        }

        String ext = NamingConventions.getExtension(path);
        if (ext.equals(Constants.GMT)) {
            return readGeneSetMatrixT(path, is, useCache, checkforduplicates, add2Cache);
        }

        if (ext.equals(Constants.GRP)) {
            GeneSet gset = readGeneSet(path, is, useCache, add2Cache);
            return new DefaultGeneSetMatrix(toName(path), new GeneSet[]{gset});
        }

        if (useCache && (_getCache().isCached(path, GeneSetMatrix.class))) {
            // TODO: very likely should be handled in try/finally
            is.close();
            return (GeneSetMatrix) _getCache().get(path, GeneSetMatrix.class);
        }

        GmxParser parser = new GmxParser();
        parser.setCheckForDuplicates(checkforduplicates);
        GeneSetMatrix gmx = (GeneSetMatrix) parser.parse(toName(path), is).get(0);

        if (add2Cache) {
            _getCache().add(path, gmx, GeneSetMatrix.class);
        }

        // IMP also fetch and add all gsets in the gmx to cache
        File parentFile = new File(path).getParentFile();
        for (int i = 0; i < gmx.getNumGeneSets(); i++) {
            File pseudo = new File(parentFile, gmx.getGeneSet(i).getName());

            if (add2Cache) {
                _getCache().addInvisibly(pseudo, gmx.getGeneSet(i));
            }
        }

        if (add2Cache) {
            _getCache().sortModel(GeneSet.class);
            // @todo fix me
            _getCache().hackAddAuxSets(gmx);
        }

        // TODO: very likely should be handled in try/finally
        is.close();
        return gmx;
    }

    private static GeneSetMatrix readGeneSetMatrixT(String path,
                                                   InputStream is,
                                                   boolean useCache,
                                                   boolean checkForDuplicates,
                                                   boolean add2Cache) throws Exception {

        if (path == null) {
            throw new IllegalArgumentException("Param file cannot be null");
        }

        if (is == null) {
            throw new IllegalArgumentException("Param is cannot be null");
        }

        if (AuxUtils.isAux(path)) {
            path = AuxUtils.getBasePathFromAuxPath(path);
        }

        // @note as a help to make gm ext agnostic
        String ext = NamingConventions.getExtension(path);
        if (ext.equals(Constants.GMX)) {
            return readGeneSetMatrix(path, is, useCache, checkForDuplicates, add2Cache);
        }

        if (ext.equals(Constants.GRP)) {
            GeneSet gset = readGeneSet(path, is, useCache);
            return new DefaultGeneSetMatrix(toName(path), new GeneSet[]{gset});
        }

        if (useCache && (_getCache().isCached(path, GeneSetMatrix.class))) {
            // TODO: very likely should be handled in try/finally
            is.close();
            return (GeneSetMatrix) _getCache().get(path, GeneSetMatrix.class);
        }

        Parser parser = new GmtParser();
        //parser.setCheckForDuplicates(checkForDuplicates);
        GeneSetMatrix gmx = (GeneSetMatrix) parser.parse(toName(path), is).get(0);

        _getCache().add(path, gmx, GeneSetMatrix.class);

        //disabled after advance chooser
        // IMP also fetch and add all gsets in the gmx to cache
        File parentFile = new File(path).getParentFile();
        for (int i = 0; i < gmx.getNumGeneSets(); i++) {
            File pseudo = new File(parentFile, gmx.getGeneSet(i).getName());
            _getCache().addInvisibly(pseudo, gmx.getGeneSet(i));
        }

        _getCache().hackAddAuxSets(gmx);

        // TODO: very likely should be handled in try/finally
        is.close();
        return gmx;
    }

    public static PersistentObject read(final String path, final InputStream is) throws Exception {
        return read(path, is, true);
    }
    
    private static PersistentObject read(final String path, final InputStream is, boolean useCache) throws Exception {

        try {
            if (path == null) {
                throw new IllegalArgumentException("Param file cannot be null");
            }

            if (is == null) {
                throw new IllegalArgumentException("Param is cannot be null");
            }
            StringTokenizer tok = new StringTokenizer(path, "@");

            final String tmp = tok.nextToken();
            final String ext = NamingConventions.getExtensionLiberal(tmp).toLowerCase();

            if (ext.equalsIgnoreCase(RES)) {
                return readDataset(path, is, useCache);
            } else if (ext.equalsIgnoreCase(GCT)) {
                return readDatasetGct(path, is, useCache);
            } else if (ext.equalsIgnoreCase(TXT)) {
                return readDatasetTXT(path, is, useCache);
            } else if (ext.equalsIgnoreCase(PCL)) {
                return readDatasetPcl(path, is, useCache);
            } else if (ext.startsWith(CLS)) { // IMP note -- special for the aux hash
                return readTemplate(path, is, useCache, useCache, false);
            } else if (ext.equalsIgnoreCase(GRP)) {
                return readGeneSet(path, is, useCache);
            } else if (ext.equalsIgnoreCase(RNK)) {
                return readRankedList(path, is, useCache);
            } else if (ext.startsWith(GMX)) {// IMP note -- special for the aux hash
                return readGeneSetMatrix(path, is, useCache);
            } else if (ext.startsWith(GMT)) {// IMP note -- special for the aux hash
                return readGeneSetMatrixT(path, is, useCache, true, true);
            } else if (ext.equalsIgnoreCase(EDB)) {
                return readEdb(new File(path), useCache);
            } else if (ext.equalsIgnoreCase(RPT)) {
                return readReport(path, is, useCache);
            } else if (ext.equalsIgnoreCase(CHIP) || ext.equalsIgnoreCase(CSV)) {
                return readChip(path, is, useCache);
            } else {
                throw new IllegalArgumentException("Unknown file format: " + path + " no known Parser for ext: " + ext);
            }

        } catch (java.io.InterruptedIOException e) {
            klog.info("progress exception - possibly cancelled ... ignoring");
            return null;
        } finally {
            is.close();
        }
    }

    /**
     * default is to use the cache
     *
     * @param file
     * @return
     * @throws Exception
     */
    public static PersistentObject read(final File file) throws Exception {
        return read(file, true);
    }

    // IMP have to give back the  aux object if the file is an aux one
    // but have to read data from the base file
    // TODO: review cache query arrangement here.
    // There is exactly one call where we bypass the cache (LoadAction) on a general read call; all 
    // other callers go through it.
    // It is probably better to turn this on its head somehow, where we raise the cache query to
    // the fore front and centralize it.
    public static PersistentObject read(final File file, final boolean useCache) throws Exception {
        File baseFile = AuxUtils.getBaseFileFromAuxFile(file);
        return read(file.getPath(), createInputStream(baseFile), useCache);
    }

    /**
     * auto adds appropriate extension if specified file doesnt already have it
     *
     * @param pob
     * @param file
     * @throws Exception
     * @maint add a new parser and this method needs attention
     * @todo change to use DataFormat directly to avoid else if statements
     */
    public static void save(PersistentObject pob, File file) throws Exception {

        if (pob == null) {
            throw new IllegalArgumentException("Parameter pob cannot be null");
        }

        if (file == null) {
            throw new IllegalArgumentException("Parameter file cannot be null");
        }

        // TODO: We may not allow all of these to be saved
        if (pob instanceof Dataset) {
            file = ensureCorrectExt(file, RES);
            save((Dataset) pob, file);
        } else if (pob instanceof Template) {
            file = ensureCorrectExt(file, CLS);
            save((Template) pob, file);
        } else if (pob instanceof GeneSet) {
            file = ensureCorrectExt(file, GRP);
            save((GeneSet) pob, file);
        } else if (pob instanceof RankedList) {
            file = ensureCorrectExt(file, RNK);
            save((RankedList) pob, file);
        } else if (pob instanceof GeneSetMatrix) {
            file = ensureCorrectExt(file, GMX);
            save((GeneSetMatrix) pob, file);
        } else if (pob instanceof Report) {
            file = ensureCorrectExt(file, RPT);
            save((Report) pob, file);
        } else if (pob instanceof EnrichmentDb) {
            file = ensureCorrectExt(file, EDB);
            save((EnrichmentDb) pob, file);
        } else {
            throw new IllegalArgumentException("No save method available for: " + pob.getName() + " class: " + pob.getClass());
        }
    }

    /**
     * @param file
     * @param exp_ext
     * @return
     */
    private static File ensureCorrectExt(final File file, final String exp_ext) {
        final String ext = NamingConventions.getExtension(file);
        if (ext.equals(exp_ext)) {
            return file;
        } else {
            return new File(file.getAbsolutePath() + "." + exp_ext);
        }
    }

    /**
     * @param ds
     * @param toFile
     * @throws Exception
     */
    private static File save(Dataset ds, File toFile) throws Exception {
        GctParser parser = new GctParser();
        parser.export(ds, toFile);
        _getCache().add(toFile, ds, Dataset.class);
        return toFile;
    }

    /**
     * @param ds
     * @param toFile
     * @throws Exception
     */
    public static File saveGct(final Dataset ds, final File toFile) throws Exception {
        return save(ds,toFile);
    }

    public static void saveInvisibly2Cache(IDataframe idf, File toFile) throws Exception {
        if (idf instanceof Dataframe) {
            saveInvisibly2Cache((Dataframe) idf, toFile);
        } else if (idf instanceof StringDataframe) {
            saveInvisibly2Cache((StringDataframe) idf, toFile);
        } else {
            throw new NotImplementedException();
        }
    }

    private static void saveInvisibly2Cache(final Dataframe df, final File toFile) throws Exception {
        Parser parser = new DataframeParser();
        parser.export(df, toFile);
        _getCache().addInvisibly(toFile, df);
    }

    /**
     * @param edb
     * @param toFile
     * @throws Exception
     */
    private static void save(final EnrichmentDb edb, final File toFile) throws Exception {
        Parser parser = new EdbFolderParser();
        parser.export(edb, toFile);
        _getCache().add(toFile, edb, EnrichmentDb.class);
    }

    public static void save(final RankedList rl, final File toFile) throws Exception {
        Parser parser = new RankedListParser();
        parser.export(rl, toFile);
    }

    /**
     * @param template
     * @param toFile
     * @throws Exception
     */
    public static void save(final Template template, final File toFile) throws Exception {
        save(template, toFile, true);
    }

    public static void save(final Template template, final File toFile, final boolean add2cache) throws Exception {
        ClsParser parser = new ClsParser();
        parser.export(template, toFile);
        if (add2cache) {
            // Instead of this, we need to read back the just-created file as the template
            // is not usable as-is, though it should be.
            //_getCache().add(toFile, template, Template.class);
            readTemplate(toFile, true, true, false);
        }
    }

    /**
     * @param gset
     * @param toFile
     * @throws Exception
     */
    public static void save(GeneSet gset, File toFile) throws Exception {
        Parser parser = new GeneSetParser();
        parser.export(gset, toFile);
        _getCache().add(toFile, gset, GeneSet.class);
    }

    public static void saveGmt(GeneSetMatrix gmt, File toFile, boolean add2cache) throws Exception {
        Parser parser = new GmtParser();
        parser.export(gmt, toFile);
        if (add2cache) {
            _getCache().add(toFile, gmt, GeneSetMatrix.class);
        }
    }

    /**
     * @param gmx
     * @param toFile
     * @throws Exception
     */
    public static void save(GeneSetMatrix gmx, File toFile) throws Exception {
        Parser parser = new GmxParser();
        parser.export(gmx, toFile);
        if (true) {
            _getCache().add(toFile, gmx, GeneSetMatrix.class);
        }
    }

    public static void save(Report rpt, File toFile) throws Exception {
        save(rpt, toFile, true);
    }

    public static void save(Report rpt, File toFile, boolean add2cache) throws Exception {
        Parser parser = new ReportParser();
        parser.export(rpt, toFile);
        if (add2cache) {
            _getCache().add(toFile, rpt, Report.class);
        }
    }

    public static void save(StringDataframe sdf, File toFile, boolean add2cache) throws Exception {
        Parser parser = new StringDataframeParser();
        parser.export(sdf, toFile);
        if (add2cache) {
            _getCache().add(toFile, sdf, StringDataframe.class);
        }
    }

    private static void saveInvisibly2Cache(StringDataframe sdf, File toFile) throws Exception {
        Parser parser = new StringDataframeParser();
        parser.export(sdf, toFile);
        _getCache().addInvisibly(toFile, sdf);
    }

    private static Template findTemplate(String name, final Template[] tss) {

        // for cases where somehow it becomes: test.cls#test.cls
        if (AuxUtils.getAuxNameOnlyNoHash(name).equals(AuxUtils.getBaseStringFromAux(name))) {
            name = AuxUtils.getAuxNameOnlyNoHash(name);
        }

        if (AuxUtils.isAux(name) && name.endsWith(".cls")) { // hack for coo.cls@a_vs_b.cls
            name = name.substring(0, name.length() - 4);
        }

        for (int t = 0; t < tss.length; t++) {
            if (tss[t].getName().equals(name)) {
                return tss[t];
            }

            if (tss[t].isContinuous()) {
                String name_no_aux = AuxUtils.getAuxNameOnlyNoHash(name);
                if (tss[t].getName().equals(name_no_aux)) {
                    return tss[t];
                }
            }
        }

        StringBuffer buf = new StringBuffer("<html>\n" +
                "<body>\n" +
                "<p>No template for name: " + name + "</p>" +
                "<p>The Available templates are:</p>\n");
        for (int i = 0; i < tss.length; i++) {
            buf.append(tss[i].getName()).append("<br>\n");
        }
        buf.append("</body></html>");
        throw new IllegalArgumentException(buf.toString());
    }

    // @note convention
    private static String toName(final String path) {
        return new File(path).getName();
    }

    // if file doesnt exsits or is a dir then error out with an intuitive message
    // useful before using a new FileInputStream(f) as the error message from that is not
    // very explanatory
    private static InputStream createInputStream(File file) throws IOException {

        // @note as a convebicne auto detect of the file is really a URL
        if (NamingConventions.isURL(file.getPath())) {
            return createInputStream(new URL(file.getPath()));
        }

        if (AuxUtils.isAuxFile(file)) {
            //klog.debug("Auto UNauxing file: " + file);
            file = AuxUtils.getBaseFileFromAuxFile(file);
        }

        if (!file.exists()) {
            throw new FileNotFoundException("File not found: " + file.getAbsolutePath());
        }

        if (file.isDirectory()) {
            throw new IOException("Specified path is a Directory (expecting a File): " + file.getAbsolutePath());
        }

        if (!file.canRead()) {
            throw new IOException("Check file permissions - cannot read data from file: " + file.getAbsolutePath());
        }

        return new BufferedInputStream(new FileInputStream(file));
    }

    private static InputStream createInputStream(URL url) throws IOException {
        klog.debug("Parsing URL: " + url.getPath() + " >> " + url.toString());
        if (url.getProtocol().equalsIgnoreCase("ftp") && url.getHost().equalsIgnoreCase(GseaWebResources.getGseaFTPServer())) {
            try {
                FtpSingleUrlTransferCommand ftpCommand = new FtpSingleUrlTransferCommand(url);
                FtpResultInputStream ftpInputStream = ftpCommand.retrieveAsInputStream();
                return ftpInputStream;
            }
            catch (Exception e) {
                throw new IOException(e);
            }
        } else {
            return new BufferedInputStream(url.openStream());
        }
    }

    private static InputStream createInputStream(final Object source) throws IOException {

        if (source == null) {
            throw new IllegalArgumentException("Parameter source cannot be null");
        }

        if (source instanceof File) {
            return createInputStream((File) source);
        } else if (source instanceof URL) {
            return createInputStream((URL) source);
        }

        String path = source.toString();


        if (NamingConventions.isURL(path)) {
            // common error is ftp.broad... while it should be ftp://ftp.broad...

            if (path.startsWith("ftp.")) {
                path = "ftp://" + path;
            } else if (path.startsWith("gseaftp.")) {
                path = "ftp://" + path;
            }

            return createInputStream(new URL(path));
        }

        // Ok, it might be a file

        File file = new File(path);

        if (file.exists()) {
            return createInputStream(file);
        }

        throw new IOException("Bad data source -- neither file nor url exists for: " + source);
    }

    public static void saveGmt(GeneSetMatrix gmt, File toFile) throws Exception {
        Parser parser = new GmtParser();
        parser.export(gmt, toFile);
        _getCache().add(toFile, gmt, GeneSetMatrix.class);
    }
}