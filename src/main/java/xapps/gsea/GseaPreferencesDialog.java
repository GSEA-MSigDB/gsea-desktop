/*
 * Copyright (c) 2003-2020 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xapps.gsea;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jidesoft.dialog.*;
import com.jidesoft.swing.PartialLineBorder;
import edu.mit.broad.genome.JarResources;
import edu.mit.broad.xbench.core.api.Application;
import edu.mit.broad.xbench.prefs.XPreferencesFactory;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.awt.event.ActionEvent;

public class GseaPreferencesDialog extends MultiplePageDialog {
    /**
     * Class constructor
     *
     * @param owner
     * @param title
     * @throws HeadlessException
     */
    public GseaPreferencesDialog(final Frame owner, final String title) throws HeadlessException {
        super(owner, title);

        super.setStyle(MultiplePageDialog.ICON_STYLE);
        PageList model = new PageList();

        // setup model
        AbstractDialogPage panel1 = new FirebirdOptionPage_general("General", JarResources.getImageIcon("prefs_general.png"));
        AbstractDialogPage panel2 = new FirebirdOptionPage_alg("Algorithms", JarResources.getImageIcon("PreferencesSystemBig.png"));

        model.append(panel1);
        model.append(panel2);


        super.setPageList(model);
    }

    protected void initComponents() {
        super.initComponents();
        getContentPanel().setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        getIndexPanel().setBackground(Color.white);
        getButtonPanel().setBorder(BorderFactory.createEmptyBorder(20, 10, 10, 10));
        getPagesPanel().setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
    }

    public ButtonPanel createButtonPanel() {
        ButtonPanel buttonPanel = super.createButtonPanel();
        AbstractAction okAction = new AbstractAction(UIManager.getString("OptionPane.okButtonText")) {
            public void actionPerformed(ActionEvent e) {
                setDialogResult(RESULT_AFFIRMED);
                savePreferences();
                setVisible(false);
                dispose();
            }
        };

        AbstractAction applyAction = new AbstractAction("Apply") {
            public void actionPerformed(ActionEvent e) {
                setDialogResult(RESULT_AFFIRMED);
                savePreferences();
                Application.getWindowManager().showMessage("Saved preferences (many preferences need a restart of GSEA)");
            }
        };

        AbstractAction cancelAction = new AbstractAction(UIManager.getString("OptionPane.cancelButtonText")) {
            public void actionPerformed(ActionEvent e) {
                setDialogResult(RESULT_CANCELLED);
                setVisible(false);
                dispose();
            }
        };

        ((JButton) buttonPanel.getButtonByName(ButtonNames.OK)).setAction(okAction);
        ((JButton) buttonPanel.getButtonByName(ButtonNames.APPLY)).setAction(applyAction);
        ((JButton) buttonPanel.getButtonByName(ButtonNames.CANCEL)).setAction(cancelAction);

        //but.setText("Close");

        buttonPanel.addButton(JarResources.createHelpButton("gsea_preferences_widget"), ButtonNames.HELP);
        setDefaultCancelAction(cancelAction);
        setDefaultAction(okAction);
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

    public Dimension getPreferredSize() {
        return new Dimension(750, 540);
    }

    static class FirebirdOptionPage extends AbstractDialogPage {
        public FirebirdOptionPage(String name, Icon icon) {
            super(name, icon);
        }

        public void lazyInitialize() {
            initComponents();
        }

        public void initComponents() {
            BannerPanel headerPanel = new BannerPanel(getTitle(), null);
            headerPanel.setForeground(Color.WHITE);
            headerPanel.setBackground(new Color(10, 36, 106));
            headerPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED, Color.white, Color.darkGray, Color.darkGray, Color.gray));

            setLayout(new BorderLayout());
            add(headerPanel, BorderLayout.BEFORE_FIRST_LINE);
            add(new JLabel("This is just a demo. \"" + getFullTitle() + "\" page is not implemented yet.", JLabel.CENTER), BorderLayout.CENTER);
        }
    }


    static class FirebirdOptionPage_general extends FirebirdOptionPage {
        public FirebirdOptionPage_general(String name, Icon icon) {
            super(name, icon);
        }

        public void initComponents() {
            super.initComponents();
            add(createPanel(this), BorderLayout.CENTER);
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

        private JPanel createPanel(final AbstractDialogPage page) {
            // home page panel
            JPanel outPanel = createOutputPanel();

            JPanel pathsPanel = createPathsPanel();

            // app prefs
            JPanel windowsAndTabsPanel = createAppPreferencesPanel();

            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

            panel.add(Box.createVerticalStrut(6));
            panel.add(outPanel);
            panel.add(Box.createVerticalStrut(6));
            panel.add(pathsPanel);
            panel.add(Box.createVerticalStrut(6));
            //panel.add(emailPanel);
            panel.add(Box.createVerticalStrut(6));
            panel.add(windowsAndTabsPanel);
            panel.add(Box.createVerticalStrut(300));
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

    static class FirebirdOptionPage_alg extends FirebirdOptionPage {
        public FirebirdOptionPage_alg(String name, Icon icon) {
            super(name, icon);
        }

        public void initComponents() {
            super.initComponents();
            add(createPanel(this), BorderLayout.CENTER);
        }

        private JPanel createPanel(final AbstractDialogPage page) {
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

            JPanel bpanel = builder.getPanel();
            bpanel.setBorder(createRoundCornerBorder(" These are 'Defaults' "));

            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
            panel.add(Box.createVerticalStrut(6));
            panel.add(bpanel);
            panel.add(Box.createVerticalStrut(6));
            return panel;
        }
    }

    private static CompoundBorder createRoundCornerBorder(String title) {
        return BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(
                new PartialLineBorder(Color.gray, 1, true), title), BorderFactory.createEmptyBorder(0, 6, 4, 6));
    }
}