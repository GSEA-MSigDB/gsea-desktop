/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.utils;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.util.*;

/**
 * Collection of utility methods related to Files.
 * <p/>
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class FileUtils {
    /**
     * For logging support
     */
    private final static Logger klog = Logger.getLogger(FileUtils.class);

    /**
     * The file is opened and closed after counting
     *
     * @param file
     * @param ignoreblanklines
     * @return
     * @throws IOException
     */
    public static int countLines(File file, boolean ignoreblanklines) throws IOException {
        BufferedReader bin = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        return _countLines(bin, ignoreblanklines);
    }

    public static int countLines(String path, boolean ignoreblanklines) throws IOException {
        return countLines(new File(path), ignoreblanklines);
    }

    // reader should be untouched, reader is closed when done
    private static int _countLines(BufferedReader reader, boolean ignoreblanklines)
            throws IOException {
        try {
            int numberOfLines = 0;

            if (!ignoreblanklines) {
                while (reader.readLine() != null) {
                    numberOfLines++;
                }
            } else {
                String currLine = null;
                while ((currLine = reader.readLine()) != null) {
                    if (currLine.trim().length() > 0) {
                        numberOfLines++;
                    }
                }
            }
            return numberOfLines;
        } finally {
            reader.close();
        }
    }

    // shorten as easier to read
    public static String shortenedPathRepresentation(File file) {

        if ((file != null) && (file.getParentFile() != null)) {
            return (".." + File.separator + file.getParentFile().getName() + File.separator + file.getName());
        } else if (file != null) {
            return file.getName();
        } else {
            return null;
        }

    }

    public static void copy(URL url, File fileB) throws IOException {
        InputStream source = url.openStream();
        FileOutputStream target = new FileOutputStream(fileB);
        try {
            IOUtils.copy(source, target);
        }
        finally {
            target.close();
        }
    }

    /**
     * Writes specified String to specified file.
     * File must exist. Any existing content is overwritten.
     *
     * @throws IOException on errors
     */
    public static void write(String s, File f) throws IOException {
        // open PrintWriter in overwrite mode
        PrintWriter out = new PrintWriter(new FileOutputStream(f));
        try {
            IOUtils.write(s, out);
        }
        finally {
            out.close();
        }
    }

    public static void writeSafely(String s, File f) {
        try {
            write(s, f);
        } catch (Throwable t) {
            klog.error("Writing to file: " + f + " failed");
        }
    }

    /**
     * One element per line
     *
     * @param list
     * @param file
     * @throws IOException
     */
    public static void write(List<?> list, File file) throws IOException {
        PrintWriter out = new PrintWriter(new FileOutputStream(file));
        try{
            IOUtils.writeLines(list, IOUtils.LINE_SEPARATOR, out);
        }
        finally {
            out.close();
        }
    }

    /*
     * As with toURL, unused for now but keep the code for possible revamp.
     */
    public static BufferedReader toBufferedReader(String filepathORurl) throws IOException {
        URL url = toURL(filepathORurl);
        return new BufferedReader(new InputStreamReader(url.openStream()));
    }

    /*
     * This detection routine is bogus, failing on Windows with e.g. 'C:/' file paths being
     * detected as URLs.  Keeping the code for now as we may revamped soon, but it will be 
     * unused in the meanwhile.
     */
    private static URL toURL(String filepathORurl) throws IOException {
        if (StringUtils.contains(filepathORurl, ":")) {
            return new URL(filepathORurl);
        } else {
            URI uri = new File(filepathORurl).toURI();
            return uri.toURL();
        }
    }

    public static File findFile(final File dir, final String endsWith, boolean barfOnMissing) {
        Collection<File> allMatches = org.apache.commons.io.FileUtils.listFiles(dir,
                new String[] { endsWith }, false);        
        
        if (allMatches.isEmpty()) {
            if (barfOnMissing) {
                throw new IllegalArgumentException("No file with endsWith: " + endsWith + " found in dir: "
                        + dir + "\n");
            } else {
                return null;
            }
        }
        
        return allMatches.iterator().next();
    }

    /**
     * Need -> to detect of some other external process (like ms excel) has a file open and thus
     * locked. If java tries to write to such a file, a IOException is thrown.
     * This method will attempt to detect if the specified file is locked by some such external process
     * Java doesnt have this built -- this method is a hck copied from a javalang.help posting.
     * Not sure if it will work in all cases, but seems reasonable.
     * <p/>
     * If file doesnt exts, returns false always
     *
     * @param file
     * @return
     */
    // TODO: Clean this up...
    // Actually, this is probably not robust anyway - there is going to be a race condition using this
    // method no matter how it is implemented, so it's probably better to just write to the file, detect
    // any issues, and recover from there.
    // There's only one use so I think we can get there.
    public static boolean isLocked(File file) {

        if (!file.exists()) {
            return false;    // cant be locked if it doesnt exist!
        }

        File origFile = new File(file.getAbsolutePath());

// cant use this as it actually makes a result file by itslef, and then cant rename into it
//File newFile = File.createTempFile(file.getName(), ".lock_check");
        StringBuffer btmp = new StringBuffer(file.getName()).append(System.currentTimeMillis()).append(".lock_check");
        File newFile = new File(SystemUtils.getTmpDir(), btmp.toString());

//log.debug("Created lock result file: " + newFile.getPath() + " file: " + file.getAbsolutePath());
        boolean able2rename = file.renameTo(newFile);

//log.debug("able2rename: " + able2rename);
        if (!able2rename) {
            return true;    // couldnt rename, so we take this to mean that it must be locked
        } else {

// move it back!
            boolean movedback = newFile.renameTo(origFile);

            if (!movedback) {
                throw new IllegalStateException("Bad error - couldnt move file back after lock check");
            }

            return false;
        }
    }
}        // End FileUtils
