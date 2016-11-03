/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.explorer.objmgr;

import edu.mit.broad.genome.objects.PersistentObject;
import edu.mit.broad.genome.parsers.DataFormat;
import edu.mit.broad.genome.parsers.ParserFactory;
import edu.mit.broad.genome.utils.ClassUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;
import java.io.File;

/**
 * @author Aravind Subramanian
 */
public class ObjectTreeRenderer extends DefaultTreeCellRenderer {

    public ObjectTreeRenderer() {
    }

    public Component getTreeCellRendererComponent(final JTree tree,
                                                  final Object value,
                                                  final boolean sel,
                                                  final boolean expanded,
                                                  final boolean leaf,
                                                  final int row,
                                                  final boolean hasFocus) {

        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

        Icon icon = null;
        Object obj = value;

        if (value instanceof DefaultMutableTreeNode) {
            obj = ((DefaultMutableTreeNode) value).getUserObject();
        }

        //System.out.println(">>> " + obj);

        if (obj instanceof String) {
        } else {
            icon = DataFormat.getIcon(obj);
            //log.debug("Checking icon for: " + obj + " got: " + icon );
        }


        String txt = null;
        String desc = null;

        if (obj == null) {

        } else if (obj instanceof PersistentObject) {
            PersistentObject pob = (PersistentObject) obj;

            if (pob.getQuickInfo() != null) {
                StringBuffer buf = new StringBuffer("<html><body>").append(pob.getName());
                buf.append("<font color=gray> [").append(pob.getQuickInfo()).append(']').append("</font></html></body>");
                txt = buf.toString();
            } else {
                txt = pob.getName();
            }

            // @note IMP nopt checking the regular global cache but rather this projectc cache
            //File f = ParserFactory.getCache().getSourceFile(obj);
            File f = ParserFactory.getCache().getSourceFile(obj);

            if (f != null) {
                desc = f.getAbsolutePath();
            } else {
                desc = "Unknown path for pob: " + obj;
            }

        } else { // for the node HEADERS UGGGG not good @todo fix
            // @note maint: some custom labelling to make things more user friendly
            txt = obj.toString();
            txt = ClassUtils.packageName2ClassName(txt);
            // @maint hack to make names more user friendly
            if (txt.equals("FSet")) {
                txt = "<html><body>GeneSet <font color=gray>[grp]</font></body></html>";
            } else if (txt.equalsIgnoreCase("GeneSet")) {
                txt = "Gene sets";
            } else if (txt.equalsIgnoreCase("ErrorPob")) {
                txt = "Errors";
            } else if (txt.equalsIgnoreCase("GeneSetMatrix")) {
                txt = "Gene set databases";
            } else if (txt.equalsIgnoreCase("SampleAnnot")) {
                txt = "Sample annotations";
            } else if (txt.equalsIgnoreCase("GenesOfInterest")) {
                txt = "Genes of interest";
            } else if (txt.equalsIgnoreCase("Dataset")) {
                txt = "Datasets";
            } else if (txt.equalsIgnoreCase("EnrichmentDb")) {
                txt = "Enrichment results";
            } else if (txt.equalsIgnoreCase("Template")) {
                txt = "Phenotypes";
            } else if (txt.equalsIgnoreCase("RankedList")) {
                txt = "RankedGeneList";
            }
        }

        //log.debug("Value: " + obj + " icon: " + icon + " tooltip: " + desc);
        if (txt != null) {
            this.setText(txt);
        }

        if (desc != null) {
            this.setToolTipText(desc);
            this.setOpaque(false);
        }

        if (icon != null) {
            this.setIcon(icon);
        }

        return this;
    }

}    // End Renderer