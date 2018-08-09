/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.xchoosers;

import edu.mit.broad.genome.objects.Template;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * only non aux templates are added / used
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class TemplateNonAuxBoxModel implements ComboBoxModel {

    private ComboBoxModel fOrigModel;
    private List objects;

    private int fSelIndex = 0;

    /**
     * Class Constructor.
     *
     * @param model
     */
    public TemplateNonAuxBoxModel(ComboBoxModel model) {
        model.addListDataListener(new MyListDataListener());
        init(model);
    }

    private void init(ComboBoxModel model) {
        this.fOrigModel = model;

        this.objects = new ArrayList();

        for (int i = 0; i < fOrigModel.getSize(); i++) {
            Template template = (Template) fOrigModel.getElementAt(i);
            //log.debug("Checking for adding: " + template.getName() + " aux: " + template.isAux() + " cont: " + template.isContinuous());

            if (template.isContinuous()) { // must be first as cont is non aux also
                objects.add(template);
            } else if (template.isAux() == false) {
                objects.add(template);
            }

            /*
            if (template.isContinuous()) {
                File file = ParserFactory.getCache().getSourceFile(template);
                TemplateContWrapper wr = new TemplateContWrapper(file);
                if (objects.contains(wr) == false) {
                    objects.add(file);
                }
            } else if (template.isAux() == false) {
                objects.add(template);
            }
            */
        }
    }

    /**
     * For listening to the real model
     */
    class MyListDataListener implements ListDataListener {
        public void intervalAdded(ListDataEvent e) {
            init(fOrigModel);
        }


        public void intervalRemoved(ListDataEvent e) {
            init(fOrigModel);
        }

        public void contentsChanged(ListDataEvent e) {
            init(fOrigModel);
        }
    } // End inner class MyListDataListener


    public void addListDataListener(ListDataListener l) {
        fOrigModel.addListDataListener(l);
    }

    public Object getElementAt(int index) {
        if (objects.size() == 0) {
            return null;
        }

        return objects.get(index);
    }

    public int getSize() {
        return objects.size();
    }

    public void removeListDataListener(ListDataListener l) {
        fOrigModel.removeListDataListener(l);
    }

    /**
     * ComboBoxModel impl.
     *
     * @return
     */
    public Object getSelectedItem() {
        if (objects.size() == 0) {
            return null;
        }

        if (fSelIndex >= objects.size()) {
            return null;
        }

        return objects.get(fSelIndex);
    }

    /**
     * ComboBoxModel implementation
     */
    public void setSelectedItem(Object obj) {
        this.fSelIndex = objects.indexOf(obj);
    }


    public static class TemplateContWrapper {
        File sourceFile;

        public int hashCode() {
            return sourceFile.hashCode();
        }

        public boolean equals(Object obj) {
            return obj.equals(sourceFile);
        }

        public String toString() {
            return sourceFile.toString();
        }

    }

} // End TemplateBoxModel
