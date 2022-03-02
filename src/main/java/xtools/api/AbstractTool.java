/*
 * Copyright (c) 2003-2022 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xtools.api;

import edu.mit.broad.genome.Conf;
import edu.mit.broad.genome.JarResources;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xapps.gsea.UpdateChecker;
import xtools.api.param.*;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Interface for a runnable Tool.
 *
 * @author Aravind Subramanian, David Eby
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

    protected ToolParamSet fParamSet;

    private edu.mit.broad.genome.utils.Timer fTimer;

    private boolean fHelpMode;

    protected static final Object[] EMPTY_OBJECTS = new Object[]{};
    
    // ----------- PARAMETERS COMMON TO ALL TOOLS -------------
    protected final BooleanParam fHelpParam = new BooleanParam("help",
            "Usage information on the tool", false);

    private ReportDirParam fReportDirParam;
    private GuiParam fGuiParam;
    private ReportLabelParam fRptLabelParam;
    protected ToolReport fReport;

    // @maint If the standard params above are updated, this var needs attention
    private static int SHARED_PARAM_CNT = 4;

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

    // Dont call declareParams()  - class vars arent yet inited
    // constructed using instantiation and npe is thrown.
    protected AbstractTool(final String toolName) {
        this.fTimer = new edu.mit.broad.genome.utils.Timer();
        this.log = LoggerFactory.getLogger(this.getClass());

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
                    log.warn("Ignoring param_file key: {} value: {} in favor of cmd line value: {}", param_name, param_val, extant_param_val);
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
        StringBuilder buf = new StringBuilder();
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
        if (!fReport.getToolComments().isEmpty()) {
            if (fReport.getIndexPage() != null) {
                Div div = new Div();
                H4 h4 = new H4("Comments");
                div.addElement(h4);
                div.addElement(fReport.getToolComments().toHTML());
                fReport.getIndexPage().addBlock(div, false);
            }
        }
        
        if (!fReport.getToolWarnings().isEmpty()) {
            if (fReport.getIndexPage() != null) {
                Div div = new Div();
                H4 h4 = new H4("Warnings");
                h4.addAttribute("style", "color: magenta;");
                div.addElement(h4);
                div.addElement(fReport.getToolWarnings().toHTML());
                fReport.getIndexPage().addBlock(div, false);
            }
        }
        
        if (fReport.getIndexPage() != null) {
            Div div = new Div();
            H4 h4 = new H4("Citing GSEA and MSigDB");
            div.addElement(h4);
            P citingGsea = new P();
            StringElement citingGseaText = new StringElement("To cite your use of the GSEA software please reference the following:");
            LI citingGsea2005 = new LI(HtmlFormat.Links.hyper("Subramanian, A., Tamayo, P., et al. (2005, PNAS). ", 
                    "https://www.pnas.org/content/102/43/15545", null));
            LI citingGsea2003 = new LI(HtmlFormat.Links.hyper("Mootha, V. K., Lindgren, C. M., et al. (2003, Nature Genetics). ", 
                    "http://www.nature.com/ng/journal/v34/n3/abs/ng1180.html", null));
            citingGsea.addElement(citingGseaText).addElement(new UL().addElement(citingGsea2005).addElement(citingGsea2003));
            div.addElement(citingGsea);
            P citingMSigDB = new P();
            StringElement citingMSigDBText = new StringElement("For use of the Molecular Signatures Database (MSigDB), "
                    + "to cite please reference <br/ >one or more of the following as appropriate, "
                    + "along with the source for the gene set as listed on the gene set page: ");
            LI citingMSigDB2011 = new LI(HtmlFormat.Links.hyper("Liberzon A, et al. (Bioinformatics, 2011). ", 
                    "https://doi.org/10.1093/bioinformatics/btr260", null));
            LI citingMSigDB2015 = new LI(HtmlFormat.Links.hyper("Liberzon A, et al. (Cell Systems 2015). ", 
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

    // TODO: parameterize the Class with the correct generic type
    private int countParamFields(Class cl) {
        Field[] fields = cl.getDeclaredFields();
        int paramcnt = 0;

        for (int i = 0; i < fields.length; i++) {
            Class fc = fields[i].getType();
            boolean isParam = Param.class.isAssignableFrom(fc);

            if (isParam) { paramcnt++; }
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
            StringBuilder buf = new StringBuilder("Have you forgotten to update declareParams()?").append('\n');
            buf.append("In ParamSet # declared: ").append(fParamSet.getNumParams()).append(" is NOT equal to deduced thro reflection # : ").append(paramCnt);

            throw new IllegalStateException(buf.toString());
        }
    }

    public boolean isHelpMode() { return fHelpMode; }

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
