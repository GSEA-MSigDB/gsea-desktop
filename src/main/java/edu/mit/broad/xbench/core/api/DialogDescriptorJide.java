/*
 * Copyright (c) 2003-2022 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.xbench.core.api;

import com.jidesoft.dialog.ButtonPanel;

import edu.mit.broad.genome.swing.GuiHelper;
import edu.mit.broad.xbench.actions.ext.BrowserAction;

import xapps.gsea.GseaWebResources;

import javax.swing.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * has some magic to make jlists double clickable
 *
 * @author Aravind Subramanian, David Eby
 */
public class DialogDescriptorJide implements DialogDescriptor {
    private JPanel fMainPanel;
    private int fChoosenOption = -1;
    private boolean fModal = true;
    private JDialog fDialog;
    private String fTitle;
    private boolean showLicenseButton = false;
    private Action fHelpAction_opt;
    private Action fInfoAction_opt;
    private JButton bCancel;
    private JButton[] fCustomButtons;
    private boolean fAddCancelButton = true;
    private boolean fDisplayWider = false;

    public DialogDescriptorJide(final String title, final Component inputComp, final Action help_action_opt) {
        this.fHelpAction_opt = help_action_opt;
        jbInit(title, inputComp);
    }

    public DialogDescriptorJide(final String title, final Component inputComp, final Action help_action_opt, final Action info_action_opt, boolean showLicenseButton) {
        this.showLicenseButton = showLicenseButton;
        this.fHelpAction_opt = help_action_opt;
        this.fInfoAction_opt = info_action_opt;
        jbInit(title, inputComp);
    }

    protected DialogDescriptorJide() { }

    // must call if the paramless form of the constructor is used
    protected void jbInit(final String title, final Component inputComp) {
        this.fTitle = title;
        this.fMainPanel = new JPanel(new BorderLayout(10, 10));
        fMainPanel.add(inputComp, BorderLayout.CENTER);
        this.bCancel = new JButton("Cancel");
    }

    JPanel botPanel;
    protected void _jbInit_jit_buttons() {
        if (botPanel == null) {
            botPanel = new JPanel(new BorderLayout(3, 3));
            botPanel.add(createButtonPanel(), BorderLayout.CENTER);
            botPanel.add(Box.createVerticalStrut(10), BorderLayout.SOUTH); // just for a border
            fMainPanel.add(botPanel, BorderLayout.AFTER_LAST_LINE);
            fMainPanel.setPreferredSize(fDisplayWider ? DD_SIZE_WIDER : DD_SIZE);
        }
    }

    /**
     * If modal, a regular jdialog is always used
     * If not modal:
     * 1) if application (desktop) is available then the dialog is shown in a jif
     * (why? ->else the dial dissapears behind the desktop)
     * 2) If app is null, then the dialog iks shows in a jdialog
     * (same as modal, except jdialog is not modal)
     * <p/>
     * Known issue -> This does not work when launched from within JUNIT -> it always
     * makes a jdialog
     *
     * @return One of OK or CANCEL
     */
    public int show() {
        _jbInit_jit_buttons();
        this.fChoosenOption = DialogDescriptor.CANCEL_OPTION; // default
        fDialog = new JDialog(Application.getWindowManager().getRootFrame(), fTitle, fModal);
        fDialog.setModal(fModal);
        //fDialog.setLayout(new BorderLayout(15, 15));
        fMainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        fDialog.setContentPane(fMainPanel);
        fDialog.setResizable(true);
        fDialog.pack();
        GuiHelper.centerComponent(fDialog);
        fDialog.setVisible(true);
        fDialog.requestFocus();
        return fChoosenOption;
    }

    /**
     * only makes sense if the specified jlist is a component that is
     * displayed in the dialog descriptor window
     * Double click / enter on the jlist == a OK button click
     * Simpley closes the dialog and returns void when double clicked (the OK is implied)
     */
    public void enableDoubleClickableJList(final JList jl) {
        jl.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent me) {
                Object objs[] = jl.getSelectedValues();
                if (objs == null) {
                    return;
                }

                if (me.getClickCount() == 2) {
                    me.consume();
                    fChoosenOption = OK_OPTION;
                    if (fDialog != null) {
                        fDialog.dispose();    // this somehow auto calls the show method in joptionpane
                    }
                }
            }
        });
    }

    public void setDisplayWider() {
        fDisplayWider = true;
        if (fMainPanel != null) { fMainPanel.setPreferredSize(DD_SIZE_WIDER); }
    }
    
    public void setButtons(final JButton[] boptions) {
        this.fCustomButtons = boptions;
    }

    public void setOnlyShowCloseOption() {
        this.fCustomButtons = new JButton[]{};
        bCancel.setText("Close");
        this.fAddCancelButton = true;
    }

    private JButton bOk;
    private ButtonPanel buttonPanel;

    private ButtonPanel createButtonPanel() {
        this.buttonPanel = new ButtonPanel();
        if (fCustomButtons != null) {
            for (int i = 0; i < fCustomButtons.length; i++) {
                buttonPanel.addButton(fCustomButtons[i]);
            }

            if (fAddCancelButton) {
                bCancel.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        fChoosenOption = CANCEL_OPTION;
                        fDialog.setVisible(false);
                        fDialog.dispose();
                    }
                });
                buttonPanel.addButton(bCancel, ButtonPanel.CANCEL_BUTTON);
            }
        } else { // generic options
            bOk = new JButton("OK");
            buttonPanel.addButton(bOk, ButtonPanel.AFFIRMATIVE_BUTTON);
            buttonPanel.addButton(bCancel, ButtonPanel.CANCEL_BUTTON);

            if (fHelpAction_opt != null) {
                JButton bHelp = new JButton("Help");
                bHelp.setAction(fHelpAction_opt);
                buttonPanel.addButton(bHelp, ButtonPanel.HELP_BUTTON);
            }

            if (fInfoAction_opt != null) {
                JButton bInfo = new JButton("Info");
                bInfo.setAction(fInfoAction_opt);
                buttonPanel.addButton(bInfo, ButtonPanel.HELP_BUTTON);
            }

            if (showLicenseButton) {
                JButton bLicense = new JButton("MSigDB License", GuiHelper.ICON_HELP16);
                bLicense.addActionListener(new BrowserAction("MSigDB License", "MSigDB License Terms",
                        GuiHelper.ICON_HELP16, GseaWebResources.getGseaBaseURL() + "/license_terms_list.jsp"));
                buttonPanel.addButton(bLicense, ButtonPanel.OTHER_BUTTON);
            }

            bOk.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    fChoosenOption = OK_OPTION;
                    fDialog.setVisible(false);
                    fDialog.dispose();
                }
            });

            bCancel.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    fChoosenOption = CANCEL_OPTION;
                    fDialog.setVisible(false);
                    fDialog.dispose();
                }
            });
        }

        return buttonPanel;
    }
}
