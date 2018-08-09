/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome;

import edu.mit.broad.genome.utils.SystemUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * IMP IMP IMP: Keep this class thin and light as it is loaded up at startup
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class Conf {

    public static int EXIT_CODE_ERROR = 1;
    public static int EXIT_CODE_OK = 0;

    public static void exitSystem(final boolean thereWasAnError) {

        if (thereWasAnError) {
            System.exit(EXIT_CODE_ERROR);
        } else {
            System.exit(EXIT_CODE_OK);
        }

    }

    public static boolean isGseaApp() {
        return SystemUtils.isPropertyTrue("GSEA");
    }

    /**
     * Did the application launch is debug mode
     * (or is the application currently in debug mode)
     *
     * @return true if in debug mode
     */
    public static boolean isDebugMode() {

        final String debug = SystemUtils.getProperty(Constants.DEBUG_MODE_KEY, false);

        if (debug == null) {
            return false;
        } else {
            return Boolean.valueOf(debug).booleanValue();
        }
    }

    public static boolean isMakeReportDirOffMode() {
        final String ts = SystemUtils.getProperty(Constants.MAKE_REPORT_DIR_KEY, false);

        //System.out.println("$$$$$$ chekcing isDeterministicReportNameMode: " + ts);

        if (ts == null || ts.length() == 0) {
            return false;
        } else {
            // is timestamp is specified as false then it IS det
            boolean det = !Boolean.valueOf(ts).booleanValue();
            if (det) {
                System.out.println("Working in DETERMINISTIC report mode -- a subdir ts will not be applied");
            }
            return det;
        }
    }
} // End inner class Conf
