/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xtools.api.ui;

import javax.swing.*;

/**
 * @author Aravind Subramanian
 */
public class NamedModel {

    public ListModel model;
    public String name;

    public NamedModel(final String name, final ListModel model) {
        this.name = name;
        this.model = model;
    }
}
