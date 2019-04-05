/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.viewers;

import com.jidesoft.grid.SortableTable;
import edu.mit.broad.genome.JarResources;
import edu.mit.broad.vdb.chip.Chip;
import edu.mit.broad.vdb.chip.NullSymbolModes;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;

/**
 * ChipViewer viewer.
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class ChipViewer extends AbstractViewer {

    public static final Icon ICON = JarResources.getIcon("Chip16.png");
    public static final String NAME = "ChipViewer";

    private Chip fChip;

    /**
     * @param gset
     */
    public ChipViewer(final Chip chip) {
        super(NAME, ICON, chip);

        this.fChip = chip;

        Model model = new Model(fChip);
        SortableTable table = createTable(model, true, true);

        setColumnSize(100, 1, table, false);
        this.setLayout(new BorderLayout());
        this.add(createAlwaysScrollPane(table), BorderLayout.CENTER);
        this.revalidate();

    }

    public JMenuBar getJMenuBar() {
        return EMPTY_MENU_BAR;
    }

    /**
     * An implementation of AbstractTableModel for ChipViewer.
     *
     * @author Aravind Subramanian
     * @version %I%, %G%
     */
    public static class Model extends AbstractTableModel {

        /**
         * The underlying object being modell'ed
         */
        private final Chip fChip;

        private static String[] COL_NAMES = new String[]{"FEATURE", "SYMBOL", "TITLE"};

        /**
         * Class Constructor.
         * Initializes model to specified Template.
         */
        public Model(Chip chip) {
            this.fChip = chip;
        }

        /**
         * Always kColNames.length
         */
        public int getColumnCount() {
            return COL_NAMES.length;
        }

        /**
         * As many rows as there are elements
         */
        public int getRowCount() {
            try {
                return fChip.getNumProbes();
            } catch (Exception e) {
                e.printStackTrace();
                return 0;
            }
        }

        public String getColumnName(int col) {
            return COL_NAMES[col];
        }

        private boolean once = false;

        public Object getValueAt(int row, int col) {

            try {
                String probeName = fChip.getProbeName(row);
                if (col == 0) {
                    return probeName;
                } else if (col == 1) {
                    return fChip.getSymbol(probeName, NullSymbolModes.OmitNulls);
                } else {

                    return fChip.getTitle(probeName, NullSymbolModes.OmitNulls);
                }
            } catch (Exception e) {
                if (!once) {
                    e.printStackTrace();
                    once = true;
                }
            }

            return null;
        }

        public Class getColumnClass(int col) {
            return String.class;
        }

        public boolean isCellEditable(int row, int col) {
            return false;
        }

    }    // End Inner class Model


}        // End ChipViewer
