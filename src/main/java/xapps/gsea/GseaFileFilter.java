/*
 * Copyright (c) 2003-2020 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xapps.gsea;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.swing.filechooser.FileFilter;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Aravind Subramanian
 */
// Note: converting from JFileChoosers to FileDialogs, so this implements what we need for both
// as a bridge during the switch.  We'll keep just the latter when we're finished.
public class GseaFileFilter extends FileFilter implements FilenameFilter {


    private Set<String> fCustomExts;
    private String fDesc;

    public GseaFileFilter(String[] customExts, String desc) {
        this.fCustomExts = new HashSet<String>(Arrays.asList(customExts));
        this.fDesc = desc;
    }

    public boolean accept(File f) {
        if (f.isDirectory()) {
            return true;
        }

        return fCustomExts.contains(StringUtils.lowerCase(FilenameUtils.getExtension(f.getName())));
    }
    
    @Override
    public boolean accept(File dir, String name) {
        return dir != null && fCustomExts.contains(StringUtils.lowerCase(FilenameUtils.getExtension(name)));
    }
    
    public String getDescription() {
        return fDesc;
    }
}