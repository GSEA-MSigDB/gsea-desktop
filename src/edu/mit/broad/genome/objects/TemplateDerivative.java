/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.objects;

/**
 * Something derived from a Template object -> an Aux or auto-splitter
 */
public interface TemplateDerivative {

    public String getName(boolean parentNamePlusMyName, boolean fullPath);

}
