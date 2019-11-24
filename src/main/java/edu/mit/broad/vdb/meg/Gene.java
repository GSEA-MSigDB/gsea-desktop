/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.vdb.meg;

/**
 * @author Aravind Subramanian
 */
public interface Gene {

    public String getSymbol();

    public String getTitle();

    public String getTitle_truncated();

    public static class Helper {

        public static String getTitle_truncated(String title) {
            if (title != null && title.length() > 150) {
                return title.substring(150);
            }

            return title;
        }
    }
}