/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.core.api;

import java.io.File;

/**
 * @author Aravind Subramanian
 */
public class XStores {

    public static class FilePathStore extends XStore {
        FilePathStore(final File file) {
            super.init(file, new AdditionDecider() {
                public String addThis(final String str) {
                    if (str == null || str.length() == 0) {
                        return null;
                    }

                    if (str.endsWith(".rpt")) {
                        return null;
                    }

                    File file = new File(str);
                    if (file.exists() && !contains(str)) {
                        return str;
                    }

                    return null;
                }
            });
        }
    }

    public static class DirPathStore extends XStore {
        DirPathStore(final File file) {
            super.init(file, new AdditionDecider() {
                public String addThis(final String str) {
                    if (str == null || str.length() == 0) {
                        return null;
                    }

                    if (str.endsWith(".rpt")) {
                        return null;
                    }

                    File dir = new File(str);
                    if (dir.isDirectory() == false) {
                        dir = dir.getParentFile();
                    }

                    if (dir.exists() &&
                            !contains(str) &&
                            !contains(dir.getPath()) &&
                            !contains(dir.getAbsolutePath())) {
                        return dir.getPath();
                    }

                    return null;
                }
            });
        }
    }

    public static class StringStore extends XStore {
        StringStore(final File file) {
            super.init(file, new AdditionDecider() {
                public String addThis(final String str) {
                    if (str == null || str.length() == 0) {
                        return null;
                    }

                    if (!contains(str)) {
                        return str;
                    } else {
                        return null;
                    }
                }
            });
        }
    }
}
