/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome;

import edu.mit.broad.genome.math.Vector;
import edu.mit.broad.genome.objects.Template;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntObjectIterator;

import java.util.Iterator;
import java.util.List;

/**
 * <p>
 * Console or log printing utilities for debugging.  Note that much of this  
 * functionality has been subsumed by modern Java and other libraries.  This
 * is being kept at present for legacy purposes.
 * <p/>
 * <p>
 * Originally had a basis in http://www.ibiblio.org/javafaq/formatter/ but
 * it no long looks like that or is implemented with that code.
 * <p/>
 * <p>
 * <em>Original javadoc notes:</em>
 * Not very oopy, but experience has proven that its better to collect these
 * into a single class then embed printf() functionality into objects.
 * <p/>
 * /**
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */

public class Printf {

    private static final int DEFAULT_PRECISION = 4;
    private static final String DEFAULT_PRECISION_STR = "%.4f";

    /**
     * Privatized class constructor to prevent instantiation.
     */
    private Printf() {
    }


    public static String outs(final TIntObjectHashMap map) {

        if (map == null) {
            return "null TIntObjectHashMap";
        } else if (map.isEmpty()) {
            return "empty TIntObjectHashMap";
        }

        TIntObjectIterator iterator = map.iterator();

        StringBuffer buf = new StringBuffer();
        for (int i = map.size(); i-- > 0;) {
            iterator.advance();
            buf.append(iterator.key()).append('\t').append(iterator.value()).append('\n');
        }

        return buf.toString();
    }

    public static String format(final Vector v, final char delim) {
        return format(v, DEFAULT_PRECISION, delim);
    }

    public static String format(final Vector v, final int precision, final char delim) {

        if (v == null) {
            return "";
        }

        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < v.getSize(); i++) {
            buf.append(format(v.getElement(i), precision));
            if (i != v.getSize() - 1) {
                buf.append(delim);
            }
        }

        return buf.toString();
    }

    public static String outs(final float[] arr) {

        if (arr == null) {
            return "Null array";
        }

        StringBuffer buf = new StringBuffer("array length: " + arr.length).append('\n');
        int max = 20;

        if (max > arr.length) {
            max = arr.length;
        }

        for (int i = 0; i < max; i++) {
            buf.append(arr[i]).append(' ');
        }

        buf.append("\nand so on ...\n");

        return buf.toString();
    }

    public static void out(final int[] arr) {
        System.out.println(outs(arr));
    }

    public static String outs(final int[] arr) {

        if (arr == null) {
            return "Null array";
        }

        StringBuffer buf = new StringBuffer("array length: " + arr.length).append('\n');
        int max = 200;

        if (arr.length < max) {
            max = arr.length;
        }

        for (int i = 0; i < max; i++) {
            buf.append(arr[i]).append(' ');
        }

        buf.append("\nand so on ...\n");

        return buf.toString();
    }

    public static String format(int[] arr, char delim) {

        if (arr == null) {
            return "";
        }

        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < arr.length; i++) {
            buf.append(arr[i]);
            if (i != arr.length - 1) {
                buf.append(delim);
            }
        }

        return buf.toString();
    }

    //TODO: Review uses for consistency in report precision.
    public static String format(final float f, final int precision) {
        String formatString = new StringBuffer("%.").append(precision).append('f').toString();
        String s = String.format(formatString, f);
        if (s.endsWith(".")) {
            return s.substring(0, s.length() - 1);
        } else {
            return s;
        }
    }

    public static String format(final float f) {
        return String.format(DEFAULT_PRECISION_STR, f);
    }
    
    public static void out(final Object[] arr) {
        System.out.println(outs(arr));
    }

    // no truncation
    public static String outs(final Object[] arr) {

        if (null == arr) {
            return "null array";
        }

        if (arr.length == 0) {
            return ("empty array");
        }

        StringBuffer buf = new StringBuffer();
        buf.append("# of elements = ").append(arr.length).append('\n');

        for (int i = 0; i < arr.length; i++) {
            buf.append(arr[i]).append(' ');
        }

        return buf.toString();
    }

    public static StringBuffer outs(final Template template) {

        StringBuffer buf = new StringBuffer();

        if (template == null) {
            return new StringBuffer("null template");
        }

        buf.append("Template id: ").append(template.getId()).append(" name: ").append(template.getName()).append('\n');
        buf.append("Number of classes: ").append(template.getNumClasses()).append('\n');
        buf.append("Classes:\n");

        for (int i = 0; i < template.getNumClasses(); i++) {
            buf.append(template.getClass(i).getMembershipInfo()).append('\n').append('\n');
        }

        buf.append('\n');


        buf.append("Template is continuous: ").append(template.isContinuous()).append('\n');
        buf.append("Template is aux: ").append(template.isAux()).append('\n');
        buf.append("Class of Interest (COI) is: ").append(template.getClassOfInterestName()).append(" Index: ").append(template.getClassOfInterestIndex());
        buf.append('\n');

        buf.append("Total # of items: ").append(template.getNumItems()).append('\n').append('\n');
        buf.append("Output as string follows:\n");
        buf.append(template.getAsString(false)).append('\n');

        return buf;
    }

    public static StringBuffer outs(Template.Item ti) {

        StringBuffer s = new StringBuffer();

        if (ti == null) {
            return new StringBuffer("null Template.Item");
        }

        s.append("Item id: ").append(ti.getId()).append(" pos: ").append(ti.getProfilePosition()).append('\n');

        return s;
    }

    public static StringBuffer outs(List list) {

        if (list == null) {
            return new StringBuffer("null List");
        }

        return outs(list.iterator());
    }

    public static StringBuffer outs(final Iterator it) {

        StringBuffer s = new StringBuffer();

        if (it == null) {
            return new StringBuffer("null Iterator");
        }

        int cnt = 0;

        while (it.hasNext()) {
            Object key = it.next();

            if (key == null) {
                s.append("null");
            } else {
                s.append(key);
            }

            s.append('\t');

            if (cnt % 20 == 0) {
                s.append('\n');
            }

            if (cnt >= 100) {
                s.append("\nand so on ...");
                break;
            }

            cnt++;
        }

        while (it.hasNext()) {
            it.next();

            cnt++;
        }

        s.append('\n');
        s.append("Number of elements: ").append(cnt);
        s.append('\n');
        return s;
    }
}    // End Printf

/*--- Formatted in Sun Java Convention Style on Fri, Sep 27, '02 ---*/