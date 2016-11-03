/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.objects;

/**
 * @author Aravind Subramanian
 */
public class ProbePreTemplate extends AbstractPreTemplate {

    private String fJustProbeName;

    /**
     * Class constructor
     *
     * @param pts
     */
    public ProbePreTemplate(final String sourceFileName, final String justProbeName) {
        super(sourceFileName + "#" + justProbeName); // name doesnt matter as invisible
        this.fJustProbeName = justProbeName;
    }

    public boolean isContinuous() {
        return true; // @note always
    }

    public boolean isCategorical() {
        return false;
    }

    public boolean isAux() {
        return true; // doesnt matter what
    }

} // End class ProbePreTemplate
