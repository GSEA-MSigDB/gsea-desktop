/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.viewers;

import edu.mit.broad.genome.JarResources;
import edu.mit.broad.genome.Printf;
import edu.mit.broad.genome.models.TemplateModel;
import edu.mit.broad.genome.objects.Template;
import edu.mit.broad.genome.swing.GuiHelper;

import javax.swing.*;
import java.awt.*;

/**
 * Phenotype viewer.
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class PhenotypeViewer extends AbstractViewer {

    public static final Icon ICON = JarResources.getIcon("Cls.gif");
    public static final String NAME = "PhenotypeViewer";

    private final Template fTemplate;

    /**
     * Class Constructor.
     *
     * @param template
     */
    public PhenotypeViewer(final Template template) {

        super(NAME, ICON, template);

        this.fTemplate = template;

        jbInit();
    }

    private void jbInit() {
        this.setLayout(new GridLayout(1, 1));
        JTabbedPane tp = new JTabbedPane(JTabbedPane.BOTTOM);
        tp.addTab("Phenotype Structure", new TableView(fTemplate));
        tp.addTab("Phenotype Text", new TextView(fTemplate));
        tp.addTab("Phenotype Info", new InfoView(fTemplate));
        this.add(tp);
    }


    /**
     * Structure tableview of Template
     */
    public static class TableView extends JPanel {

        private final JTable tableStructured;

        /**
         * Class Constructor.
         * Creates a new empty Tabel chart.
         */
        public TableView(Template template) {

            tableStructured = createTable(new TemplateModel(template), false, true);
            tableStructured.revalidate();
            tableStructured.repaint();
            GuiHelper.fill(this, createAlwaysScrollPane(tableStructured));
            this.revalidate();
        }
    }    // End TableView

    /**
     * Free text - cls format like view of Template
     */
    public static class TextView extends JPanel {

        private final JTextArea taFreeText;

        /**
         * Class Constructor.
         * Creates a new empty FreeText chart.
         */
        public TextView(Template template) {

            taFreeText = new JTextArea(template.getAsString(false));

            taFreeText.setEditable(false);
            GuiHelper.fill(this, new JScrollPane(taFreeText));
            this.revalidate();
        }
    }    // End TextView

    /**
     * Info on the template
     */
    public static class InfoView extends JPanel {

        private final JTextArea taFreeText;

        /**
         * Class Constructor.
         * Creates a new empty FreeText chart.
         */
        public InfoView(Template template) {

            taFreeText = new JTextArea(Printf.outs(template).toString());
            taFreeText.setEditable(false);
            GuiHelper.fill(this, new JScrollPane(taFreeText));
            this.revalidate();
        }
    }    // End InfoView

}        // End TemplateViewer
