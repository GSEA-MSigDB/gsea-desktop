/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.math;

import edu.mit.broad.genome.objects.LabelledVector;

import java.util.List;

/**
 * @author Aravind Subramanian
 */
public interface LabelledVectorProcessor {

    public String getName();

    public LabelledVector process(LabelledVector lv);

    public void process(List<DoubleElement> dels);

}
