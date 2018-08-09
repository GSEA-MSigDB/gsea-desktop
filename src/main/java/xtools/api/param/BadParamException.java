/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xtools.api.param;

import edu.mit.broad.genome.StandardException;

/**
 * @author Aravind Subramanian
 */
public class BadParamException extends StandardException {

    /**
     * Class constructor
     *
     * @param badParams
     */
    public BadParamException(final String title, int errorCode) {
        super(title, errorCode);
    }

} // End class BadParamException
