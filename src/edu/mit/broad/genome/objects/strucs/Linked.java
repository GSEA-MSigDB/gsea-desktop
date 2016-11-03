/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.objects.strucs;

/**
 * @author Aravind Subramanian
 *         <p/>
 *         // kept here to avoid the vdb loader constraints
 */
public interface Linked {

    public String getText();

    public Hyperlink createDefaultLink();

    public Hyperlink[] createAllLinks();

} // End interface Linked
