/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xapps.gsea;

/**
 * Subclass of xapps.gsea.Main solely for the purpose of launching the app under a different
 * class name.
 * 
 * This is a hack to change the name of the first menu displayed in the Mac Finder menu bar.
 * It should really be done by a -Xdock:name="GSEA" setting or the equivalent.
 * @author David Eby
 */
public class GSEA extends Main {
    public static void main(String[] args) {
        Main.main(args);
    }
}
