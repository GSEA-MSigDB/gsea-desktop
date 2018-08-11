/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.searchers;

import com.jidesoft.list.QuickListFilterField;
import com.jidesoft.swing.JideTitledBorder;
import com.jidesoft.swing.PartialEtchedBorder;
import com.jidesoft.swing.PartialSide;
import com.jidesoft.swing.SearchableUtils;
import org.apache.log4j.Logger;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author Aravind Subramanian
 */
public class GeneSearchList {

    private JPanel fMainPanel;

    private java.util.List fFeatureNames;

    private JList fSearchList;

    private DefaultListModel listModel;

    private Logger log = Logger.getLogger(GeneSearchList.class);


    public GeneSearchList() {
        this.fFeatureNames = new ArrayList();
    }

    public JComponent getComponent() {
        if (fMainPanel == null) {
            jbInit();
        }

        return fMainPanel;
    }

    public JList getJList() {
        if (fMainPanel == null) {
            jbInit();
        }

        return fSearchList;
    }

    //Map listModelMap;

    // @todo improve this

    public void setFeatures(final java.util.List featureNames) {
        log.debug("setFeatures: " + featureNames.size());
        this.fFeatureNames = featureNames;

        /*
        if (listModelMap == null) {
            listModelMap = new WeakHashMap();
        }

        DefaultListModel newListModel;
        Object obj = listModelMap.get(featureNames);
        if (obj != null) {
            newListModel = (DefaultListModel) obj;
        } else {
            newListModel = new DefaultListModel();
            //listModel.removeAllElements();
            for (int i = 0; i < fFeatureNames.size(); i++) {
                newListModel.addElement(fFeatureNames.get(i));
                //listModel.addElement(fFeatureNames.get(i));
            }
            listModelMap.put(featureNames, newListModel);
        }

        this.listModel = newListModel;
        */

        listModel.removeAllElements();
        for (int i = 0; i < fFeatureNames.size(); i++) {
            listModel.addElement(fFeatureNames.get(i));
        }

        fSearchList.revalidate();
        fMainPanel.revalidate();
    }

    public void jbInit() {

        listModel = new DefaultListModel();
        for (int i = 0; i < fFeatureNames.size(); i++) {
            listModel.addElement(fFeatureNames.get(i));
        }

        this.fMainPanel = new JPanel(new BorderLayout(6, 6));

        JPanel quickSearchPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        final QuickListFilterField field = new QuickListFilterField(listModel);
        quickSearchPanel.add(field);
        quickSearchPanel.setBorder(new JideTitledBorder(new PartialEtchedBorder(PartialEtchedBorder.LOWERED, PartialSide.NORTH), "QuickListFilterField", JideTitledBorder.LEADING, JideTitledBorder.ABOVE_TOP));

        this.fSearchList = new JList(field.getDisplayListModel());
        fSearchList.setVisibleRowCount(30);
        field.setList(fSearchList);
        SearchableUtils.installSearchable(fSearchList);

        JPanel listPanel = new JPanel(new BorderLayout(2, 2));
        listPanel.setBorder(BorderFactory.createCompoundBorder(new JideTitledBorder(new PartialEtchedBorder(PartialEtchedBorder.LOWERED, PartialSide.NORTH), "Filtered features List", JideTitledBorder.LEADING, JideTitledBorder.ABOVE_TOP),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)));
        final JLabel label = new JLabel(field.getDisplayListModel().getSize() + " out of " + listModel.getSize() + " features");
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        fSearchList.registerKeyboardAction(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                int[] selection = fSearchList.getSelectedIndices();
                int[] actualSelection = new int[selection.length];
                for (int i = 0; i < selection.length; i++) {
                    actualSelection[i] = field.getDisplayListModel().getActualIndexAt(selection[i]);
                }

                Arrays.sort(actualSelection);

                for (int i = actualSelection.length - 1; i >= 0; i--) {
                    int index = actualSelection[i];
                    listModel.remove(index);
                }
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), JComponent.WHEN_FOCUSED);

        field.getDisplayListModel().addListDataListener(new ListDataListener() {
            public void intervalAdded(ListDataEvent e) {
                updateLabel(e);
            }

            public void intervalRemoved(ListDataEvent e) {
                updateLabel(e);
            }

            public void contentsChanged(ListDataEvent e) {
                updateLabel(e);
            }

            protected void updateLabel(ListDataEvent e) {
                if (e.getSource() instanceof ListModel) {
                    int count = ((ListModel) e.getSource()).getSize();
                    label.setText(count + " out of " + listModel.getSize() + " features");
                }
            }
        });
        listPanel.add(new JScrollPane(fSearchList));
        listPanel.add(label, BorderLayout.BEFORE_FIRST_LINE);

        fMainPanel.add(quickSearchPanel, BorderLayout.BEFORE_FIRST_LINE);
        fMainPanel.add(listPanel, BorderLayout.CENTER);
    }

} // End class GeneSearchList