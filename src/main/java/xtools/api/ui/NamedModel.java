/*
 * Copyright (c) 2003-2022 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xtools.api.ui;

import javax.swing.ListCellRenderer;
import javax.swing.ListModel;

/**
 * @author Aravind Subramanian, David Eby
 */
public class NamedModel {
    public ListModel model;
    public String name;
    public ListCellRenderer renderer;

    public NamedModel(final String name, final ListModel model, ListCellRenderer renderer) {
        this.name = name;
        this.model = model;
        this.renderer = renderer;
    }
}
