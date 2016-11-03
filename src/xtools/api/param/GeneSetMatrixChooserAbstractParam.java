/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xtools.api.param;

import edu.mit.broad.genome.charts.XChart;
import edu.mit.broad.genome.io.FtpResultInputStream;
import edu.mit.broad.genome.io.FtpSingleUrlTransferCommand;
import edu.mit.broad.genome.objects.*;
import edu.mit.broad.genome.parsers.AuxUtils;
import edu.mit.broad.genome.parsers.DataFormat;
import edu.mit.broad.genome.parsers.ParseUtils;
import edu.mit.broad.genome.parsers.ParserFactory;
import edu.mit.broad.genome.swing.fields.GFieldPlusChooser;
import edu.mit.broad.vdb.chip.Chip;
import edu.mit.broad.xbench.RendererFactory2;
import edu.mit.broad.xbench.core.ObjectBindery;
import edu.mit.broad.xbench.prefs.XPreferencesFactory;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.genepattern.uiutil.FTPFile;
import org.genepattern.uiutil.FTPList;

import xapps.gsea.GseaWebResources;
import xtools.api.ui.GeneSetMatrixChooserUI;
import xtools.api.ui.NamedModel;

import javax.swing.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.*;
import java.util.List;

/**
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
abstract class GeneSetMatrixChooserAbstractParam extends AbstractParam {

    private boolean fMultipleAllowed = true;

    private MyPobActionListener fAl;

    protected GeneSetMatrixChooserUI fChooser;

    private boolean fIncludeGmx = true;

    // Delimiter to use in separating gene sets when represented as Strings.
    private String delimiter = ",";

    /**
     * Class constructor
     *
     * @param name
     * @param nameEnglish
     * @param desc
     * @param reqd
     * @param multipleAlllowed
     * @param includeGeneSetMatrix
     */
    GeneSetMatrixChooserAbstractParam(final String name,
                                      final String nameEnglish,
                                      final String desc,
                                      final boolean reqd,
                                      final boolean multipleAlllowed,
                                      final boolean includeGeneSetMatrix) {
        super(name, nameEnglish, GeneSetMatrix[].class, desc, new GeneSetMatrix[]{}, new GeneSetMatrix[]{}, reqd);
        this.fMultipleAllowed = multipleAlllowed;
        this.fIncludeGmx = includeGeneSetMatrix;
    }

    public GeneSetMatrix getGeneSetMatrixCombo(final boolean removeNativeGmNames) throws Exception {
        return _getGeneSets().toGm(removeNativeGmNames);
    }
    
    /**
     * Set an alternate delimiter to <em>replace</em> the default comma separator.  This must be a 
     * single character.  A null or empty value will revert to use of the comma.
     * 
     * This <em>must</em> be set prior to use of getValue() or getStrings() in order for the 
     * parameter to be properly parsed using the alternativeDelimiter.
     * 
     * @param alternateDelimiter
     */
    public void setAlternateDelimiter(String alternateDelimiter) {
        if (StringUtils.length(alternateDelimiter) > 1) {
            throw new IllegalArgumentException("Illegal alternate delimiter '"
                    + alternateDelimiter + "'; must be a single character only.");
        }
        this.delimiter = alternateDelimiter;
    }

    //-----------------------------------------------------------------------//
    //------------------------- CORE METHODS --------------------------------//
    //-----------------------------------------------------------------------//
    protected Object[] _getObjects() throws Exception {

        Object val = getValue();

        Object[] objs;

        // cant et a ftp file object because it has to be a string n the obect chooser text area
        // log.debug("value = " + val + " class: " + val.getClass());

        if (val instanceof String) {
            String[] paths = _parse(val.toString());
            objs = new Object[paths.length];
            for (int p = 0; p < paths.length; p++) {
                if (paths[p].toLowerCase().startsWith("ftp.") || paths[p].toLowerCase().startsWith("gseaftp.")) {                    
                    java.net.URL ftpURL = new java.net.URL("ftp://" + paths[p]);
                    FtpSingleUrlTransferCommand ftpCommand = new FtpSingleUrlTransferCommand(ftpURL);
                    FtpResultInputStream resultInputStream = null;
                    try {
                        resultInputStream = ftpCommand.retrieveAsInputStream();
                        GeneSetMatrix gm = ParserFactory.readGeneSetMatrix(paths[p], resultInputStream, false);
                        if (AuxUtils.isAux(paths[p])) {
                            objs[p] = gm.getGeneSet(AuxUtils.getAuxNameOnlyIncludingHash(paths[p]));
                        } else {
                            objs[p] = gm;
                        }
                    }
                    finally {
                        // If the inputStream came from an FTP download, clean up the underlying 
                        // temp file on disk after processing is complete.
                        if (resultInputStream != null) {
                            File tempFile = resultInputStream.getFile();
                            // Try to delete the file.  If that fails, set it to be deleted when the JVM exits.
                            if (!tempFile.delete()) tempFile.deleteOnExit();
                        }
                    }
                } else if (AuxUtils.isAux(paths[p])) {
                    objs[p] = ParserFactory.readGeneSet(new File(paths[p]), true);
                } else {
                    objs[p] = ParserFactory.read(new File(paths[p]));
                }
            }
        } else if (val instanceof Object[]) {
            objs = (Object[]) val;
        } else {
            objs = new Object[]{val};
        }

        return objs;
    }

    /**
     * Internal; sttic to preserve the name of the input gene matrix
     * in cases where there is only 1 specified (95% of the time)
     */
    class GeneSetsStruc {
        GeneSet[] gsets;
        String name;

        GeneSetsStruc(final String name, final GeneSet[] gsets) {
            this.gsets = gsets;
            this.name = name;
        }

        GeneSetMatrix toGm(boolean removeNativeGmNames) {
            return new DefaultGeneSetMatrix(name, gsets, removeNativeGmNames);
        }
    }

    private String _getName(final Object[] objs) {

        String name = "combo";

        if (objs.length == 1 && objs[0] instanceof GeneSetMatrix) {
            name = ((GeneSetMatrix) objs[0]).getName();
        }

        return name;

    }

    protected GeneSetsStruc _getGeneSets() throws Exception {

        Object[] objs = _getObjects();

        //log.debug("num of selections: " + objs.length);

        if (isReqd() && objs.length == 0) {
            throw new IllegalArgumentException("Must specify GeneSetMatrix parameter: " + getNameEnglish() + " (" + getDesc() + ")");
        }

        List gsets = new ArrayList();

        String name = _getName(objs);
        for (int i = 0; i < objs.length; i++) {
            if (objs[i] instanceof GeneSetMatrix) {
                gsets.addAll(((GeneSetMatrix) objs[i]).getGeneSetsL());
            } else if (objs[i] instanceof GeneSet) {
                gsets.add(objs[i]);
            } else if (objs[i] instanceof Dataset) {
                gsets.add(((Dataset) objs[i]).getRowNamesGeneSet());
            } else if (objs[i] instanceof Chip) {
                gsets.add(((Chip) objs[i]).getProbeNamesAsGeneSet());
            } else {
                throw new IllegalArgumentException("Unknown object: " + objs[i]);
            }
        }

        return new GeneSetsStruc(name, (GeneSet[]) gsets.toArray(new GeneSet[gsets.size()]));
    }

    private String[] _parse(final String s) {

        if (s == null) {
            throw new IllegalArgumentException("Parameter s cannot be null");
        }

        Set vals = ParseUtils.string2stringsSet(s, delimiter, false);

        System.out.println("to parse>" + s + "< got: " + vals);

        Set use = new HashSet();
        for (Iterator it = vals.iterator(); it.hasNext();) {
            String key = it.next().toString();
            if (key.length() > 0) {
                use.add(key);
            }
        }

        return (String[]) use.toArray(new String[use.size()]);
    }

    // override base class method to do for both pobs and strings
    private String format(final Object[] vals) {
        if (vals == null) {
            return "";
        }

        StringBuffer buf = new StringBuffer();

        for (int i = 0; i < vals.length; i++) {

            if (vals[i] == null) {
                continue;
            }

            log.debug(vals[i].getClass());

            if (vals[i] instanceof PersistentObject) {
                String p = ParserFactory.getCache().getSourcePath(vals[i]);
                buf.append(p);
            } else {
                buf.append(vals[i].toString().trim());
            }

            if (i != vals.length - 1) {
                buf.append(delimiter);
            }
        }

        return buf.toString();
    }

    // override base impl
    protected ActionListener getActionListener() {
        if (fAl == null) {
            this.fAl = new MyPobActionListener(fMultipleAllowed, fIncludeGmx);
            fAl.setChooser(fChooser);
        }

        return fAl;
    }

    public boolean isFileBased() {
        return true;
    }

    // redo from the abstract super class here as we dont want to swap out the model
    // (model is the datasets etc)
    // REIMPLEMENT HERE as we want to (possibly) use both genesets and genesetmatrixes
    private static class MyPobActionListener implements ActionListener {

        private boolean fMultipleAllowed;
        private boolean fIncludeGmx;
        private GeneSetMatrixChooserUI fChooser;

        public MyPobActionListener(boolean multipleAllowed, boolean includeGeneSetMatrix) {
            this.fMultipleAllowed = multipleAllowed;
            this.fIncludeGmx = includeGeneSetMatrix;
        }

        // cant have this in the class constructor as the action list needs to
        // be instantiated before the chooser object is made
        public void setChooser(GeneSetMatrixChooserUI chooser) {
            this.fChooser = chooser;
        }

        final DefaultListCellRenderer rend = new GeneSetsFromFTPSiteRenderer();

        private ListModel createFTPModel() {

            if (XPreferencesFactory.kOnlineMode.getBoolean() == false) {
                DefaultListModel model = new DefaultListModel();
                model.addElement("Offline mode");
                model.addElement("Change this in Menu=>Preferences");
                model.addElement("Choose gene sets from other tabs");
                return model;
            } else {

                try {

                    FTPList ftpList;
                    ftpList = new FTPList(GseaWebResources.getGseaFTPServer(),
                                          GseaWebResources.getGseaFTPServerUserName(),
                                          GseaWebResources.getGseaFTPServerPassword(),
                                          GseaWebResources.getGseaFTPServerGeneSetsDir(),
                                          new GeneSetsFromFTPSiteComparator());
                    ftpList.quit();
                    return ftpList.getModel();
                } catch (Exception e) {
                    klog.error(e);
                    DefaultListModel model = new DefaultListModel();
                    model.addElement("Error listing Broad website");
                    model.addElement(e.getMessage());
                    model.addElement("Choose gene sets from other tabs");
                    return model;
                }
            }
        }


        public void actionPerformed(ActionEvent e) {

            if (fChooser == null) {
                //klog.debug("Chooser not yet inited: " + fChooser);
                return;
            }

            int selmode;
            if (fMultipleAllowed == false) {
                selmode = ListSelectionModel.SINGLE_SELECTION;
            } else {
                selmode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION;
            }

            NamedModel[] models;
            final NamedModel gsetsModel = new NamedModel("Gene sets (grp)", ObjectBindery.getModel(GeneSet.class));
            final NamedModel subsetsModel = new NamedModel("Subsets", ObjectBindery.getHackAuxGeneSetsBoxModel());
            final NamedModel gmFromFTPModel = new NamedModel("Gene matrix (from website)", createFTPModel());


            if (fIncludeGmx) {
                models = new NamedModel[]
                        {
                                gmFromFTPModel,
                                gsetsModel,
                                new NamedModel("Gene matrix (local gmx/gmt)", ObjectBindery.getModel(GeneSetMatrix.class)),
                                subsetsModel
                        };
            } else {
                models = new NamedModel[]
                        {
                                gsetsModel,
                                subsetsModel
                        };
            }

            final Object[] sels = fChooser.getJListWindow().showDirectlyWithModels(models, selmode, rend);

            if ((sels == null) || (sels.length == 0)) { // <-- @note
            } else {
                String[] paths = new String[sels.length];
                for (int i = 0; i < sels.length; i++) {
                    if (sels[i] instanceof FTPFile) {
                        paths[i] = ((FTPFile) sels[i]).getPath();
                    } else {
                        paths[i] = ParserFactory.getCache().getSourcePath(sels[i]);
                    }
                }

                String str = ChooserHelper.formatPob(sels);
                fChooser.setText(str);
            }
        }
    }

    // have to make the strs into paths
    public String getValueStringRepresentation(final boolean full) {

        Object val = getValue();

        if (val == null) {
            return null;
        }

        // log.debug("value: " + val.getClass() + " " + val);

        if (val instanceof String) {
            return (String) val;
        } else if (val instanceof Object[]) {
            Object[] objs = (Object[]) val;
            return format(objs);
        } else {
            return format(new Object[]{val});
        }

    }


    public GFieldPlusChooser getSelectionComponent() {

        if (fChooser == null) {
            //fChooser = new GOptionsFieldPlusChooser(getActionListener(), Application.getWindowManager().getRootFrame());
            // do in 2 stages, as the al needs a valid (non-null) chooser at its construction
            fChooser = new GeneSetMatrixChooserUI(false);
            fChooser.setCustomActionListener(getActionListener());
            String text = this.getValueStringRepresentation(false);
            if (text == null) {
                text = format((Object[]) getDefault());
            }

            if (isFileBased()) { // as otherwise lots of exceptions thrown if user edits a bad file
                // @todo but probelm is that no way to cancel and "null out" a choice once made
                //fChooser.getTextField().setEditable(false);
            }

            fChooser.setText(text);
            ParamHelper.addDocumentListener(fChooser.getTextField(), this);
        }

        return fChooser;
    }


    public static class GeneSetsFromFTPSiteComparator implements Comparator {

        /* private static member used to track highest seen version */
        private static String highestVersionId;
        private static DefaultArtifactVersion highestVersion;

        public static String getHighestVersionId() {
            return highestVersionId;
        }

        /**
         * Compare method that yields the following ordering of gmt file names:
         *       versions will be listed in descending order
         *       within a given version, collection types (c1, c2, c3, c4, c5)
         *         will be listed in increasing order
         *       within a given version and collection type, subset types
         *         (e.g., all vs. cp) will be listed in lexicographic order
         */
        public int compare(Object pn1, Object pn2) {

            MSigDBFilenameParser p1 = new MSigDBFilenameParser(pn1.toString());
            MSigDBFilenameParser p2 = new MSigDBFilenameParser(pn2.toString());

            DefaultArtifactVersion version1 = new DefaultArtifactVersion(p1.getCanonicalVersionId());
            DefaultArtifactVersion version2 = new DefaultArtifactVersion(p2.getCanonicalVersionId());

            /* want to keep track of highest version seen */
            if (highestVersion == null) {
                if (version1.compareTo(version2) < 0) {
                    highestVersion = version2;
                    highestVersionId = p2.getVersionId();
                }
                else {
                    highestVersion = version1;
                    highestVersionId = p2.getVersionId();
                }
            }
            else {
                if (highestVersion.compareTo(version2) < 0) {
                    highestVersion = version2;
                    highestVersionId = p2.getVersionId();
                }
            }

            if (!version1.equals(version2)) {
                return version2.compareTo(version1);
            }
            else {
                int collectionNum1 = p1.getCollectionIdNum();
                int collectionNum2 = p2.getCollectionIdNum();
                if (collectionNum1 != collectionNum2) {
                    return collectionNum1 - collectionNum2;
                }
                else {
                    String subsetId1 = p1.getSubsetId();
                    String subsetId2 = p2.getSubsetId();
                    return subsetId1.compareTo(subsetId2);
                }
            }
        }

        public boolean equals(Object o2) {
            return false;
        }
    }    // End GeneSetsFromFTPSiteComparator

    /**
     * static nested class for parsing names of gmt files retrievable from
     * MSigDB
     */
    public static class MSigDBFilenameParser {

        static String MSIGDB_VERSION_V1 = "v1";
        static String MSIGDB_VERSION_V2 = "v2";

        private String filename = null;
        private String versionId = null;
        private String collectionId = null;
        private String subsetId = null;
        private int collectionIdNum = 0;

        public MSigDBFilenameParser(String filename) {
            if (filename == null)
                throw new IllegalArgumentException("MSigDB Filename cannot be null");
            this.filename = filename;

            collectionId = filename.substring(0,2);
            if (collectionId.substring(0,1).equals("h")) {
                collectionId = collectionId.substring(0,1);
                collectionIdNum = 0;
            }
            else {
                collectionIdNum = Integer.parseInt(collectionId.substring(1));
            }
            versionId = filename.substring(filename.lastIndexOf("v"), filename.lastIndexOf(".symbols.gmt"));

            if (!versionId.equals(MSIGDB_VERSION_V1) && !versionId.equals(MSIGDB_VERSION_V2)) {
                subsetId = filename.substring(3, filename.lastIndexOf(versionId)-1);
            }
        }

        public String getVersionId() {
            return versionId;
        }

        public String getCanonicalVersionId() {
            if (versionId.equals(MSIGDB_VERSION_V1) || versionId.equals(MSIGDB_VERSION_V2)) {
                return versionId + ".0";
            }
            else
                return versionId;
        }

        public int getCollectionIdNum() {
            return collectionIdNum;
        }

        public String getSubsetId() {
            return subsetId;
        }

    }

    private static boolean isVersion(String versionSuffix, String name) {
        int index = StringUtils.indexOfIgnoreCase(name, versionSuffix);
        if (index < 0) {
            return false;
        } else {
            return true;
        }
    }

    public static class GeneSetsFromFTPSiteRenderer extends DefaultListCellRenderer {
        private boolean ifFileOnlyShowName;

        public GeneSetsFromFTPSiteRenderer(final boolean ifFileOnlyShowName) {
            this.ifFileOnlyShowName = ifFileOnlyShowName;
        }

        public GeneSetsFromFTPSiteRenderer() {
            this(false); // default is to show the full path
        }

        public Component getListCellRendererComponent(JList list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {

            // doesnt work properly unless called
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            // order is important
            if (value instanceof PersistentObject) {
                PersistentObject pob = (PersistentObject) value;

                if (pob.getQuickInfo() != null) {
                    StringBuffer buf = new StringBuffer("<html><body>").append(pob.getName());
                    buf.append("<font color=#666666> [").append(pob.getQuickInfo()).append(']').append("</font></html></body>");
                    this.setText(buf.toString());
                } else {
                    this.setText(pob.getName());
                }

                File f = null;

                if (ParserFactory.getCache().isCached(pob)) {
                    f = ParserFactory.getCache().getSourceFile(pob);
                }

                if (f != null) {
                    this.setToolTipText(f.getAbsolutePath());
                } else {
                    this.setToolTipText("Unknown origins of file: " + f);
                }
            } else if (value instanceof File) {
                if (ifFileOnlyShowName) {
                    this.setText(((File) value).getName());
                } else {
                    this.setText(((File) value).getAbsolutePath());
                }
                this.setIcon(DataFormat.getIcon(value));
                this.setToolTipText(((File) value).getAbsolutePath());
            } else if (value instanceof XChart) {
                this.setText(((XChart) value).getName());
                this.setIcon(XChart.ICON);
            } else if (value instanceof FTPFile) {

                String s = ((FTPFile) value).getName();
                String slc = s.toLowerCase();
                if (slc.indexOf("c1.") != -1) {
                    s = s + " [Positional]";
                } else if (slc.indexOf("c2.") != -1) {
                    s = s + " [Curated]";
                } else if (slc.indexOf("c3.") != -1) {
                    s = s + " [Motif]";
                } else if (slc.indexOf("c4.") != -1) {
                    s = s + " [Computational]";
                } else if (slc.indexOf("c5.") != -1) {
                    s = s + " [Gene ontology]";
                } else if (slc.indexOf("c6.") != -1) {
                    s = s + " [Oncogenic signatures]";
                } else if (slc.indexOf("c7.") != -1) {
                    s = s + " [Immunologic signatures]";
                } else if (slc.indexOf("h.") != -1) {
                    s = s + " [Hallmarks]";                 
                }

                if (isVersion( GeneSetsFromFTPSiteComparator.getHighestVersionId(), slc)) {
                    Font font = this.getFont();
                    String fontName = font.getFontName();
                    int fontSize = font.getSize();
                    this.setFont(new Font(fontName, Font.BOLD, fontSize));
                }

                this.setText(s);
                this.setIcon(RendererFactory2.FTP_FILE_ICON);
            }

            return this;
        }
    }    // End CommonLookListRenderer

} // End class AbstractGeneSetMatrixChooserParam
