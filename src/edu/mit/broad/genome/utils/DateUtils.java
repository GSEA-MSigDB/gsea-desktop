/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.utils;

import java.util.Date;

import org.apache.commons.lang3.time.FastDateFormat;

/**
 * Date formatting utilities.
 *
 * @author David Eby
 */
public class DateUtils {
    private static final FastDateFormat dayMonthYearFormatter = FastDateFormat
            .getInstance("EEE, MMM d, ''yy");
    private static final FastDateFormat hourMinFormatter = FastDateFormat.getInstance("K a m");

    public static String formatAsDayMonthYear(Date d) {
        return dayMonthYearFormatter.format(d);
    }

    public static String formatAsHourMin(Date d) {
        return hourMinFormatter.format(d);
    }
} // End DateUtils
