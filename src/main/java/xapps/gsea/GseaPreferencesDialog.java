/*
 * Copyright (c) 2003-2024 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xapps.gsea;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import edu.mit.broad.genome.JarResources;
import edu.mit.broad.xbench.core.api.Application;
import edu.mit.broad.xbench.prefs.XPreferencesFactory;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.event.ActionEvent;

public class GseaPreferencesDialog extends JDialog {
    public GseaPreferencesDialog(final Frame owner, final String title) throws HeadlessException {
        super(owner, title);
        this.setModal(true);
        BorderLayout borderLayout = new BorderLayout(15, 15);
        this.setLayout(borderLayout);
        JPanel panel = new OptionsPanel();
        this.add(panel, BorderLayout.CENTER);
        
        JPanel buttonPanel = createButtonPanel();
        this.add(buttonPanel, BorderLayout.SOUTH);
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BorderLayout(15, 15));
        JButton helpButton = JarResources.createHelpButton("#Prefs-Window");
        helpButton.setMargin(new Insets(0, 30, 0, 30));
        JPanel helpPanel = new JPanel(new FlowLayout()); 
        helpPanel.add(helpButton);
        buttonPanel.add(helpPanel, BorderLayout.WEST);
        
        AbstractAction okAction = new AbstractAction(UIManager.getString("OptionPane.okButtonText")) {
            public void actionPerformed(ActionEvent e) {
                savePreferences();
                setVisible(false);
                Application.getWindowManager().showMessage("Saved preferences (many preferences need a restart of GSEA)");
                dispose();
            }
        };
        JButton okButton = new JButton("  OK  ");
        okButton.setAction(okAction);
        okButton.setMargin(new Insets(0, 30, 0, 30));

        AbstractAction cancelAction = new AbstractAction(UIManager.getString("OptionPane.cancelButtonText")) {
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
                dispose();
            }
        };
        JPanel okCancelPanel = new JPanel(new FlowLayout());
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setAction(cancelAction);
        cancelButton.setMargin(new Insets(0, 30, 0, 30));
        okCancelPanel.add(cancelButton);
        okCancelPanel.add(okButton);
        buttonPanel.add(okCancelPanel, BorderLayout.EAST);

        return buttonPanel;
    }

    // @note hardcoded what prefs are used for GSEA
    private void savePreferences() {
        try {
            XPreferencesFactory.kAskBeforeAppShutdown.setValueOfPref2SelectionComponentValue();
            XPreferencesFactory.kOnlineMode.setValueOfPref2SelectionComponentValue();
            XPreferencesFactory.kCytoscapeRESTPort.setValueOfPref2SelectionComponentValue();
            XPreferencesFactory.kDefaultReportsOutputDir.setValueOfPref2SelectionComponentValue();
            XPreferencesFactory.kMakeGseaUpdateCheck.setValueOfPref2SelectionComponentValue();

            XPreferencesFactory.kBiasedVar.setValueOfPref2SelectionComponentValue();
            XPreferencesFactory.kMedian.setValueOfPref2SelectionComponentValue();
            XPreferencesFactory.kFixLowVar.setValueOfPref2SelectionComponentValue();

            XPreferencesFactory.save();
        } catch (Exception e) {
            Application.getWindowManager().showError("Trouble saving preferences", e);
        }
    }

    static class OptionsPanel extends JPanel {
        public OptionsPanel() {
            setLayout(new BorderLayout());
            add(createPanel(), BorderLayout.CENTER);
        }

        private JPanel createPathsPanel() {
            final String str = "275dlu";
            int rowCnt = 3;
            final StringBuffer rowStr = _createRowStr(5);
            final FormLayout layout = new FormLayout(str, rowStr.toString());
            PanelBuilder builder = new PanelBuilder(layout);
            builder.setDefaultDialogBorder();
            final CellConstraints cc = new CellConstraints();
            builder.add(new JLabel("Cytoscape REST port (for Enrichment Map Visualization)"), cc.xy(1, rowCnt));
            rowCnt += 2; // because the spaces also count as a row
            builder.add(XPreferencesFactory.kCytoscapeRESTPort.getSelectionComponent().getComponent(), cc.xy(1, rowCnt));
            JPanel panel = builder.getPanel();
            panel.setBorder(createRoundCornerBorder(" Program settings "));
            return panel;
        }

        private JPanel createOutputPanel() {
            final String str = "75dlu,      4dlu,        200dlu"; // columns
            int rowCnt = 3;
            final StringBuffer rowStr = _createRowStr(1);
            final FormLayout layout = new FormLayout(str, rowStr.toString());
            PanelBuilder builder = new PanelBuilder(layout);
            builder.setDefaultDialogBorder();
            final CellConstraints cc = new CellConstraints();
            builder.add(new JLabel("Default output folder"), cc.xy(1, rowCnt));
            builder.add(XPreferencesFactory.kDefaultReportsOutputDir.getSelectionComponent().getComponent(), cc.xy(3, rowCnt));
            JPanel panel = builder.getPanel();
            panel.setBorder(createRoundCornerBorder(" Report settings "));
            return panel;
        }

        private JPanel createAppPreferencesPanel() {
            final String str = "180dlu,      4dlu,        10dlu"; // columns
            //            // 1(label)    2 (spacer)   3(field)
            int rowCnt = 5;
            final StringBuffer rowStr = _createRowStr(5);
            final FormLayout layout = new FormLayout(str, rowStr.toString());
            PanelBuilder builder = new PanelBuilder(layout);
            builder.setDefaultDialogBorder();
            final CellConstraints cc = new CellConstraints();
            builder.add(new JLabel("Prompt before closing application"), cc.xy(1, rowCnt));
            builder.add(XPreferencesFactory.kAskBeforeAppShutdown.getSelectionComponent().getComponent(), cc.xy(3, rowCnt));
            rowCnt += 2; // because the spaces also count as a row
            builder.add(new JLabel("Connect over the Internet"), cc.xy(1, rowCnt));
            builder.add(XPreferencesFactory.kOnlineMode.getSelectionComponent().getComponent(), cc.xy(3, rowCnt));
            rowCnt += 2;
            builder.add(new JLabel("Check for new GSEA version on startup"), cc.xy(1, rowCnt));
            builder.add(XPreferencesFactory.kMakeGseaUpdateCheck.getSelectionComponent().getComponent(), cc.xy(3, rowCnt));
            JPanel panel = builder.getPanel();
            panel.setBorder(createRoundCornerBorder(" Application preferences "));
            return panel;
        }

        private JPanel createAlgPanel() {
            //final String str = "75dlu,      4dlu,        125dlu,   4dlu,  40dlu"; // columns
            final String str = "200dlu,      4dlu,        50dlu"; // columns
            //            // 1(label)    2 (spacer)   3(field)
            int rowCnt = 3;
            final StringBuffer rowStr = _createRowStr(5);
            final FormLayout layout = new FormLayout(str, rowStr.toString());
            PanelBuilder builder = new PanelBuilder(layout);
            builder.setDefaultDialogBorder();
            final CellConstraints cc = new CellConstraints();
            builder.add(new JLabel(XPreferencesFactory.kMedian.getName()), cc.xy(1, rowCnt));
            builder.add(XPreferencesFactory.kMedian.getSelectionComponent().getComponent(), cc.xy(3, rowCnt));
            rowCnt += 2; // because the spaces also count as a row
            builder.add(new JLabel(XPreferencesFactory.kFixLowVar.getName()), cc.xy(1, rowCnt));
            builder.add(XPreferencesFactory.kFixLowVar.getSelectionComponent().getComponent(), cc.xy(3, rowCnt));
            rowCnt += 2; // because the spaces also count as a row
            builder.add(new JLabel(XPreferencesFactory.kBiasedVar.getName()), cc.xy(1, rowCnt));
            builder.add(XPreferencesFactory.kBiasedVar.getSelectionComponent().getComponent(), cc.xy(3, rowCnt));
            JPanel panel = builder.getPanel();
            panel.setBorder(createRoundCornerBorder("Algorithm: These are 'Defaults' "));
            return panel;
        }

        private JPanel createPanel() {
            // home page panel
            JPanel outPanel = createOutputPanel();
            JPanel pathsPanel = createPathsPanel();
            JPanel algPanel = createAlgPanel();

            // app prefs
            JPanel windowsAndTabsPanel = createAppPreferencesPanel();
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
            panel.add(Box.createVerticalStrut(20));
            panel.add(outPanel);
            panel.add(Box.createVerticalStrut(6));
            panel.add(pathsPanel);
            panel.add(Box.createVerticalStrut(6));
            panel.add(Box.createVerticalStrut(6));
            panel.add(windowsAndTabsPanel);
            panel.add(Box.createVerticalStrut(6));
            panel.add(Box.createVerticalStrut(6));
            panel.add(algPanel);
            panel.add(Box.createVerticalStrut(30));
            return panel;
        }
    }

    private static StringBuffer _createRowStr(final int num) {
        StringBuffer rowStr = new StringBuffer();
        rowStr.append("pref, 5dlu,"); // for the spacer
        for (int i = 0; i < num; i++) {
            rowStr.append("pref, 3dlu");
            if (num != i - 1) {
                rowStr.append(",");
            }
        }
        return rowStr;
    }

    private static CompoundBorder createRoundCornerBorder(String title) {
        return BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15), 
                BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1, true), title), BorderFactory.createEmptyBorder(0, 6, 4, 6)));
    }
}
