/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xtools.api;

import edu.mit.broad.genome.Conf;
import edu.mit.broad.genome.JarResources;
import edu.mit.broad.genome.alg.GeneSetGenerators;
import edu.mit.broad.genome.objects.*;
import edu.mit.broad.genome.parsers.ParseUtils;
import edu.mit.broad.genome.reports.api.Report;
import edu.mit.broad.genome.reports.api.ReportIndexState;
import edu.mit.broad.genome.reports.api.ToolReport;
import edu.mit.broad.genome.utils.ClassUtils;
import edu.mit.broad.genome.utils.CmdLineArgs;
import edu.mit.broad.genome.utils.SystemUtils;
import edu.mit.broad.vdb.chip.Chip;
import edu.mit.broad.xbench.core.api.Application;

import org.apache.ecs.html.Div;
import org.apache.ecs.html.H4;
import org.apache.log4j.Logger;

import xapps.gsea.UpdateChecker;
import xtools.api.param.*;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Interface for a runnable Tool.
 * <p/>
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
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

    /**
     * For logging support
     */
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
        return JarResources.getHelpURL(getName());
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
    protected void init(final Properties prp) {

        fParamSet.addParam(fHelpParam);

        declareParams();

        if (prp != null) {
            // by default, if help spec do usage
            if (prp.containsKey("help") || prp.containsKey("HELP")) {
                this.fHelpMode = true;
                /*
                String val = prp.getProperty("help");
                if (val != null && val.equalsIgnoreCase("false")) {
                    this.fHelpMode = false;
                } else {
                    this.fHelpMode = true;
                }
                */
            }
        }

        //System.out.println(">>>> prp: " + prp + " " + fHelpMode + " val>>" + prp.getProperty("help"));

        if (isHelpMode()) {
            // no filling nor checking
            fParamSet.printfUsage();
            Conf.exitSystem(false);
        } else {
            fParamSet.fill(prp);
            //ParamSet.printf();
            fParamSet.check();
            ensureAllDeclaredWereAdded();
            checkHeadlessState();
        }

    }

    protected void init(final String[] args) {

        try {

            Properties prp = CmdLineArgs.parse(args);

            String argline = CmdLineArgs.toString(args);
            int index = argline.indexOf("help");
            if (index != -1) {
                prp.setProperty("help", Boolean.TRUE.toString());
            }

            String param_file_path = prp.getProperty(ParamSet.PARAM_FILE);
            if (param_file_path != null && param_file_path.length() > 0) {
                enhanceParams(param_file_path, prp);
            }
            
            init(prp);

        } catch (Throwable t) {
            t.printStackTrace();
            // if the rpt dir was made try to rename it so that easily identifiable
            if (fReport != null) {
                fReport.setErroredOut();
            } else {
                log.info("No report dir was made yet (analysis errored out)");
            }

            Conf.exitSystem(true);
        }
    }

    /*
     * The filepath was formerly treated as a path OR a URL, but this lead to errors on Windows where
     * our URL detection fails to identify 'C:/' as a path.  We might revamp this later (see GSEA-1170)
     * but for now these will be restricted to being handled as *local files only*.  Loading param_files
     * from URL seems like an unlikely use-case anyway.
     */
    private void enhanceParams(final String filepath, final Properties prp) throws Exception {
        Properties addPrp = ParseUtils.readKeyVal(filepath, false, true, false);
        for (Iterator it = addPrp.keySet().iterator(); it.hasNext();) {
            String param_name = it.next().toString();
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
                "<h3 style=\"text-align: left\"><font color=\"#808080\">Report for: ").append(getName()).append("</font></h3>\n").append("</div>");
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

        if (fReport.getToolComments().isEmpty() == false) {
            Div div = new Div();
            H4 h4 = new H4("Comments");
            div.addElement(h4);
            div.addElement(fReport.getToolComments().toHTML());
            if (fReport.getIndexPage() != null) {
                fReport.getIndexPage().addBlock(div, false);
            }
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

    public String getName() {
        return this.getClass().getName();
    }

    public String getTitle() {
        String sn = ClassUtils.shorten(getName());
        String desc = getDesc();

        if (desc == null || desc.length() == 0) {
            return "<html><body><b>" + sn + "</b>" + "</body></html>";
        } else {
            return "<html><body><b>" + sn + "</b>: " + desc + "</body></html>";
        }

        //return "<html><body><b>" + sn.toUpperCase() + "</b>: Set parameters and run enrichment tests</body></html>";
    }

    // Real tools must override
    public String getDesc() {
        return "";
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
            List list = new ArrayList();
            for (int i = 0; i < gsets.length; i++) {
                if (gsets[i].getNumMembers() > 0) {
                    list.add(gsets[i]);
                }
            }

            return (GeneSet[]) list.toArray(new GeneSet[list.size()]);
        }
    }

    public static void setChip(final Dataset ds, final ChipOptParam chipOptParam) throws Exception {
        if (chipOptParam != null && chipOptParam.isSpecified()) {
            setChip(ds, chipOptParam.getChip());
        }
    }

    private static void setChip(final Dataset ds, final Chip chip) throws Exception {
        if (chip != null) {
            ds.getAnnot().setChip(chip, null);
        }
    }

}    // End AbstractTool
