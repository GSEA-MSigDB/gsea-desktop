/*
 * Copyright (c) 2003-2022 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.parsers;

import edu.mit.broad.genome.Constants;
import edu.mit.broad.genome.JarResources;
import edu.mit.broad.genome.NamingConventions;
import edu.mit.broad.genome.math.Matrix;
import edu.mit.broad.genome.objects.*;
import edu.mit.broad.genome.objects.esmatrix.db.EnrichmentDb;
import edu.mit.broad.genome.reports.api.Report;
import edu.mit.broad.genome.utils.SystemUtils;
import edu.mit.broad.vdb.chip.Chip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.filechooser.FileView;
import java.io.File;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.Map;

/**
 * Central repository for data formats.
 * A DataFormat is primarily identified by a file extension.
 * Also associates things like a desc, icon etc with DataFormats.
 *
 * @author Aravind Subramanian
 */
// TODO: parameterize class, throughout
public class DataFormat extends DataType implements Constants {
    public DataFormat() { }

    private static final Logger klog = LoggerFactory.getLogger(DataFormat.class);

    public static final DataFormat XLS_FORMAT = new DataFormat(ExtFormat.class, "Excel",
            "Microsoft Excel", Constants.XLS,
            JarResources.getIcon("Xls.gif"), null);
    public static final DataFormat TXT_FORMAT = new DataFormat(ExtFormat.class, "Text",
            "Plain Text", "txt",
            JarResources.getIcon("Txt.gif"), null);
    public static final DataFormat XML_FORMAT = new DataFormat(ExtFormat.class, "XML",
            "Extensible Markup Language", Constants.XML,
            JarResources.getIcon("Xml.gif"), null);
    public static final DataFormat TSV_FORMAT = new DataFormat(ExtFormat.class, "TSV",
            "Tab-separated values", Constants.TSV,
            JarResources.getIcon("Xls.gif"), null);

    /**
     * BROAD CANCER res format for datasets
     */
    public static final DataFormat RES_FORMAT = new DataFormat(Dataset.class, "Dataset",
            "MIT Format for a Dataset with P-Calls", RES,
            JarResources.getIcon("Res16.gif"), ResParser.class);
    public static final DataFormat GCT_FORMAT = new DataFormat(Dataset.class, "Dataset",
            "MIT Format for a Dataset", GCT,
            JarResources.getIcon("Gct16.gif"), GctParser.class);

    public static final DataFormat TXT_DATASET_FORMAT = new DataFormat(Dataset.class, "Dataset",
            "generic Txt Format for Datasets", TXT,
            JarResources.getIcon("Txt.gif"), TxtDatasetParser.class);

    public static final DataFormat PCL_FORMAT = new DataFormat(Dataset.class, "Dataset",
            "Stanford Format for a Dataset", "pcl",
            JarResources.getIcon("Pcl.gif"), PclParser.class);

    /**
     * MIT cls format for class vectors
     */
    public static final DataFormat CLS_FORMAT = new DataFormat(Template.class, "Template",
            "MIT Format for Class Labels", CLS,
            JarResources.getIcon("Cls.gif"), ClsParser.class);


    public static final DataFormat GRP_FORMAT = new DataFormat(GeneSet.class, "GeneSet",
            "MIT GeneSet Format", GRP,
            JarResources.getIcon("Grp.gif"), GeneSetParser.class);

    public static final DataFormat RNK_FORMAT = new DataFormat(RankedList.class, "RankedList",
            "MIT RankedList Format", RNK,
            JarResources.getIcon("Rnk.png"), RankedListParser.class);

    public static final DataFormat GMX_FORMAT = new DataFormat(GeneSetMatrix.class, "GeneSetMatrix",
            "MIT format for a Matrix of Gene Sets",
            Constants.GMX, JarResources.getIcon("Gmx.png"), GmxParser.class);

    public static final DataFormat GMT_FORMAT = new DataFormat(GeneSetMatrix.class,
            "GeneSetMatrix_Transposed",
            "MIT format for a Matrix of Gene Sets",
            Constants.GMT, JarResources.getIcon("Gmt.png"), GmtParser.class);

    public static final DataFormat EDB_FORMAT = new DataFormat(EnrichmentDb.class,
            "Enrichment-Database",
            "MIT format for an enrichment database",
            Constants.EDB, JarResources.getIcon("Edb.png"), EdbFolderParser.class);

    public static final DataFormat RPT_FORMAT = new DataFormat(Report.class, "Report",
            "Report for a program",
            Constants.RPT, JarResources.getIcon("Rpt.gif"), ReportParser.class);

    public static final DataFormat CHIP_FORMAT = new DataFormat(Chip.class, "Chip",
            "Chip",
            Constants.CHIP, JarResources.getIcon("Chip16.png"), ChipParser.class);

    /**
     * @maint manually keep in synch with declared formats
     */

    // imp to not expose
    private static final DataFormat[] ALL = new DataFormat[]{
            RES_FORMAT, GCT_FORMAT, TXT_DATASET_FORMAT, PCL_FORMAT,
            CLS_FORMAT,
            GRP_FORMAT, GMT_FORMAT, GMX_FORMAT,
            RNK_FORMAT,
            RPT_FORMAT,
            EDB_FORMAT,
            CHIP_FORMAT,
            XLS_FORMAT, TXT_FORMAT, XML_FORMAT, TSV_FORMAT
    };

    /**
     * @maint
     */
    public static final DataFormat[] ALL_DATASET_FORMATS = new DataFormat[]
            {GCT_FORMAT, RES_FORMAT, TXT_DATASET_FORMAT, PCL_FORMAT};

    public static final DataFormat[] ALL_GENESETMATRIX_FORMATS = new DataFormat[]
            {GMX_FORMAT, GMT_FORMAT};  // Seems like GRP should be here...

    static class ParsableFileView extends FileView {

        /**
         * Custom icons for file types that have associated actions.
         */
        public Icon getIcon(final File file) {
            return DataFormat.getIconOrNull(file);
        }

        /**
         * Default handling.
         * Let the L&F FileView figure this out.
         */
        public String getTypeDescription(final File file) {
            return null;
        }

        /**
         * Default handling.
         * Let the L&F FileView figure this out.
         */
        public String getDescription(final File file) {
            return DataFormat.getDesc(file);
        }

    }    // End class ParsableFileView


    public static FileView getParsableFileView() {
        return new ParsableFileView();
    }


    /**
     * key -> Class (one of the pobs), value -> string ext
     */
    private static final Map<Class, String> kClassExtMap = new HashMap<Class, String>();

    /**
     * key -> ext, value -> DataFormat that represents that ext
     */
    private static final Map<String, DataFormat> kExtDfMap = new HashMap<String, DataFormat>();

    /**
     * key -> classname, value -> icon to represent the class
     */
    private static final Map<Class, Icon> kClassIconMap = new HashMap<Class, Icon>();

    /**
     * key -> class, value->corresp dataformats name
     */
    private static final Map<Class, String> kClassNameMap = new HashMap<Class, String>();

    static {
        /*
         * Hash the internal data formats as client code deals with them
         * in object form and nees to lookup things like, extension, icon etc.
         */
        for (int i = 0; i < ALL.length; i++) {
            kExtDfMap.put(ALL[i].getExtension(), ALL[i]);

            Class repClass = ALL[i].getRepresentationClass();

            if (ExtFormat.class.isAssignableFrom(repClass)) {
                // dont bother hashing
            } else {

                // use the first one as the default
                if (!kClassNameMap.containsKey(repClass)) {
                    kClassNameMap.put(repClass, ALL[i].getName());
                }
                if (!kClassExtMap.containsKey(repClass)) {
                    kClassExtMap.put(repClass, ALL[i].getExtension());
                }
                if (!kClassIconMap.containsKey(repClass)) {
                    kClassIconMap.put(repClass, ALL[i].getIcon());
                }
            }
        }
    }

    /*
     * Class variables
     */
    private String fName;
    private String fDesc;
    private Icon fIcon;
    private String fExt;
    // TODO: remove this apparently unused field, clean up.
    // It should be safe to do so, but I have a small fear that it's somehow used by reflection.
    private Class fParserClass;
    private FilenameFilter fFilt;

    /**
     * Class to represent a (and all) external data formats.
     * Such as ms excel, adobe pdf etc - things that have no pob's to
     * represent them.
     *
     * @author Aravind Subramanian
     * @version %I%, %G%
     */
    private class ExtFormat {
    }

    /**
     * Class Constructor.
     * <p/>
     * ext should not include the ".". For example cls files
     * have the ext as "cls" and not ".cls"
     *
     * @param ext The extension.
     * @throws java.lang.IllegalArgumentException
     *          on invalid ext and bad input params
     */
    public DataFormat(Class repClass, String name, String desc, String ext, Icon icon, Class parserClass)
            throws IllegalArgumentException {

        super(repClass, ext);

        if (name == null) {
            throw new IllegalArgumentException("Param name cannot be null");
        }

        if (desc == null) {
            throw new IllegalArgumentException("Param desc cannot be null");
        }

        if (!SystemUtils.isHeadless() && icon == null) {
            throw new IllegalArgumentException("Param icon cannot be null");
        }

        if (ext == null) {
            throw new IllegalArgumentException("Null extension is not allowed: " + ext);
        }

        if (ext.startsWith(".")) {
            throw new IllegalArgumentException("DataFormat extension cannot begin with a period '.' " + ext);
        }

        if (!(ext.length() > 0)) {
            throw new IllegalArgumentException("DataFormat extension cannot have zero length "
                    + ext);
        }

        // parser can be null

        this.fExt = ext;
        this.fIcon = icon;
        this.fName = name;
        this.fDesc = desc;
        this.fParserClass = parserClass;
    }

    /**
     * @return Name of this DataFormat
     *         In "English"
     */
    public String getName() {
        return fName;
    }

    /**
     * @return A short description of this DataFormat
     */
    public String getDesc() {
        return fDesc;
    }

    // lazily made
    public FilenameFilter getFilenameFilter() {
        if (fFilt == null) {
            fFilt = createFnf(fExt);
        }

        return fFilt;
    }

    private static FilenameFilter createFnf(final String ext) {
        return new FilenameFilter() {
            public boolean accept(File file, String s) {
                return s.endsWith(ext);
            }
        };
    }

    /**
     * @return Extension for this DataFormat
     */
    public String getExtension() {
        return fExt;
    }

    /**
     * @return Icon standard for this DataFormat
     */
    public Icon getIcon() {
        return fIcon;
    }

    /**
     * @return
     * @note IMP IMP: overriding the base (java mime-type impl) to get a more user friendly output
     * -- used in combo boxes etc.
     * (dont leave spaces though as needs to be used in cmd line also)
     */
    public String toString() {
        return new StringBuilder(getName()).append("[").append(getExtension()).append("]").toString();
    }

    /**
     * @param obj
     * @return
     * @note IMP: Again, override base impl so that lookups based on the EXT value work
     */
    public boolean equals(Object obj) {

        if (obj == null) {
            return false;
        }

        if (obj instanceof DataFormat) {
            if (((DataFormat) obj).fExt.equals(this.fExt)) {
                return true;
            }
        } else {
            return fExt.equalsIgnoreCase(obj.toString());
        }

        return false;
    }

    // --------------------------- STATIC LOOKUP METHODS ---------------------------//
    /**
     * @param obj Typically a DataFormat or ext.
     *            Ok, to specify the toString value alos (for example: Foo[bar])
     *            This is used typically form a combo box.
     * @return
     */
    public static DataFormat getExtension(Object obj) {
        if (obj == null) {
            throw new IllegalArgumentException("Parameter obj cannot be null");
        }
    
        String ext;
        if (obj instanceof DataFormat) {
            ext = ((DataFormat) obj).getExtension();
        } else {
            ext = obj.toString();
        }
    
        for (int i = 0; i < ALL.length; i++) {
            if (ALL[i].getExtension().equalsIgnoreCase(ext)) {
                return ALL[i];
            }
    
            if (ALL[i].toString().equalsIgnoreCase(ext)) { // ok, to specify the toString value alos (for example: Foo[bar])
                return ALL[i];
            }
        }
    
        throw new IllegalArgumentException("No DataFormat found for: " + ext);
    }

    /**
     * @param cl NamingConventions extension for specified class. If no
     *           standard is known, returns null.
     * @return
     */
    public static String getExtension(final Class clz) {

        Class cl = _deconvClass(clz);

        Object obj = kClassExtMap.get(cl);

        if (obj != null) {
            return obj.toString();
        } else {
            klog.warn("No extension found for class: {}", cl);
            return "txt";
        }
    }

    /**
     * @param pob NamingConventions extension for specified PersistentObject.
     *            If no standard is known, returns null.
     * @return
     */
    public static String getExtension(PersistentObject pob) {

        if (pob == null) {
            return null;
        }

        return getExtension(pob.getClass());
    }

    /**
     * @param ext
     * @return
     */
    public static DataFormat getDataFormat(String ext) {

        Object obj = kExtDfMap.get(ext);

        if (obj != null) {
            return (DataFormat) obj;
        } else {
            return null;
        }
    }

    /**
     * @param ext
     * @return standard Icon for specified ext, if known.
     *         Else JarResources.ICON_NOT_FOUND.
     */
    public static Icon getIcon(final String ext) {

        //log.info("Getting getIcon for ext: " + ext);
        DataFormat df = getDataFormat(ext);

        if (df != null) {
            return df.getIcon();
        } else {
            return JarResources.ICON_UNKNOWN_DATA_FORMAT;
        }
    }

    public static Icon getIconOrNull(final String ext) {

        //log.info("Getting getIcon for ext: " + ext);
        DataFormat df = getDataFormat(ext);

        if (df != null) {
            return df.getIcon();
        } else {
            return null;
        }
    }

    public static String getDesc(File file) {

        //log.info("Getting getIcon for ext: " + ext);
        String ext = NamingConventions.getExtension(file).toLowerCase();
        DataFormat df = getDataFormat(ext);

        if (df != null) {
            return df.getDesc();
        } else {
            return null;
        }
    }

    private static Class _deconvClass(final Class pobClass) {

        if (kClassNameMap.containsKey(pobClass)) {
            return pobClass;
        }

        Class[] interfaces = pobClass.getInterfaces();
        for (int i = 0; i < interfaces.length; i++) {
            Object obj = kClassNameMap.get(interfaces[i]);
            if (obj != null) {
                return interfaces[i];
            }
        }

        // @todo fix me see nnm2genesetmatrix for why
        if (pobClass.equals(DefaultGeneSetMatrix.class)) {
            return GeneSetMatrix.class;
        }

        if (pobClass.equals(TemplateImpl.class)) {
            return Template.class;
        }

        return pobClass; // cant do anything
    }

    /**
     * @param obj
     * @return
     */
    public static Icon getIcon(final Object obj) {
        if (obj == null) {
            return JarResources.ICON_NOT_FOUND;
        } else if (obj instanceof File) {
            return getIcon(NamingConventions.getExtension((File) obj));
        } else if (obj instanceof String) {
            return getIcon(NamingConventions.getExtension(obj.toString()));
        } else {
            Object ic = kClassIconMap.get(obj.getClass());
            if (ic == null && obj instanceof PersistentObject) {
                Class repcl = getRepresentationClass((PersistentObject) obj);
                ic = kClassIconMap.get(repcl);
            }

            if (ic == null) {
                return JarResources.ICON_NOT_FOUND;
            } else {
                return (Icon) ic;
            }
        }
    }

    /**
     * The class that represent specified object
     * Central location where objects are linked with registered classes.
     * <p/>
     * why? other generic ways are tough to implement when dealing with interfaces, subclasses etc
     *
     * @param pob
     * @return
     * @maint add new objects and this will need updating
     */
    public static Class getRepresentationClass(final PersistentObject pob) {

        // TODO: Comb through this for otherwise unused classes.
        if (pob instanceof Template) {
            return Template.class;
        } else if (pob instanceof FeatureAnnot) {
            return FeatureAnnot.class;
        } else if (pob instanceof GeneSet) {
            return GeneSet.class;
        } else if (pob instanceof RankedList) { // careful about dataset!
            return RankedList.class;
        } else if (pob instanceof Dataframe) {
            return Dataframe.class;
        } else if (pob instanceof Matrix) {
            return Matrix.class;
        } else if (pob instanceof GeneSetMatrix) {
            return GeneSetMatrix.class;
        } else if (pob instanceof StringDataframe) {
            return StringDataframe.class;
        } else if (pob instanceof Report) {
            return Report.class;
        } else if (pob instanceof EnrichmentDb) {
            return EnrichmentDb.class;
        } else if (pob instanceof Chip) {
            return Chip.class;
        } else if (pob instanceof Dataset) { // has to be at end so that other ds impls get precedence
            return Dataset.class;
        } else if (pob instanceof SampleAnnot) {
            return SampleAnnot.class;
        } else {
            throw new IllegalArgumentException("Unknown object: " + pob);
        }
    }

    /**
     * @param file
     * @return
     */
    public static Icon getIcon(final File file) {
        String ext = NamingConventions.getExtension(file);
        return getIcon(ext);
    }

    public static Icon getIconOrNull(final File file) {
        String ext = NamingConventions.getExtension(file).toLowerCase();
        return getIconOrNull(ext);
    }


    public static boolean isCompatibleRepresentationClass(final Object obj, final Class[] classes) {
        for (int c = 0; c < classes.length; c++) {
            if (isCompatibleRepresentationClass(obj, classes[c])) {
                return true;
            }
        }

        return false;
    }

    // @todo this is buggy (see saving reports for error)
    public static boolean isCompatibleRepresentationClass(Object obj, Class cl) {

        if (obj == null) {
            throw new IllegalArgumentException("Parameter obj cannot be null");
        }

        if (cl == null) {
            throw new IllegalArgumentException("Parameter cl cannot be null");
        }

        // first check directly
        if (obj.getClass().getName().equals(cl.getName())) {
            return true;
        }

        // then check interfaces in obj
        Class[] interfacesA = obj.getClass().getInterfaces();
        for (int i = 0; i < interfacesA.length; i++) {
            if (cl.getName().equals(interfacesA[i].getName())) {
                return true;
            }
        }

        // then check interfaces in cl
        Class[] interfacesB = cl.getInterfaces();
        for (int i = 0; i < interfacesB.length; i++) {
            if (obj.getClass().getName().equals(interfacesB[i].getName())) {
                return true;
            }
        }

        // then check interfaces vs interfaces
        for (int a = 0; a < interfacesA.length; a++) {
            for (int b = 0; b < interfacesB.length; b++) {
                if (interfacesA[a].getName().equals(interfacesB[b].getName())) {
                    return true;
                }
            }
        }

        // @todo fix me see GeneSetMatrixReqdParam uses for why
        if (obj instanceof GeneSetMatrix) {
            return true; // disable the check
        }

        if (obj instanceof Dataset) {
            return true; // disable check
        }

        // ok, thats it
        return false;
    }
}
