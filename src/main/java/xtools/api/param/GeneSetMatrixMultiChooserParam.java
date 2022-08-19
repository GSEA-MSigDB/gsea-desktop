/*
 * Copyright (c) 2003-2022 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xtools.api.param;

import edu.mit.broad.genome.objects.*;
import edu.mit.broad.genome.parsers.AuxUtils;
import edu.mit.broad.genome.parsers.ParseUtils;
import edu.mit.broad.genome.parsers.ParserFactory;
import edu.mit.broad.genome.swing.fields.GFieldPlusChooser;
import edu.mit.broad.vdb.chip.Chip;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

import xapps.gsea.GseaWebResources;
import xtools.api.ui.GeneSetMatrixChooserUI;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.*;
import java.util.List;

/**
 * @author Aravind Subramanian, David Eby
 */
public class GeneSetMatrixMultiChooserParam extends AbstractParam {
    private MyPobActionListener fAl;
    private GeneSetMatrixChooserUI fChooser;

    // Delimiter to use in separating gene sets when represented as Strings.
    private String delimiter = ",";

    public GeneSetMatrixMultiChooserParam(boolean reqd) {
        this(GMX, GMX_ENGLISH, GMX_DESC_MULTI, reqd);
    }

    public GeneSetMatrixMultiChooserParam(final String name, final String nameEnglish, final String desc, final boolean reqd) {
        super(name, nameEnglish, GeneSetMatrix[].class, desc, new GeneSetMatrix[]{}, new GeneSetMatrix[]{}, reqd);
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

    private static final String GSEA_ALTERED_FTP_PATH = GseaWebResources.GSEA_FTP_SERVER + ":" + GseaWebResources.GSEA_FTP_SERVER_BASE_DIR;
    private static final String GSEA_BASE_FTP_PATH = GseaWebResources.GSEA_FTP_SERVER + ":/" + GseaWebResources.GSEA_FTP_SERVER_BASE_DIR;
    
    private Object[] _getObjects() throws Exception {
        Object val = getValue();
        Object[] objs;

        // cant get a ftp file object because it has to be a string in the object chooser text area
        // log.debug("value = " + val + " class: " + val.getClass());

        if (val instanceof String) {
            String[] paths = _parse(val.toString());
            objs = new Object[paths.length];
            for (int p = 0; p < paths.length; p++) {
                String path = paths[p];
                if (path.toLowerCase().startsWith("ftp.") || path.toLowerCase().startsWith("gseaftp.")) {
                    if (AuxUtils.isAux(path)) {
                        // We're looking for just one gene set out of an FTP-based file.
                        // Hack the path if necessary.  The FTP paths used in caching the individual gene sets may get munged 
                        // so we put them back as expected, otherwise there will be a cache miss and we'll re-fetch the file
                        // unnecessarily.  This is specific to Broad FTP paths.
                        // TODO: clean up the caching behind this.
                        // This path munging happens because the keys are stored as *Files*, which causes issues when converting
                        // back-and-forth to Strings (as needed for URLs).  Part of that is implicit canonicalization and part is
                        // platform-specific (i.e. Windows) conversion.  It's not simple though since the Files are used elsewhere.
                        if (StringUtils.containsIgnoreCase(path, GseaWebResources.GSEA_FTP_SERVER)) {
                            // Special case: correct Windows path separators
                            if (SystemUtils.IS_OS_WINDOWS) { path = StringUtils.replace(path, "\\", "/"); }
                            path = StringUtils.replace(path, GSEA_ALTERED_FTP_PATH, GSEA_BASE_FTP_PATH);
                        }
                        
                        GeneSetMatrix gm = ParserFactory.readGeneSetMatrix(path, true);
                        GeneSet geneSet = gm.getGeneSet(AuxUtils.getAuxNameOnlyIncludingHash(path));
                        objs[p] = geneSet;
                    } else {
                        objs[p] = ParserFactory.readGeneSetMatrix(path, true);
                    }
                } else if (AuxUtils.isAux(path)) {
                    objs[p] = ParserFactory.readGeneSet(new File(path), true);
                } else {
                    objs[p] = ParserFactory.read(new File(path));
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

    private GeneSetsStruc _getGeneSets() throws Exception {
        Object[] objs = _getObjects();

        if (isReqd() && objs.length == 0) {
            throw new IllegalArgumentException("Must specify GeneSetMatrix parameter: " + getNameEnglish() + " (" + getDesc() + ")");
        }

        List<GeneSet> gsets = new ArrayList<GeneSet>();

        String name = _getName(objs);
        for (int i = 0; i < objs.length; i++) {
            if (objs[i] instanceof GeneSetMatrix) {
                // TODO: fix type safety of GeneSetMatrix.getGeneSetsL().  Should return List<GeneSet>
                gsets.addAll(((GeneSetMatrix) objs[i]).getGeneSetsL());
            } else if (objs[i] instanceof GeneSet) {
                gsets.add((GeneSet)objs[i]);
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
        if (s == null) { throw new IllegalArgumentException("Parameter s cannot be null"); }

        Set<String> vals = ParseUtils.string2stringsSet(s, delimiter);

        System.out.println("to parse>" + s + "< got: " + vals);

        Set<String> use = new HashSet<String>();
        for (Iterator<String> it = vals.iterator(); it.hasNext();) {
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

            log.debug("{}", vals[i].getClass());

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

    private ActionListener getActionListener() {
        if (fAl == null) {
            this.fAl = new MyPobActionListener();
            fAl.setChooser(fChooser);
        }

        return fAl;
    }

    public boolean isFileBased() { return true; }

    private static class MyPobActionListener implements ActionListener {
        private GeneSetMatrixChooserUI fChooser;

        public MyPobActionListener() {}

        // cant have this in the class constructor as the action list needs to
        // be instantiated before the chooser object is made
        public void setChooser(GeneSetMatrixChooserUI chooser) {
            this.fChooser = chooser;
        }

        public void actionPerformed(ActionEvent e) {
            if (fChooser == null) { return; }

            final String[] selectedPaths = fChooser.getJListWindow().showDirectlyWithModels();
            if ((selectedPaths != null) && (selectedPaths.length >  0)) {
                fChooser.setText(String.join(",", selectedPaths));
            }
        }
    }

    // have to make the strs into paths
    public String getValueStringRepresentation(final boolean full) {
        Object val = getValue();

        if (val == null) { return null; }

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
            // do in 2 stages, as the al needs a valid (non-null) chooser at its construction
            fChooser = new GeneSetMatrixChooserUI();
            fChooser.setCustomActionListener(getActionListener());
            String text = this.getValueStringRepresentation(false);
            if (text == null) {
                text = format((Object[]) getDefault());
            }

            fChooser.setText(text);
            ParamHelper.addDocumentListener(fChooser.getTextField(), this);
        }

        return fChooser;
    }
}
