/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.objects;

/**
 * @author Aravind Subramanian
 */
public interface IDataframe extends PersistentObject {

    public Object getElementObj(int rown, int coln);

    public String getRowName(int rown);

    public String getColumnName(int coln);

    public int getNumRow();

    public int getNumCol();
} // End IDataframe
