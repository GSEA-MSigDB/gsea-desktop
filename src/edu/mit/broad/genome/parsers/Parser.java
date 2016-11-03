/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.parsers;

import edu.mit.broad.genome.objects.PersistentObject;

import java.io.File;
import java.io.InputStream;
import java.util.List;

/**
 * Basic interface for a Parser.
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public interface Parser {

    /**
     * Parser the source and produce one or more objects
     */
    public List parse(String objname, File file) throws Exception;

    /**
     * sourcepath is the path to the where the data is coming from
     */
    public List parse(String sourcepath, InputStream is) throws Exception;

    /**
     * Save the specified pob to a store
     */
    public void export(PersistentObject pob, File file) throws Exception;

    public void setSilentMode(boolean silent);

}    // End Parser
