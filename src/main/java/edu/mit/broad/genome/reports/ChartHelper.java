/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.reports;

import edu.mit.broad.genome.XLogger;
import org.apache.log4j.Logger;

import java.awt.*;
import java.awt.geom.Ellipse2D;

/**
 * @author Aravind Subramanian
 */
public class ChartHelper {

    protected static final transient Logger klog = XLogger.getLogger(ChartHelper.class);

    public static Shape createCircleShape() {
        return new Ellipse2D.Float(2f, 2f, 2f, 2f);
    }

}
