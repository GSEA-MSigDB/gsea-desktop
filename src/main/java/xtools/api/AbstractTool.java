/*
 * Copyright (c) 2003-2021 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xtools.api;

import edu.mit.broad.genome.Conf;
import edu.mit.broad.genome.JarResources;
import edu.mit.broad.genome.alg.GeneSetGenerators;
import edu.mit.broad.genome.objects.*;
import edu.mit.broad.genome.parsers.ParseUtils;
import edu.mit.broad.genome.reports.api.Report;
import edu.mit.broad.genome.reports.api.ReportIndexState;
import edu.mit.broad.genome.reports.api.ToolReport;
import edu.mit.broad.genome.reports.pages.HtmlFormat;
import edu.mit.broad.genome.utils.ClassUtils;
import edu.mit.broad.genome.utils.CmdLineArgs;
import edu.mit.broad.genome.utils.SystemUtils;
import edu.mit.broad.vdb.chip.Chip;
import edu.mit.broad.xbench.core.api.Application;

import org.apache.commons.lang3.StringUtils;
import org.apache.ecs.StringElement;
import org.apache.ecs.html.Div;
import org.apache.ecs.html.H4;
import org.apache.ecs.html.LI;
import org.apache.ecs.html.P;
import org.apache.ecs.html.UL;
import org.apache.log4j.Logger;

import xapps.gsea.UpdateChecker;
import xtools.api.param.*;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Interface for a runnable Tool.
 *
 * @author Aravind Subramanian
 */
public abstract class AbstractTool implements Tool {

    // @imp note: needed here as otherwise jar resources tries to load icons
    static {
        if (!SystemUtils.isPropertyDefined("java.awt.headless")) {
            System.setProperty("java.awt.headless", "true");
        }
    }

    public static final String REPORT_INDEX = "xtools.report_index";

    private PrintStream fOut;

    protected transient Logger log;

    protected static final transient Logger klog = Logger.getLogger(AbstractTool.class);

    protected ToolParamSet fParamSet;

    private edu.mit.broad.genome.utils.Timer fTimer;

    private boolean fHelpMode;

    protected static final Object[] EMPTY_OBJECTS = new Object[]{};
    
    /**
     * ----------- PARAMETERS COMMON TO ALL TOOLS -------------
     */
    protected final BooleanParam fHelpParam = new BooleanParam("help",
            "Usage information on the tool", false);

    private ReportDirParam fReportDirParam;
    private GuiParam fGuiParam;
    private ReportLabelParam fRptLabelParam;
    protected ToolReport fReport;

    /**
     * @maint If the standard params above are updated, this var needs attention
     */
    private static int SHARED_PARAM_CNT = 4;

    /**
     * Class constructor
     */
    protected AbstractTool() {
        this("dummy");
    }

    public String getHelpURL() {
        return JarResources.getHelpURL(getClass().getName());
    }

    public PrintStream getOutputStream() {
        if (fOut == null) {
            fOut = System.out; // default
        }

        return fOut;
    }

    /**
     * Only for junit usage
     *
     * @param toolName
     */

    // Dont call declareParams()  - class vars arent yet inited
    // constructed using instantiation and npe is thrown.
    protected AbstractTool(final String toolName) {

        this.fTimer = new edu.mit.broad.genome.utils.Timer();
        //this.fHelpMode = Boolean.getBoolean(System.getProperty("help")); // just doesnt work!!
        //this.fHelpMode = SystemUtils.isPropertyTrue("help");
        this.log = Logger.getLogger(this.getClass());

        if (Application.isHandlerSet() == false) {
            Application.registerHandler(new XToolsApplication());
        }

        this.fParamSet = new ToolParamSet();

        this.fGuiParam = new GuiParam();
        this.fRptLabelParam = new ReportLabelParam(false);
        this.fReportDirParam = new ReportDirParam(false);

        // @note always add the common ones
        fParamSet.addParam(fGuiParam);
        fParamSet.addParam(fReportDirParam);
        fParamSet.addParam(fRptLabelParam);
    }
    
    /**
     * subclasses must call in their class constructor after a call to super()
     */
    protected void init(final Properties prp, String paramFilePath) {
        fParamSet.addParam(fHelpParam);

        declareParams();

        if (prp != null) {
            // by default, if help spec do usage
            if (prp.containsKey("help") || prp.containsKey("HELP")) {
                this.fHelpMode = true;
            }
        }

        //System.out.println(">>>> prp: " + prp + " " + fHelpMode + " val>>" + prp.getProperty("help"));

        if (isHelpMode()) {
            // no filling nor checking
            fParamSet.printfUsage();
            Conf.exitSystem(false);
        } else {
            try {
                if (StringUtils.isNotBlank(paramFilePath)) {
                    enhanceParams(paramFilePath, prp);
                }

                fParamSet.fill(prp);
                //ParamSet.printf();
                fParamSet.check();
                ensureAllDeclaredWereAdded();
                checkHeadlessState();

            } catch (Throwable t) {
                t.printStackTrace();
                // if the rpt dir was made try to rename it so that easily identifiable
                if (fReport != null) {
                    fReport.setErroredOut();
                } else {
                    log.info("No report dir was made yet (analysis errored out)");
                }
            }
        }
    }

    protected void init(final String[] args) {
        Properties prp = CmdLineArgs.parse(args);

        String argline = CmdLineArgs.toString(args);
        int index = argline.indexOf("help");
        if (index != -1) {
            prp.setProperty("help", Boolean.TRUE.toString());
        }

        String param_file_path = prp.getProperty(ParamSet.PARAM_FILE);
        init(prp, param_file_path);
    }

    /*
     * The filepath was formerly treated as a path OR a URL, but this lead to errors on Windows where
     * our URL detection fails to identify 'C:/' as a path.  We might revamp this later (see GSEA-1170)
     * but for now these will be restricted to being handled as *local files only*.  Loading param_files
     * from URL seems like an unlikely use-case anyway.
     */
    private void enhanceParams(final String filepath, final Properties prp) throws Exception {
        Properties addPrp = ParseUtils.readKeyVal(filepath);
        for (String param_name : addPrp.stringPropertyNames()) {
            String param_val = addPrp.getProperty(param_name);
            if (prp.containsKey(param_name)) {
                String extant_param_val = prp.getProperty(param_name);
                if (!param_val.equals(extant_param_val)) {
                    log.warn("Ignoring param_file key: " + param_name + " value: " + param_val + " in favor of cmd line value: " + extant_param_val);
                }
            } else {
                prp.setProperty(param_name, param_val);
            }
        }
    }

    protected void startExec() throws IOException {
        startExec(getHeader());
    }

    protected String getHeader() {
        StringBuffer buf = new StringBuffer();
        buf.append("<div id=\"footer\" style=\"width: 905; height: 35\">\n").append(
                "<h3 style=\"text-align: left\"><font color=\"#808080\">Report for: ").append(getClass().getName()).append("</font></h3>\n").append("</div>");
        return buf.toString();
    }

    protected void startExec(final String optHeader) throws IOException {
        this.startExec(makeReportIndexPage(), optHeader); // leave report sub dir making ON
    }

    // @note this is the core start report method
    protected void startExec(final ReportIndexState indexState) throws IOException {
        UpdateChecker.oneTimeGseaUpdateCheck(null);
        fTimer.start();
        fReport = new ToolReport(this, true, indexState);
        //log.info("Running " + getName() + " with reports: " + fRptLabelParam.getReportLabel() + " folder: " + fReport.getReportDir() + " indexState: " + indexState.toString());
    }

    protected void startExec(final boolean makeReportIndexPage, final String headerOpt) throws IOException {
        this.startExec(new ReportIndexState(makeReportIndexPage, headerOpt));
    }

    private boolean makeReportIndexPage() {
        String p = System.getProperty(REPORT_INDEX);

        if (p == null || p.length() == 0) {
            return true; // @note IMP if not specified then it is ON
        } else {
            return Boolean.valueOf(p).booleanValue();
        }
    }

    public void doneExec() {
        //fOut.close(); // dont let the giver take care of that

        if (!fReport.getToolComments().isEmpty()) {
            if (fReport.getIndexPage() != null) {
                Div div = new Div();
                H4 h4 = new H4("Comments");
                div.addElement(h4);
                div.addElement(fReport.getToolComments().toHTML());
                fReport.getIndexPage().addBlock(div, false);
            }
        }
        
        if (fReport.getIndexPage() != null) {
            Div div = new Div();
            H4 h4 = new H4("Citing GSEA and MSigDB");
            div.addElement(h4);
            P citingGsea = new P();
            StringElement citingGseaText = new StringElement("To cite your use of the GSEA software please reference the following:");
            LI citingGsea2005 = new LI(HtmlFormat.Links.hyper("Subramanian, A., Tamayo, P., "
                    + "Mootha, V. K., Mukherjee, S., Ebert, B. L., Gillette, M. A., Paulovich, A., "
                    + "Pomeroy, S. L., Golub, T. R., Lander, E. S. & Mesirov, J. P. (2005)  "
                    + "Gene set enrichment analysis: A knowledge-based approach for interpreting genome-wide expression profiles. "
                    + "Proc. Natl. Acad. Sci. USA 102, 15545-15550.", 
                    "http://www.pnas.org/cgi/content/abstract/102/43/15545", null));
            LI citingGsea2003 = new LI(HtmlFormat.Links.hyper("Mootha, V. K., Lindgren, C. M., "
                    + "Eriksson, K. F., Subramanian, A., Sihag, S., Lehar, J., Puigserver, P., Carlsson, "
                    + "E., Ridderstrale, M., Laurila, E., et al. (2003). PGC-1alpha-responsive genes involved in oxidative "
                    + "phosphorylation are coordinately downregulated in human diabetes. Nat Genet 34, 267-273.", 
                    "http://www.nature.com/ng/journal/v34/n3/abs/ng1180.html", null));
            citingGsea.addElement(citingGseaText).addElement(new UL().addElement(citingGsea2005).addElement(citingGsea2003));
            div.addElement(citingGsea);
            P citingMSigDB = new P();
            StringElement citingMSigDBText = new StringElement("To cite your use of the Molecular Signatures Database (MSigDB) "
                    + "please reference one or more of the following as appropriate, "
                    + "along with the source for the gene set as listed on the gene set page: ");
            LI citingMSigDB2011 = new LI(HtmlFormat.Links.hyper("Liberzon A, Subramanian A, Pinchback R, "
                    + "Thorvaldsdóttir H, Tamayo P, Mesirov JP. Molecular signatures database (MSigDB) 3.0. "
                    + "Bioinformatics. 2011 Jun 15;27(12):1739-40. doi: 10.1093/bioinformatics/btr260."
                    + "Epub 2011 May 5. PMID: 21546393; PMCID: PMC3106198.", 
                    "https://doi.org/10.1093/bioinformatics/btr260", null));
            LI citingMSigDB2015 = new LI(HtmlFormat.Links.hyper("Liberzon A, Birger C, Thorvaldsdóttir H, "
                    + "Ghandi M, Mesirov JP, Tamayo P. The Molecular Signatures Database (MSigDB) hallmark "
                    + "gene set collection. Cell Syst. 2015 Dec 23;1(6):417-425.", 
                    "https://www.ncbi.nlm.nih.gov/pmc/articles/PMC4707969/", null));
            citingMSigDB.addElement(citingMSigDBText).addElement(new UL().addElement(citingMSigDB2011).addElement(citingMSigDB2015));
            div.addElement(citingMSigDB);

            fReport.getIndexPage().addBlock(div, false);
        }

        fReport.closeReport(true);

        if (fGuiParam.isTrue()) {
            fReport.display();
        }

        fTimer.stop();
        fTimer.printTimeTakenS();
    }

    public Report getReport() {
        return fReport;
    }

    /**
     * Make sure that if we are headless (aka on unix, lsf) that the GUI param is always off
     */
    private void checkHeadlessState() {

        /* comm out dec 18
        if (GraphicsEnvironment.isHeadless()) {
            GuiParam gui = fParamSet.getGuiParam();
            if (gui == null) {
                gui = new GuiParam();
                fParamSet.addParam(gui);
            }
            gui.setValue(false); // always
        }
        */
    }

    // TODO: parameterize the Class with the correct generic type
    private int countParamFields(Class cl) {
        //log.debug("Counting for class name: " + cl.getName());
        Field[] fields = cl.getDeclaredFields();
        int paramcnt = 0;

        for (int i = 0; i < fields.length; i++) {

            //log.debug(("name = " + fields[i].getName() + " " + fields[i].getDeclaringClass() + " class: " + fields[i].getType()));
            Class fc = fields[i].getType();
            boolean isParam = Param.class.isAssignableFrom(fc);

            if (isParam) {
                paramcnt++;
                //log.debug("Param: " + fields[i].getName());
            }
        }

        return paramcnt;
    }

    /**
     * problem -> declared variables but forget to add to param set
     * (its in a diff method)
     * cant automagically add all declared variables -> relect not possible
     * So instread use reflect to check (by count) that all declared vars have been added
     * <p/>
     * IMP: However, doesnt work if the tool is another abstract subclass (make the subclass run the cjheck, see above)
     * Fixed, in a hack way: hardcoded name of this class
     * Only supports 1 additional subclass!!
     */
    protected void ensureAllDeclaredWereAdded() {

        int paramCnt = SHARED_PARAM_CNT;

        paramCnt += countParamFields(this.getClass());

        if (!this.getClass().getSuperclass().getName().equals("xtools.api.AbstractTool")) { // @note
            paramCnt += countParamFields(this.getClass().getSuperclass());
        }

        if (!this.getClass().getSuperclass().getSuperclass().getName().equals("xtools.api.AbstractTool")) { // @note
            paramCnt += countParamFields(this.getClass().getSuperclass().getSuperclass());
        }

        if (fParamSet.getNumParams() != paramCnt) {
            StringBuffer buf =
                    new StringBuffer("Have you forgotten to update declareParams()?").append('\n');

            buf.append("In ParamSet # declared: ").append(fParamSet.getNumParams()).append(" is NOT equal to deduced thro reflection # : ").append(paramCnt);

            throw new IllegalStateException(buf.toString());
        }

    }

    public boolean isHelpMode() {
        return fHelpMode;
    }

    public ParamSet getParamSet() {
        fParamSet.sort();
        return fParamSet;
    }

    public String getTitle() {
        String sn = ClassUtils.shorten(getClass().getName());
        String desc = getDesc();

        if (desc == null || desc.length() == 0) {
            return "<html><body><b>" + sn + "</b>" + "</body></html>";
        } else {
            return "<html><body><b>" + sn + "</b>: " + desc + "</body></html>";
        }
    }

    // Real tools must override
    public String getDesc() {
        return "";
    }

    public static BooleanParam createZipReportParam(final boolean reqd) {
        return new BooleanParam("zip_report", "Make a zipped file with all reports",
                "Create a zipped file with all files made by the report. This can be emailed to share results",
                false, reqd, Param.ADVANCED);
    }

    protected static void tool_main(final AbstractTool tool) {

        if (tool == null) {
            throw new IllegalArgumentException("Param tool cannot be null");
        }

        boolean was_error = false;
        try {

            tool.execute();

        } catch (Throwable t) {
            // if the rpt dir was made try to rename it so that easily identifiable
            was_error = true;
            t.printStackTrace();
        }

        if (was_error && tool.getReport() != null) {
            tool.getReport().setErroredOut();
        }

        if (tool.getParamSet().getGuiParam().isFalse()) {
            Conf.exitSystem(was_error);
        } else {
            // dont exit!!
        }
    }

    /**
     * Method to run the given tool and deal with top-level error handling, meant to be used by
     * the GP modules.  Unlike tool_main() this does *not* handle process exit, allowing the module
     * to safely do some post-processing after the run.
     * 
     * Callers should exit via Conf.exitSystem() after completion.
     */
    public static boolean module_main(final AbstractTool tool) {
        if (tool == null) {
            throw new IllegalArgumentException("Param tool cannot be null");
        }
        try {
            tool.execute();
            return true;
        } catch (Throwable t) {
            // if the rpt dir was made try to rename it so that easily identifiable
            t.printStackTrace();
            if (tool.getReport() != null) {
                tool.getReport().setErroredOut();
            }
            return false;
        }
    }

    /**
     * A number of Helper methods for dealing with params, common extractions etc
     */
    public static class Helper {

        // The magic here is:
        // the ds and the gene sets have to match
        // The ds prior to this call was either collapsed or not collapsed
        // If not collapsed, do nothing at all. If it was collapsed
        // If they dont match (i.e all gsets are 0) using the chip to map the gsets
        // in two ways:
        // 1) from their source format to gene symbols
        public static GeneSet[] getGeneSets(final Object ds_or_rl,
                                            GeneSet[] gsets,
                                            final IntegerParam geneSetMinSizeParam,
                                            final IntegerParam geneSetMaxSizeParam) throws Exception {

            if (geneSetMaxSizeParam.getIValue() < geneSetMinSizeParam.getIValue()) {
                throw new IllegalArgumentException("Max size cannot be less than min size");
            }

            klog.info("Got gsets: " + gsets.length + " now preprocessing them ... min: " + geneSetMinSizeParam.getIValue() + " max: " + geneSetMaxSizeParam.getIValue());

            if (geneSetMinSizeParam.getIValue() != geneSetMaxSizeParam.getIValue()) {
                //int bef = gsets.length;
                boolean do_cloning;
                if (geneSetMinSizeParam.isSpecified()) {
                    gsets = GeneSetGenerators.removeGeneSetsSmallerThan(gsets, geneSetMinSizeParam.getIValue(), ds_or_rl, true);
                    do_cloning = false;
                } else {
                    do_cloning = true;
                }

                klog.info("Done preproc for smaller than: " + geneSetMinSizeParam.getIValue());

                if (geneSetMaxSizeParam.isSpecified()) {
                    gsets = GeneSetGenerators.removeGeneSetsLargerThan(gsets, geneSetMaxSizeParam.getIValue(), ds_or_rl, do_cloning);
                }


            } else { // @note hack
                klog.info("Skipped gene set size filtering");
            }

            klog.debug("Done geneset preproc starting analysis ...");

            // Finally remove all 0 size gene sets (if min is 0 these will still be in there)
            return removeAllZeroMemberSets(gsets);
        }

        private static GeneSet[] removeAllZeroMemberSets(final GeneSet[] gsets) {

            // Finally remove all 0 size gene sets (if min is 0 these will still be in there)
            List<GeneSet> list = new ArrayList<GeneSet>();
            for (int i = 0; i < gsets.length; i++) {
                if (gsets[i].getNumMembers() > 0) {
                    list.add(gsets[i]);
                }
            }

            return list.toArray(new GeneSet[list.size()]);
        }
    }

    public static void setChip(final Dataset ds, final ChipOptParam chipOptParam) throws Exception {
        if (chipOptParam != null && chipOptParam.isSpecified()) {
            setChip(ds, chipOptParam.getChip());
        }
    }

    private static void setChip(final Dataset ds, final Chip chip) throws Exception {
        if (chip != null) {
            ds.getAnnot().setChip(chip);
        }
    }
}