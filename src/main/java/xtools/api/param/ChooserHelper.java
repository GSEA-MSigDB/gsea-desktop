/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xtools.api.param;

import edu.mit.broad.genome.objects.PersistentObject;
import edu.mit.broad.genome.parsers.ParserFactory;

/**
 * @author Aravind Subramanian
 */
public class ChooserHelper {

    // overr base cl method to do for both pobs and strings
    public static String formatPob(Object[] vals) {
        if (vals == null) {
            return "";
        }

        StringBuffer buf = new StringBuffer();

        for (int i = 0; i < vals.length; i++) {
            if (vals[i] == null) {
                continue;
            }

            if (vals[i] instanceof PersistentObject) {
                String p = ParserFactory.getCache().getSourcePath(vals[i]);
                buf.append(p);
            } else {
                buf.append(vals[i].toString().trim());
            }

            if (i != vals.length - 1) {
                buf.append(',');
            }
        }

        return buf.toString();
    }

} // End ChooserHelper
