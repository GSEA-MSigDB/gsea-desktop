/*
 * Copyright (c) 2003-2021 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.reports.api;

import edu.mit.broad.genome.*;
import edu.mit.broad.genome.charts.XChart;
import edu.mit.broad.genome.math.Matrix;
import edu.mit.broad.genome.objects.*;
import edu.mit.broad.genome.parsers.DataFormat;
import edu.mit.broad.genome.parsers.ParserFactory;
import edu.mit.broad.genome.reports.RichDataframe;
import edu.mit.broad.genome.reports.pages.*;
import edu.mit.broad.genome.utils.SystemUtils;
import edu.mit.broad.genome.utils.ZipUtility;
import edu.mit.broad.xbench.core.api.Application;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.genepattern.io.ImageUtil;

import xtools.api.Tool;
import xtools.api.param.GeneSetMatrixFormatParam;
import xtools.api.param.ReportLabelParam;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.*;

/**
 * Both a Report and a ReportWriter (for a Tool)
 * <p/>
 * Automagically:
 * 1) makes reports dir if not already extant
 * 2) all reports data saved in tha dir
 * 3) makes files on request -- stamped with a common timestamp to avoid name clashes
 * 4) on complete, saves a Report of itself
 * <p/>
 * <p/>
 * A Report is a colection of zero, one or more files (pdfs, txts etc)
 * and charts.
 * <p/>
 * interface reprsenting a Report that is also a writing mechanism
 * Abstracts out the format - html, pdf etc
 * <p/>
 * Idea here is for the reports to maintain an "index file" with links etc to all the added stuff
 * This would be the kick off point for someone to look at the data
 * that the reports produced. If odf, would mostly all be in the file
 * if htnl, would be linked etc
 *
 * @author Aravind Subramanian
 */
// dont extend abstractobject -- easier to impl ourselves here as impl not pob but reports
public class ToolReport implements Report {

    private transient static Logger klog = Logger.getLogger(ToolReport.class);

    private static String COMMON_ERROR_PREFIX = "The Tool ran successfully but at least one part of the reports production failed\nwith the following details\nThe reports is INcomplete";

    /**
     * The tool for which this reports is being generated
     */

    // Try to NOT store the report as it could be heavy
    // // (see Web stuff for example where each page is a tool and we dont want to cache)
    private Tool fTool_opt; // not always cached

    // TODO: parameterize the type of the Class
    private Class fProducerClass;

    private String fProducerName;

    /**
     * All reports files get saved within here - see gp note below
     */
    private File fReportDir;

    private ToolComments fToolComments;

    private final long fTimestamp = System.currentTimeMillis();
    private transient Date fDate;

    /**
     * holds whether or not the reports has been closed
     */
    private boolean fClosed = false;

    private int fNumPagesAdded;

    /**
     * Name of this reports - NOT the analysis
     */
    private String fReportName;

    /**
     * File in which this reports gets saved
     */
    private File fReportParamsFile;

    private List<Throwable> fErrors;

    /**
     * Contains Page objects
     */
    private Pages fPages;

    /**
     * This is the central method that adds pages to
     *
     * @param page
     * @param file
     */
    private boolean fDoneAddingCss;

    // so that we know whether to remane or not
    private boolean fRptDirMadeExternally = false;

    private KeyValTable fKvt;

    // not always made --- tools can disable if needed
    private HtmlReportIndexPage fHtmlReportIndexPage;

    private File fHtmlIndexPageFile;
    private ReportIndexState fReportIndexState;

    /**
     * Class constructor
     *
     * @param reportForTool
     * @param makeReportSubDir
     * @throws IOException
     * @throws IllegalArgumentException
     */
    public ToolReport(final Tool reportForTool,
                      final boolean cacheToolObject,
                      final ReportIndexState indexState) throws IOException, IllegalArgumentException {

        if (reportForTool == null) {
            throw new IllegalArgumentException("Parameter reportForTool cannot be null");
        }

        // this is the PARENT dir (i.e -out) and NOT the specific rpt dir (the one with the timestamp)
        if (reportForTool.getParamSet().getAnalysisDirParam() == null) {
            throw new IllegalArgumentException("Specified tool does not have a declared fAnalysisDirParam(-out) -- cannot generate Report!");
        }

        // this is the PARENT dir (i.e -out) and NOT the specific rpt dir (i,e its not the one with the timestamp)
        File analysisEnvBaseDir = reportForTool.getParamSet().getAnalysisDirParam().getAnalysisDir();

        // This is the dir into which rpt files are written
        File rptDir = createIfNeededAndGetReportDir(analysisEnvBaseDir, reportForTool, this);

        init(reportForTool, cacheToolObject, rptDir, indexState);
    }


    // central init routine
    private void init(final Tool reportForTool_opt,
                      final boolean cacheToolObject,
                      final File useThisRptDir,
                      final ReportIndexState indexState) throws IllegalArgumentException {

        if (!useThisRptDir.exists()) {
            throw new IllegalArgumentException("Report dir does not exists!!: " + useThisRptDir.getAbsolutePath());
        }

        this.fReportDir = useThisRptDir;
        this.fToolComments = new ToolComments();

        if (reportForTool_opt != null) {
            this.fProducerClass = reportForTool_opt.getClass();
            this.fProducerName = reportForTool_opt.getClass().getName();

            if (cacheToolObject) {
                this.fTool_opt = reportForTool_opt;
            }

            // parameter checks
            // just a helpful tool check -- no real need in this class
            if (reportForTool_opt.getParamSet().getReportLabelParam() == null) {
                throw new IllegalArgumentException("Specified tool does not have a declared ReportLabelParam -- check Tool!");
            }
            this.fReportName = generateReportName(this, reportForTool_opt);
            // this is the PARENT dir (i.e -out) and NOT the specific rpt dir (the one with the timestamp)
            this.fReportParamsFile = new File(fReportDir, fReportName);
        }

        this.fPages = new Pages();

        this.fReportIndexState = indexState;

        if (fReportIndexState.makeReportIndexPage()) {
            this.fHtmlReportIndexPage = new HtmlReportIndexPage(this, fReportIndexState.getHeader());
        }
    }

    /**
     * Here because of linkage issues
     * Convention
     * label.toolname.timestamp.rpt
     *
     * @param report
     * @param tool
     * @return
     */
    private static String generateReportName(final Report report, final Tool tool) {

        final ReportLabelParam lp = tool.getParamSet().getReportLabelParam();
        String label = lp.getReportLabel();

        if (label == null) {
            label = "my_report";
        }

        label = label.trim();

        StringTokenizer tok = new StringTokenizer(tool.getClass().getName(), ".");
        String tn = null;

        while (tok.hasMoreTokens()) {
            tn = tok.nextToken();
        }

        StringBuffer rptName = new StringBuffer();
        rptName.append(label).append('.').append(tn).append('.').append(report.getTimestamp()).append('.');
        rptName.append(DataFormat.RPT_FORMAT.getExtension());

        return rptName.toString();
    }

    public File getZipReportFile() {
        return new File(getReportDir(), getName() + ".zip");
    }

    public File zipReport() {

        File zipped_file = getZipReportFile();

        try {
            File tmp_zipped_file = File.createTempFile(getName(), ".zip");
            new ZipUtility().zipDir(fReportDir, tmp_zipped_file);

            // ok, move it into the dir once it has been zipped up
            FileUtils.moveFile(tmp_zipped_file, zipped_file);

        } catch (Throwable t) {
            klog.error(t);
            try {
                FileUtils.writeStringToFile(zipped_file, t.getMessage());
            }
            catch (IOException ie) {
                klog.error(ie);
            }
        }

        return zipped_file;
    }

    public URI getReportIndex() {
        if (fHtmlIndexPageFile == null) {
            return null;
        } else {
            return fHtmlIndexPageFile.toURI();
        }
    }

    public File getParamsFile() {
        return new File(fReportParamsFile.getPath()); // safe clone
    }

    // handle all errored out stuff here
    // currently just a dir renaming for easy id
    // note that only used for complete errors
    // THIS MUST NEVER THROW AN EXCEPTION
    public void setErroredOut() {
        try {
            // dont rename if a report dir wasnt made here!!
            if (fReportDir != null && fReportDir.exists()) {
                File errorDir = new File(fReportDir.getParentFile(), "error_" + fReportDir.getName());
                if (!fRptDirMadeExternally) {
                    closeReport(false); // @note added june6 dont add to cache
                    klog.info("Renaming rpt dir on error to: " + errorDir);
                    boolean renamed = fReportDir.renameTo(errorDir);
                    if (!renamed) {
                        klog.warn("Could not rename for error to: " + errorDir);
                    }
                } else {
                    klog.info("Pseudo Renaming rpt dir on error to: " + errorDir + " (ext made report dir so not renamed)");
                }
            } else {
                klog.info("No report dir was made yet (but an error condition was detected)");
            }
        } catch (Throwable t) {
            System.out.println("Error while erroring out! (setErroredOut in ToolReport");
            t.printStackTrace();
        }
    }

    /**
     * @return The name of this reports
     * @see edu.mit.broad.genome.NamingConventions for format
     */
    public String getName() {
        return fReportName;
    }

    public long getTimestamp() {
        return fTimestamp;
    }
    
    public Date getDate() {
        if (fDate == null) {
            fDate = new Date(fTimestamp);
        }
        return fDate;
    }

    /**
     * IMP -> if headless then never displayed irrespctive of the vdbgui param
     * Display the Report's contents in a simple GUI.
     * JFrame with a JList holding objects in this reports -> only Files and Charts
     * Double clicking / right popup on an elemnt brings up the File/Chart
     * in a viewer. For files, native viewers only -> Acrobat, etc
     * For Charts -> just put in a Chart displayer
     */

    // IMP -> make sure that xomics does not get initialized due to this call
    public void display() {

        if (SystemUtils.isHeadless()) {
            klog.info("Suppressing display reports as headless mode");
        } else if (SystemUtils.isPropertyTrue("GSEA")) {
            klog.info("Suppressing display reports as gsea app");
        } else {
            klog.info("Displaying reports ...");
            ToolReportDisplay display = new ToolReportDisplay(this);
            display.show();
        }
    }

    public Id getId() {
        throw new NotImplementedException();
    }

    public String getNameEnglish() {
        return null;
    }

    public String getQuickInfo() {
        return "" + fNumPagesAdded;
    }

    public HtmlReportIndexPage getIndexPage() {
        if (fHtmlReportIndexPage == null) {
            throw new IllegalArgumentException("HtmlReportIndexpage was not made");
        }
        return fHtmlReportIndexPage;
    }

    public File getIndexPageFile() {
        return fHtmlIndexPageFile;
    }

    public File[] getFilesProduced() {
        List<File> files = fPages.getFiles_list();
        if (fHtmlIndexPageFile != null && fHtmlIndexPageFile.exists() && !files.contains(fHtmlIndexPageFile)) {
            files.add(fHtmlIndexPageFile);
        }

        return files.toArray(new File[files.size()]);
    }

    public int getNumPagesMade() {
        return fNumPagesAdded;
    }

    public void addComment(String comment) {
        fToolComments.add(comment);
    }

    // this must never fail
    public void addError(final String msg, final Throwable t) {
        if (fErrors == null) {
            fErrors = new ArrayList<Throwable>();
        }

        fErrors.add(t);
        klog.error(msg, t);
    }

    public String getComment() {
        throw new RuntimeException("use getToolComment instead");
    }

    public ToolComments getToolComments() {
        return fToolComments;
    }

    public File getReportDir() {
        return fReportDir;
    }

    /**
     * Report impl
     *
     * @return
     */
    public Properties getParametersUsed() {
        if (fTool_opt != null) {
            return fTool_opt.getParamSet().toProperties();
        } else {
            throw new IllegalStateException("Tool not cached: " + fTool_opt);
        }
    }

    public Tool getTool() {
        if (fTool_opt != null) {
            return fTool_opt;
        } else {
            throw new IllegalStateException("Tool not cached: " + fTool_opt);
        }
    }

    // TODO: parameterize the Class
    public Class getProducer() {
        return fProducerClass;
    }

    public String getProducerName() {
        return fProducerName;
    }

    public File savePage(final XChart xc, final int width, final int height, final File inDir) {
        File file = _createFile(xc.getName(), "png", inDir);

        try {
            xc.saveAsPNG(file, width, height);
            return file;
        } catch (Throwable t) {
            addError("Trouble saving png image", t);
        }

        return file;
    }

    public void savePageSvg(final XChart xc, final int width, final int height, final File file) {
        try {
            ImageUtil.saveAsSVG(xc.getFreeChart(), file, width, height, true);
        } catch (Throwable t) {
            addError("Trouble saving svg image", t);
        }
    }

    private File savePage(final Page page) {
        File file = null;
        try {
            file = _createFile(page.getName(), page.getExt(), fReportDir);
            page.write(new FileOutputStream(file));
            _centralAddPage(page, file);
        } catch (Throwable t) {
            addError("Trouble saving Page", t);
        }
        return file;
    }

    public File savePage(final Page page, final File inDir) {
        File file = null;
        try {
            file = _createFile(page.getName(), page.getExt(), inDir);
            page.write(new FileOutputStream(file));
        } catch (Throwable t) {
            addError("Trouble saving Page", t);
        }
        return file;
    }

    public File savePage(final PersistentObject pob, boolean add2cache) {
        if (pob == null) {
            String msg = "Null pob specified for saving + " + pob;
			addError(msg, new NullPointerException(msg));
            return null;
        }

        try {
            return savePage(pob.getName(), pob.getQuickInfo(), pob, fReportDir, add2cache);
        } catch (Throwable t) {
            addError("Trouble saving pob to report", t);
        }

        return null;
    }

    public File savePageTsv(final StringDataframe idf) {
        return this.savePageTsv(idf, idf.getName(), this.fReportDir);
    }

    public File savePageTsv(final IDataframe idf, final String fileName, final File inDir) {

        if (idf instanceof RichDataframe) { // @note hack
            StringDataframe sdf = (StringDataframe) ((RichDataframe) idf).getDataframe();
            return savePageTsv(sdf, fileName, inDir); // recall this
        }

        if (idf instanceof StringDataframe) {
        	// TODO: Note questionable NaN behavior
            ((StringDataframe) idf).replace("NaN", "---"); // @note default behavior
        }

        File file = _createFile(fileName, Constants.TSV, inDir);
        try {
            ParserFactory.saveInvisibly2Cache(idf, file);
            _centralAddPage(new FileWrapperPage(file, idf.getQuickInfo()));    // @note
        } catch (Throwable t) {
            addError("Trouble saving sdf to report", t);
        }
        return file;
    }

    // save to SPECIFIC DIR SPECIFIED
    private File savePage(String name, String desc, final PersistentObject pob, File inDir, boolean centralAddPage) {
        File file = null;
        try {

            if (pob instanceof RankedList) {
                file = _createFile(name, Constants.TSV, inDir);
                ParserFactory.save((RankedList) pob, file);
            } else if (pob instanceof Dataset) {
                file = _createFile(name, DataFormat.GCT_FORMAT.getExtension(), inDir);
                ParserFactory.saveGct((Dataset) pob, file);
            } else if (pob instanceof BitSetDataset) {
                Dataset ds = ((BitSetDataset) (pob)).toDataset();
                file = _createFile(name, "bsd", inDir);
                ParserFactory.saveGct(ds, file);
                File mf = _createFile(name, "mat", inDir);
                Matrix.save(ds.getMatrix(), mf);
                _centralAddPage(new FileWrapperPage(mf, desc));    // @note
            } else if (pob instanceof StringDataframe) {
                file = _createFile(name, DataFormat.getExtension(pob), inDir);
                ParserFactory.save((StringDataframe) pob, file, false);
            } else if (pob instanceof LabelledVector) {
                final String name1 = name;
                final String desc1 = desc;
                file = savePage(name1, desc1, (LabelledVector) pob, fReportDir, true);
            } else {
                file = _createFile(name, DataFormat.getExtension(pob), inDir);
                ParserFactory.save(pob, file);
            }

            //log.debug("******* " + file + " " + pob.getName());

            if (file != null) {
                _centralAddPage(new FileWrapperPage(file, desc), centralAddPage);    // @note
            }

        } catch (Throwable t) {
            addError("Trouble saving pob to reports", t);
        }

        return file;
    }

    public File savePageGmx(final GeneSetMatrix gmx) {
        final File saveInDir = fReportDir;
        StringBuffer name = new StringBuffer(gmx.getName()).append('.').append(DataFormat.GMX_FORMAT.getExtension());
        File file = createSafeReportFile(name.toString(), saveInDir);
        klog.debug("saving gmt in: " + file);
        try {
            ParserFactory.save(gmx, file);
            _centralAddPage(new FileWrapperPage(file, gmx.getQuickInfo()));    // @note
        } catch (Throwable t) {
            addError("Could not save object to reports object: " + gmx + " in file: " + file.getPath(), t);
        }
        return file;
    }

    private File savePage(final String name, final String desc, final String content) {
        StringBuffer fname = new StringBuffer(name).append('.').append(Constants.TSV);
        File file = createSafeReportFile(fname.toString(), getReportDir());
        try {
            FileUtils.writeStringToFile(file, content);
            _centralAddPage(new FileWrapperPage(file, desc));  // @note
        } catch (Throwable t) {
            addError("Could not save object to reports data: " + name + " in file: " + file.getPath(), t);
        }
        return file;
    }

    private void _centralAddPage(final Page page, final File file) {
        if (fReportIndexState.keepTrackOfPages() && !(page instanceof HtmlReportIndexPage)) {
            fPages.add(page, file);
        }
        fNumPagesAdded++;

        // for html pages, auto add a CSS file. Only do this once
        if (page instanceof HtmlPage) {
            createCss();
        }
    }

    private void createCss() {
        if (fDoneAddingCss)  return;
        try {
            File cssFile = _createFile("xtools", "css", fReportDir);
            FileUtils.copyURLToFile(JarResources.toURL("xtools.css"), cssFile);
            fDoneAddingCss = true;
        } catch (Throwable t) {
            klog.error("Trouble copying over CSS", t);
        }
    }

    public File createSubDir(final String name) {
        File subDir = NamingConventions.createSafeFile(getReportDir(), name);
        if (!subDir.exists()) {
            boolean made = subDir.mkdir();
            if (!made) {
                throw new IllegalStateException("Unable to make an output folder for the report at: " + subDir.getPath());
            }
        }
        createCss(); // @note
        return subDir;
    }

    private String fOptWebBase;

    private void _centralAddPage(final FileWrapperPage page) {
        _centralAddPage(page, fReportIndexState.keepTrackOfPages());
    }

    private void _centralAddPage(final FileWrapperPage page, boolean keepTrack) {
        if (page == null) {
            throw new IllegalArgumentException("Param page cannot be null");
        }

        if (keepTrack) {
            fPages.add(page);
        }

        fNumPagesAdded++;

        if (fReportIndexState.keepTrackOfPagesInHtmlIndex()) {
            if (fKvt == null) {
                this.fKvt = new KeyValTable();
            }
            fKvt.addRow(page.getDesc(), page.createLink(fReportDir, fOptWebBase));
        }
    }

    /**
     * Tools MUST call once done
     * IMP dont throw an exception -> dont want this to cause tool to hang
     * after its done plenty of work
     */
    public void closeReport(boolean add2ParserFactory) {

        if (fClosed) {
            return;
        }

        // add the rpt params file at the very end
        if (fReportParamsFile != null && fReportParamsFile.exists()) {
            _centralAddPage(new FileWrapperPage(fReportParamsFile, "List of parameters used by the tool (rpt)"));
        }

        // @todo make the errors in the index page somehow
        StringBuffer ebuf = null;
        try {
            if ((fErrors != null) && (fErrors.size() != 0)) {
                ebuf = new StringBuffer(COMMON_ERROR_PREFIX);
                for (int i = 0; i < fErrors.size(); i++) {
                    ebuf.append("ERROR ").append(i + 1).append('\n');
                    Throwable t = (Throwable) fErrors.get(i);
                    ebuf.append(t.getMessage()).append('\n');
                    ebuf.append("---------------------------------------------------");
                    ebuf.append(TraceUtils.getAsString(t)).append('\n');
                }
                savePage("there_were_reporting_errors", "reporting errors", ebuf.toString());
            }
        } catch (Throwable t) {
            klog.error("Errors adding errors to reports!", t);
        }

        // deal with the index page (if one was made)
        if (fHtmlReportIndexPage != null) {
            try {
                if (ebuf != null && ebuf.length() > 0) {
                    fHtmlReportIndexPage.addError(ebuf.toString());
                }

                if (fKvt != null && fReportIndexState.keepTrackOfPagesInHtmlIndex()) {
                    this.fHtmlReportIndexPage.addTable("Result files produced in this analysis", fKvt);
                }
                this.fHtmlIndexPageFile = savePage(fHtmlReportIndexPage);
            } catch (Throwable t) {
                klog.error("Error making HtmlIndexPage -- report content may otherwise be OK", t);
            }
        }

        if (fTool_opt != null && fReportParamsFile != null && !fReportParamsFile.exists()) {
            try {
                // save the parameters always to file
                ParserFactory.save(this, fReportParamsFile, add2ParserFactory);

            } catch (Throwable t) {
                klog.error("Error closing report -- suppressing", t);
            }
        }

        if (add2ParserFactory) { // @note added July 2 2006

            try {
                // Hmmm add to cache if not asked to save to parserfactory?
                // if (fReportParamsFile != null && add2ParserFactory) {
                if (fReportParamsFile != null) { //
                    // also add to the reports cache
                    File cacheFile = new File(Application.getVdbManager().getReportsCacheDir(), fReportParamsFile.getName());
                    ParserFactory.save(this, cacheFile, false);
                }
            } catch (Throwable t) {
                klog.error("Error saving report to cahche -- suppressing", t);
            }
        }


        fClosed = true;
    }

    public File createFile(final String name, final String desc) {
        File file = createSafeReportFile(name, fReportDir);
        _centralAddPage(new FileWrapperPage(file, desc));
        return file;
    }

    public File savePage(final GeneSetMatrix gm, final GeneSetMatrixFormatParam gmf) {
        return savePage(gm.getQuickInfo(), gm, gmf);
    }

    public File savePage(final String desc, final GeneSetMatrix gm, final GeneSetMatrixFormatParam gmf) {
    
        try {
            if (gmf.getDataFormat() == DataFormat.GMT_FORMAT) {
                return savePageGmt(desc, gm);
            } else if (gmf.getDataFormat() == DataFormat.GMX_FORMAT) {
                return savePage(desc, gm);
            } else {
                klog.warn("Unkown gm format: " + gmf.getDataFormat());
                return savePageGmt(desc, gm);
            }
        } catch (Throwable t) {
            addError("Trouble saving pob to reports", t);
        }
    
        return null;
    }


    public File savePageGmt(final String desc, final GeneSetMatrix gmt) {
        StringBuffer name = new StringBuffer(gmt.getName()).append('.').append(DataFormat.GMT_FORMAT.getExtension());
        File file = createSafeReportFile(name.toString());
        klog.debug("saving gmt in: " + file);
    
        try {
    
            ParserFactory.saveGmt(gmt, file);
            _centralAddPage(new FileWrapperPage(file, desc));    // @note
    
        } catch (Throwable t) {
            addError("Could not save object to reports object: " + gmt + " in file: " + file.getPath(), t);
        }
    
        return file;
    }


    public File savePage(String desc, final GeneSetMatrix gmx) {
        return savePage(desc, gmx, fReportDir);
    }


    private File createSafeReportFile(final String name) {
        return createSafeReportFile(name, fReportDir);
    }


    public File savePage(String desc, final GeneSetMatrix gmx, final File saveInDir) {
        StringBuffer name = new StringBuffer(gmx.getName()).append('.').append(DataFormat.GMX_FORMAT.getExtension());
        File file = createSafeReportFile(name.toString(), saveInDir);
        klog.debug("saving gmt in: " + file);
    
        try {
    
            ParserFactory.save(gmx, file);
            _centralAddPage(new FileWrapperPage(file, desc));    // @note
    
        } catch (Throwable t) {
            addError("Could not save object to reports object: " + gmx + " in file: " + file.getPath(), t);
        }
    
        return file;
    }


    public File savePageTxt(final String name, final String desc, final String content, final boolean silent, final boolean add2cache) {
        return savePage(name, desc, content, "txt", silent, add2cache);
    }


    public File savePage(final String name, final String desc, final String content, final String ext, final boolean silent, final boolean add2Cache) {
        return savePage(name, desc, content, ext, getReportDir(), silent, add2Cache);
    }


    public File savePage(final String name, final String desc, final String content, final String ext, final File saveInDir, final boolean silent, final boolean add2Cache) {
        StringBuffer fname = new StringBuffer(name).append('.').append(ext);
        File file = createSafeReportFile(fname.toString(), saveInDir);
    
        try {
    
            if (!silent) {
                klog.debug("saving in: " + file);
            }
            
            FileUtils.writeStringToFile(file, content);
            if (add2Cache) {
                _centralAddPage(new FileWrapperPage(file, desc));  // @note
            }
    
        } catch (Throwable t) {
            addError("Could not save object to reports data: " + name + " in file: " + file.getPath(), t);
        }
    
        return file;
    
    }


    private static File _createFile(final String fname, final String suffix, final File inDir) {

        File file = null;

        try {
            StringBuffer name;

            if (fname.endsWith(suffix)) {
                name = new StringBuffer(fname);
            } else {
                name = new StringBuffer(fname).append('.').append(suffix);
            }

            file = createSafeReportFile(name.toString(), inDir);

            if (edu.mit.broad.genome.utils.FileUtils.isLocked(file)) {
                // dont do this as it can make the file name way too long
                // instead just give it a flavor (10 chars) of what the real name is
                //name = new StringBuffer(fname).append(".WARNING_renamed_on_detecting_lock").append(System.currentTimeMillis()).append('.').append(suffix);
                int max = fname.length();
                if (max > 10) {
                    max = 10;
                }
                name = new StringBuffer(fname.substring(0, max)).append(".WARNING_renamed_on_detecting_lock").append(System.currentTimeMillis()).append('.').append(suffix);
                String name_str = NamingConventions.createSafeFileName(name.toString()); // make doubly sure
                file = new File(inDir, name_str);
            }

            if (!file.exists()) {
                boolean made = file.createNewFile();

                if (!made) {
                    throw new IOException("Could not make file: " + file.getAbsolutePath());
                }
            }

        } catch (Throwable t) {
            if (file == null) {
                file = createSafeReportFile("tmp_report_error_file." + System.currentTimeMillis() + suffix, inDir);
            }
            StringBuffer err = new StringBuffer("Fatal error making file to save a component of the reports in");
            err.append("\nInstead using a result file: ").append(file.getPath());
            klog.fatal(err, t);
        }

        return file;
    }

    // replaces chars that make file paths barf such as @ and #
    private static File createSafeReportFile(final String name, final File inDir) {
        return NamingConventions.createSafeFile(inDir, name);
    }

    /**
     * Makes a dir if not already extant
     * Add a pset info file with the timestamp
     *
     * @return sometimes we dont want tools to make explicit sub-dirs
     *         For example some programs, only likes to put output files in the parent dir
     *         Use a -D switch for this
     */
    private static File createIfNeededAndGetReportDir(final File rptWorkingBaseDir,
                                                      final Tool tool,
                                                      final Report rpt) throws IOException {

        if (!rptWorkingBaseDir.exists()) {
            boolean made = rptWorkingBaseDir.mkdir();

            if (!made) {
                throw new IOException("Could not make analysis(-out) results dir at: "
                        + rptWorkingBaseDir);
            }
        }

        // Heres a bit of magix for the GP stuff

        // Now make a report sub-folder ALWAYS
        // but name this WITHOUT a timestamp so that callers can figure out easily
        // Keep this here because of junit linking issues
        //final File rptDir = NamingConventions.generateReportDir(rpt, tool);
        final File rptDir = generateReportDir(rpt, tool);

        if (!rptDir.exists()) {
            boolean made = rptDir.mkdir();

            if (!made) {
                throw new IOException("Could not make a directory to store the Tool reports in. The location attempted was: "
                        + rptDir.getAbsolutePath());
            }
        }

        return rptDir;
    }

    /**
     * analysis_dir/
     * report_label.tool_name.timestamp/
     * <p/>
     * Now make a report sub-folder ALWAYS
     *
     * @param report
     * @param tool
     * @return
     */
    private static File generateReportDir(final Report report, final Tool tool) {

        if (Conf.isMakeReportDirOffMode()) { // use the -out specified dir and dont make a subdir
            return tool.getParamSet().getAnalysisDirParam().getAnalysisDir();
        } else { // make a report sub dir

            final ReportLabelParam lp = tool.getParamSet().getReportLabelParam();
            String label = lp.getReportLabel();

            if (label == null || label.length() == 0) {
                label = "my_report";
            }

            label = label.trim();

            StringTokenizer tok = new StringTokenizer(tool.getClass().getName(), ".");
            String toolName = null;

            while (tok.hasMoreTokens()) {
                toolName = tok.nextToken();
            }

            final StringBuffer rptName = new StringBuffer(label).append('.').append(toolName);
            rptName.append('.').append(report.getTimestamp());

            return new File(tool.getParamSet().getAnalysisDirParam().getAnalysisDir(), rptName.toString());
        }
    }

    /**
     * Inner class for class objectiviy when adding pages
     * <p/>
     * Also manages some of the business rules when pages are added - event firing etc
     */
    private static class Pages implements java.io.Serializable {

        private List<Page> plist;
        private List<File> flist;

        Pages() {
            this.plist = new ArrayList<Page>();
            this.flist = new ArrayList<File>();
        }

        private void writeObject(java.io.ObjectOutputStream out) {
        	if (klog.isDebugEnabled()) {
        		klog.debug("Ignoring: " + out);
        	}
        }

        private void readObject(java.io.ObjectInputStream in) {
        	if (klog.isDebugEnabled()) {
        		klog.debug("Ignoring: " + in);
        	}
        }

        void add(Page page, File file) {
            plist.add(page);
            flist.add(file);
        }

        void add(final FileWrapperPage page) {
            plist.add(page);
            flist.add(page.getFile());
        }

        List<File> getFiles_list() {
            return flist;
        }
    }
}