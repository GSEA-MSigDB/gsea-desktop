/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package org.genepattern.modules;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class AbstractModule {

    protected static final Pattern COMMA_PATTERN = Pattern.compile(",");
    protected static final Pattern HASH_PATTERN = Pattern.compile("#");

    protected static String copyFileWithoutBadChars(String file, File working) throws IOException {
        if (!StringUtils.containsAny(file, '#', '@')) {
            // Do nothing; the file does not need to be renamed
            return file;
        }

        // Work only with the file name itself. This naturally discards any offending characters in the full path
        // For any others we'll just substitute underscores
        File f = new File(file);
        String newName = StringUtils.replaceChars(f.getName(), "@#", "_");

        File newFile = new File(working, newName);
        System.out.println("Copying file '" + file + "' to '" + newName + "'");
        try {
            FileUtils.copyFile(f, newFile);
            return newFile.getPath();
        } catch (IOException ioe) {
            System.err.println("An error occurred trying to copy '" + file + "' to '" + newName + "'");
            throw ioe;
        }
    }

    protected static void setParam(String name, String value, Properties paramProps, Logger log) {
        log.info(name + "\t" + value);
        paramProps.setProperty(name, value);
    }

    protected static void setOptionValueAsParam(String optionName, CommandLine commandLine, Properties paramProps, Logger log) {
        String optionValue = commandLine.getOptionValue(optionName);
        if (optionValue == null) return;
        setParam(optionName, optionValue, paramProps, log);
    }

    protected static void copyAnalysisToCurrentDir(final File cwd, final File analysis, boolean createZip, final String zipFileName)
            throws Exception {
        try {
            if (createZip) {
                copyZipToJobIfPresent(analysis, zipFileName, cwd);
            }
        } finally {
            try {
                File edb = new File(analysis, "edb");
                if (edb.exists()) {
                    FileUtils.copyDirectory(edb, cwd, FileFileFilter.FILE);
                }
            } finally {
                FileUtils.copyDirectory(analysis, cwd, FileFileFilter.FILE);
            }
        }
    }

    protected static void cleanUpAnalysisDirs(final File cwd, final File tmp_working) {
        try {
            FileUtils.deleteQuietly(tmp_working);
        } finally {
            // delete empty directories; GSEA creates one named after the current date.
            deleteEmptyDirectories(cwd);
        }
    }

    protected static void copyZipToJobIfPresent(final File analysis, String zipFileName, File cwd) throws Exception {
        if (!analysis.exists()) return;

        Collection<File> zips = FileUtils.listFiles(analysis, new String[] { "zip" }, false);
        if (zips == null || zips.isEmpty()) return;

        // Check for multiple ZIPs. This should never happen.
        if (zips.size() > 1) {
            throw new Exception("Internal Error: multiple ZIP files created");
        }
        File zip = zips.iterator().next();

        try {
            File dest = new File(cwd, zipFileName);
            System.out.println("Moving from :" + zip.getAbsolutePath());
            System.out.println("To :" + dest.getAbsolutePath());
            FileUtils.moveFile(zip, dest);
        } catch (IOException ioe) {
            System.err.println("Internal error moving result ZIP: ");
            throw ioe;
        }
    }

    protected static void deleteEmptyDirectories(File dir) {
        // Shouldn't be needed because of System.setProperty("mkdir", "false"), but it seems to be required
        File[] files = dir.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    String[] filesInDir = files[i].list();
                    if (filesInDir == null || filesInDir.length == 0) {
                        files[i].delete();
                    }
                }
            }
        }
    }

    protected static final List<String> selectGeneSetsFromFiles(List<String> geneSetDBs, List<String> selectedGeneSets, boolean gpMode) {
        // If there are no geneSet DBs at all just return an empty list. This is covering any possible earlier error conditions
        // that caused these lists to be wiped out (e.g. error in downloading MSigDB files). Those errors should have already
        // been flagged so we don't want to repeat ourselves here, nor do we want to cause spurious cascading errors.
        if (geneSetDBs.isEmpty()) return Collections.emptyList();

        try {
            Map<String, String> geneSetDBPathMap = buildFileNameToFilePathMap(geneSetDBs);

            // Do we have only a single Gene Set file? If so, the user is allowed to skip the fileName#geneSetName notation
            // in favor of simply using geneSetName. This is optional and both are supported.
            boolean haveSingleFile = (geneSetDBPathMap.size() == 1);

            // If there's only one file, pull it out of the Map values() and use it directly. Otherwise, build the selector using the Map.
            List<String> selected = new ArrayList<String>();
            for (String selectedGeneSet : selectedGeneSets) {
                String selector = haveSingleFile ? buildSelector(selectedGeneSet, geneSetDBPathMap.values().iterator().next())
                        : buildSelector(selectedGeneSet, geneSetDBPathMap);
                selected.add(selector);
            }

            return selected;
        } catch (IllegalArgumentException iae) {
            String paramName = (gpMode) ? "selected.gene.sets" : "-selected_gene_sets";
            System.err.println("There was a problem processing the '" + paramName + "' parameter");
            iae.printStackTrace(System.err);
            return null;
        }
    }

    protected static final String buildSelector(String selectionToken, String filePath) {
        String fileName = FilenameUtils.getName(filePath);

        // Note that we *could* also validate that the geneSetName is present, but instead we'll leave that job to
        // the GSEA code. It's too complex to do here and a duplication of effort anyway.
        String[] specifier = HASH_PATTERN.split(selectionToken);
        if (specifier == null || (specifier.length != 1 && specifier.length != 2)) {
            throw new IllegalArgumentException("Gene Set selection specifier '" + selectionToken
                    + "' is not valid. Selections must be separated with semicolon, and each must be a "
                    + "file name + '#' + gene set name, e.g. my_file1.gmt#selected_gene_set1. The selector "
                    + "can be shortened to selected_gene_set1 when there is only one Gene Set file.");
        }
        if (specifier.length == 2 && !StringUtils.equals(fileName, specifier[0])) {
            throw new IllegalArgumentException("Gene Set selection specifier '" + selectionToken
                    + "' is not valid; Specified file name must match lone file '" + fileName + "' supplied as the Gene Set database.");
        }

        // geneSetName is always the last item in the specifier
        String geneSetName = specifier[specifier.length - 1];
        return filePath + '#' + geneSetName;
    }

    protected static final String buildSelector(String selectionToken, Map<String, String> geneSetDBPathMap) {
        String[] specifier = HASH_PATTERN.split(selectionToken);
        if (specifier == null || specifier.length != 2) {
            throw new IllegalArgumentException("Gene Set selection specifier '" + selectionToken
                    + "' is not valid. Selections must be separated with semicolon, and each must be a "
                    + "file name + '#' + gene set name, e.g. my_file1.gmt#selected_gene_set1. The selector "
                    + "can be shortened to selected_gene_set1 when there is only one Gene Set file.");
        }

        String fileName = specifier[0];
        String geneSetName = specifier[1];

        String userMatch = geneSetDBPathMap.get(fileName);
        if (userMatch != null) {
            return userMatch + "#" + geneSetName;
        } else {
            throw new IllegalArgumentException("Selected file name '" + fileName + "' not found in submitted Gene Set files.");
        }
    }

    protected static Map<String, String> buildFileNameToFilePathMap(List<String> geneSetDBs) {
        Map<String, String> fileNameToPathMap = new HashMap<String, String>();
        for (String geneSetDB : geneSetDBs) {
            String fileName = FilenameUtils.getName(geneSetDB);
            if (fileNameToPathMap.containsKey(fileName)) {
                throw new IllegalArgumentException("Duplicated file name '" + fileName
                        + "' found in submitted Gene Set files.  This is not allowed with selected.gene.gets.");
            }

            System.out.println("Adding baseName '" + fileName + "' to Map with full path '" + geneSetDB + "'");
            fileNameToPathMap.put(fileName, geneSetDB);
        }
        return fileNameToPathMap;
    }

    protected static final String determineSelectorFromParams(String geneSetDBParam, String geneSetDBListParam,
            String selectedGeneSetsParam, String altDelim, boolean gpMode, File tmpDir, Logger klog) throws IOException {
        boolean paramProcessingError = false;

        // Find the Gene Sets Database files; may be *either* -gmx or -gmx-list (not both)
        // The -gmx-list is intended for use by GenePattern while -gmx is for users of
        // the GSEA distributions. The difference is that -gmx-list takes a GP style
        // file list while -gmx takes the classic GSEA style specifiers (as given by the
        // GUI 'Command' feature).
        boolean haveGmx = StringUtils.isNotBlank(geneSetDBParam);
        boolean haveGmxList = StringUtils.isNotBlank(geneSetDBListParam);
        if (gpMode) {
            if (haveGmx) {
                klog.error("Illegal parameter '-gmx', not valid unless -run_as_genepattern false.");
            }
            if (!haveGmxList) {
                klog.error("One or more gene set files must be specified using the 'gene.sets.database' parameter.");
            }
        } else if (haveGmx && haveGmxList || (!haveGmx && !haveGmxList)) {
            klog.error("One or more gene sets must be specified using either the '-gmx' or '-gmx_list'"
                    + " parameters.  Please use one or the other but not both.");
            klog.error("For '-gmx_list', provide a single text file listing each gene set file, one per line.");
            paramProcessingError = true;
        } else if (haveGmx) {
            // For '-gmx' we use the provided value as-is.
            // Note: selected_gene_sets is invalid with '-gmx'
            if (!StringUtils.isBlank(selectedGeneSetsParam)) {
                klog.error("Invalid use of '-selected_gene_sets': must be blank when using '-gmx'.  Valid for '-gmx_list' only");
                paramProcessingError = true;
            } else {
                return geneSetDBParam;
            }
        }

        String delim = ",";
        Pattern delimPattern = COMMA_PATTERN;
        if (StringUtils.isNotBlank(altDelim)) {
            delim = altDelim;
            delimPattern = Pattern.compile(delim);
        }

        // Otherwise we're processing a file list
        List<String> geneSetDBs = haveGmxList ? FileUtils.readLines(new File(geneSetDBListParam), (Charset) null) : Collections.emptyList();
        if (gpMode) {
            List<String> safeNameGeneSetDBs = new ArrayList<String>(geneSetDBs.size());
            for (String geneSetDB : geneSetDBs) {
                String renamedFile = copyFileWithoutBadChars(geneSetDB, tmpDir);
                if (renamedFile != null) {
                    safeNameGeneSetDBs.add(renamedFile);
                } else {
                    // Something went wrong. Use the original name just to complete checking parameters
                    paramProcessingError = true;
                    safeNameGeneSetDBs.add(geneSetDB);
                }
            }
            geneSetDBs = safeNameGeneSetDBs;
        }

        List<String> selectedGeneSets = StringUtils.isBlank(selectedGeneSetsParam) ? Collections.emptyList()
                : Arrays.asList(delimPattern.split(selectedGeneSetsParam));

        // Join up all of the Gene Set DBs or the selections to be passed in the paramProps.
        List<String> geneSetsSelection = selectedGeneSets.isEmpty() ? geneSetDBs
                : selectGeneSetsFromFiles(geneSetDBs, selectedGeneSets, gpMode);
        paramProcessingError |= (geneSetsSelection == null);

        return (paramProcessingError) ? null : StringUtils.join(geneSetsSelection, delim);
    }

    protected static boolean isCollapseOrRemap(String collapseParam) {
        // TODO: switch to an Enum rather than hard-coded Strings
        // The final clause here is included to cover legacy users.
        return StringUtils.equals("Collapse", collapseParam) || StringUtils.equals("Remap_Only", collapseParam) || 
                StringUtils.equals("true", collapseParam);
    }
    
    public AbstractModule() {
    }
}