/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.log4j.Logger;

import edu.mit.broad.genome.XLogger;

/**
 * Zip utilities
 */
public class ZipUtility {
    private static final Logger klog = XLogger.getLogger(ZipUtility.class);

    /**
     * A method for unzipping of a directory archive; see org.genepattern.gsea.LeadingEdgeWidget.main This supports GenePattern Module
     * GSEALeadingEdgeViewer.
     * 
     * @author Chet Birger, David Eby
     */
    public void unzip(File zipArchive, File targetDirectory) throws IOException {
        ZipFile zipFile = new ZipFile(zipArchive);
        try {
            Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
            while (zipEntries.hasMoreElements()) {
                ZipEntry zipEntry = zipEntries.nextElement();
                File targetFile = new File(targetDirectory, zipEntry.getName());
                if (zipEntry.isDirectory()) {
                    targetFile.mkdirs();
                } else {
                    targetFile.getParentFile().mkdirs();
                    targetFile.createNewFile();
                    OutputStream out = new BufferedOutputStream(new FileOutputStream(targetFile));
                    try {
                        InputStream in = zipFile.getInputStream(zipEntry);
                        try {
                            IOUtils.copy(in, out);
                        } finally {
                            in.close();
                        }
                    } finally {
                        out.close();
                    }
                }
            }
        } finally {
            zipFile.close();
        }
    }

    /**
     * Recursively create a ZIP file from a directory.
     * 
     * Inspired by http://stackoverflow.com/questions/23318383/compress-directory-into-a-zipfile-with-commons-io
     * http://stackoverflow.com/questions/204784/how-to-construct-a-relative-path-in-java-from-two-absolute-paths-or-urls
     */
    public void zipDir(File sourceDir, File outputFile) throws IOException {
        klog.info("Zipping: " + sourceDir.getName() + " to " + outputFile.getAbsolutePath());
        ZipOutputStream zipFile = new ZipOutputStream(new FileOutputStream(outputFile));
        Path rootPath = Paths.get(sourceDir.getAbsolutePath());
        try {
            addDirContentsToZip(sourceDir, sourceDir, zipFile, rootPath);
        } finally {
            zipFile.close();
        }
    }

    private void addDirContentsToZip(File rootDir, File sourceDir, ZipOutputStream out, Path rootPath) throws IOException {
        String pathFromRoot = "";
        if (rootDir != sourceDir) {
            Path sourcePath = Paths.get(sourceDir.getAbsolutePath());
            Path relativeSourcePath = rootPath.relativize(sourcePath);
            pathFromRoot = relativeSourcePath.toString();
        }
        for (File file : sourceDir.listFiles()) {
            if (file.isDirectory()) {
                addDirContentsToZip(rootDir, file, out, rootPath);
            } else {
                // We normalize to UNIX-style paths to keep backslashes out of the ZIP bundle.
                String entryPath = FilenameUtils.normalize(FilenameUtils.concat(pathFromRoot, file.getName()), true);
                ZipEntry entry = new ZipEntry(entryPath);
                out.putNextEntry(entry);
                FileInputStream in = new FileInputStream(file);
                try {
                    IOUtils.copy(in, out);
                } finally {
                    in.close();
                }
            }
        }
    }
}