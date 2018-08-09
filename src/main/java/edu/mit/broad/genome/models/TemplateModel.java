/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.models;

import edu.mit.broad.genome.XLogger;
import edu.mit.broad.genome.objects.Template;
import org.apache.log4j.Logger;

import javax.swing.table.AbstractTableModel;

/**
 * An implementation of AbstractTableModel for Templates. <br>
 * <p/>
 * Must not replicate any of Templates's data strcutures to optimize on memory
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class TemplateModel extends AbstractTableModel {

    /**
     * The underlying Template being modell'ed
     */
    private final Template fTemplate;

    /**
     * Column labels for the table. Using "Class" rather than "Template" as
     * users are more familiar wit that term - i think.
     */
    private static final String[] kColNames = {"Class Name", "Class Id", "Class Count"};

    /**
     * For logging support
     */
    private final Logger log = XLogger.getLogger(TemplateModel.class);

    /**
     * Class Constructor.
     * Initializes model to specified Template.
     */
    public TemplateModel(Template template) {
        this.fTemplate = template;
    }

    /**
     * Always kColNames.length
     */
    public int getColumnCount() {
        return kColNames.length;
    }

    /**
     * As many rows as there are *assignments* (NOT items)
     */
    public int getRowCount() {

        //log.debug("rowcount=" + fTemplate.getNumClasses());
        return fTemplate.getNumClasses();
    }

    /**
     * @return name of col
     */
    public String getColumnName(int col) {
        return kColNames[col];
    }

    /**
     * @return value of specified cell.
     */
    public Object getValueAt(int row, int col) {
        if (col == 0) {
            return fTemplate.getClassName(row);
        } else if (col == 1) {
            // classid is really template id
            return fTemplate.getClass(row).getItem(0).getId();    // any item
        } else if (col == 2) {
            return Integer.toString(fTemplate.getClass(row).getSize());
        } else {
            log.warn("Unexpectedly i was asked for coln=" + col);
            return null;
        }
    }

    /**
     * JTable uses this method to determine the default renderer
     * editor for each cell.
     * Its always a String here as TemplatesItems are symbollic??
     */
    public Class getColumnClass(int col) {
        return String.class;
    }

    /**
     * Is the model editable or not?
     */
    public boolean isEditable() {
        return false;
    }

    /**
     * Either the entire table model is editable or the entire table is not editable.
     * That property is set using <code>setEditable()</code>
     */
    public boolean isCellEditable(int row, int col) {
        return false;
    }
}    // End TemplateModel
