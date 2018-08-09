/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.alg;

import edu.mit.broad.genome.math.Vector;
import edu.mit.broad.genome.objects.Template;

/**
 * @author Aravind Subramanian
 */
public class VectorSplitter {

    private int numtested;
    private int numtoofewA;
    private int numtoofewB;
    private int minNumNanlessPointsNeeded;

    /**
     * Class constructor
     */
    public VectorSplitter(int minNumNanlessPointsNeeded) {
        this.minNumNanlessPointsNeeded = minNumNanlessPointsNeeded;
    }

    public Vector[] splitBiphasic_nansafe(final Vector profile, final Template template) {

        Vector[] vs = template.splitByTemplateClass(profile);

        if (vs.length != 2) {
            _barf_not_biphasic(template, vs);
        }

        numtested++;

        // @note change for MV datasets
        vs[0] = vs[0].toVectorNaNless();
        vs[1] = vs[1].toVectorNaNless();

        if (vs[0].getSize() < minNumNanlessPointsNeeded) {
            numtoofewA++;
            //System.out.println("Too few good data points in 0 : " + vs[0].toString() + " " + template.getClass(0).getName());
            //throw new IllegalArgumentException("Too few good data points in 0 : " + vs[0].toString());
            return null;
        }

        if (vs[1].getSize() < minNumNanlessPointsNeeded) {
            numtoofewB++;
            //System.out.println("Too few good data points in 1: " + vs[1].toString() + " " + template.getClass(1).getName());
            //throw new IllegalArgumentException("Too few good data points in 1: " + vs[1].toString());
            return null;
        }

        return vs;
    }

    private static String _barf_not_biphasic(final Template template, final Vector[] vs) {
        StringBuffer buf = new StringBuffer("Template is not biphasic. Name: " + template.getName() + " # splits= " + vs.length);
        buf.append("\n<br>This metric can only be used with 2 class comparisons");
        throw new RuntimeException(buf.toString());
    }

    public Vector[] splitBiphasic(final Vector profile, final Template template) {

        final Vector[] vs = template.splitByTemplateClass(profile);

        if (vs.length != 2) {
            _barf_not_biphasic(template, vs);
        }

        if (vs[0] == null) {
            throw new IllegalArgumentException("Split vector 0 is null");
        }

        if (vs[1] == null) {
            throw new IllegalArgumentException("Split vector 1 is null");
        }

        return vs;
    }

} // End class VectorSplitter
