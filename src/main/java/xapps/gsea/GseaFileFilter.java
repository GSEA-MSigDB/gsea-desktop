/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xapps.gsea;

import edu.mit.broad.genome.NamingConventions;
import gnu.trove.THashSet;

import java.io.File;

/**
 * @author Aravind Subramanian
 */
public class GseaFileFilter extends javax.swing.filechooser.FileFilter {


    private THashSet fCustomExts;
    private String fDesc;

    /**
     * Class constructor
     *
     * @param customExts
     */
    public GseaFileFilter(String[] customExts, String desc) {
        this.fCustomExts = new THashSet();
        for (int i = 0; i < customExts.length; i++) {
            this.fCustomExts.add(customExts[i]);
            this.fCustomExts.add(customExts[i].toLowerCase());
            this.fCustomExts.add(customExts[i].toUpperCase());
        }

        this.fDesc = desc;
    }

    public boolean accept(File f) {
        if (f.isDirectory()) {
            return true;
        }

        String ext = NamingConventions.getExtension(f);
        return fCustomExts.contains(ext);
    }

    public String getDescription() {
        return fDesc;
    }

}    // End class GseaFileFilter
