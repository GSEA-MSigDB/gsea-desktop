/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package org.genepattern.io;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * An abstract class for reading documents.
 *
 * @author Joshua Gould
 */
public abstract class AbstractReader {
    List suffixes;

    String formatName;

    protected AbstractReader(String[] _suffixes, String _formatName) {
        suffixes = Collections.unmodifiableList(Arrays.asList(_suffixes));
        formatName = _formatName;
    }

}

