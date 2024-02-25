/*
 * Copyright (c) 2003-2024 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.xbench.core.api;

import edu.mit.broad.genome.Errors;
import edu.mit.broad.genome.swing.GuiHelper;
import edu.mit.broad.xbench.actions.ext.BrowserAction;

import xapps.gsea.GseaWebResources;
import xtools.api.param.Validator;

import javax.swing.*;

import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class DialogDescriptor {
    private static final Logger klog = LoggerFactory.getLogger(DialogDescriptor.class);
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
    private Validator warningValidator = null; 
    private Validator errorValidator = null;

    public DialogDescriptor(final String title, final Component inputComp, final Action help_action_opt) {
        this.fHelpAction_opt = help_action_opt;
        jbInit(title, inputComp);
    }

    public DialogDescriptor(final String title, final Component inputComp, final Action help_action_opt,
            final Action info_action_opt, boolean showLicenseButton) {
        this.showLicenseButton = showLicenseButton;
        this.fHelpAction_opt = help_action_opt;
        this.fInfoAction_opt = info_action_opt;
        jbInit(title, inputComp);
    }

    // must call if the paramless form of the constructor is used
    private void jbInit(final String title, final Component inputComp) {
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
            fMainPanel.setPreferredSize(fDisplayWider ? DialogDescriptor.DD_SIZE_WIDER : DialogDescriptor.DD_SIZE);
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
     * Simply closes the dialog and returns void when double clicked (the OK is implied)
     */
    // TODO: examine JList type safety
    public void enableDoubleClickableJList(final JList jl) {
        jl.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent me) {
                if (jl.isSelectionEmpty()) { return; }

                if (me.getClickCount() == 2) {
                    me.consume();
                    if (!performValidationBeforeOK()) { return; }
                    fChoosenOption = DialogDescriptor.OK_OPTION;
                    if (fDialog != null) {
                        fDialog.dispose();    // this somehow auto calls the show method in joptionpane
                    }
                }
            }
        });
    }

    public void setDisplayWider() {
        fDisplayWider = true;
        if (fMainPanel != null) { fMainPanel.setPreferredSize(DialogDescriptor.DD_SIZE_WIDER); }
    }
    
    public void setWarningValidator(Validator warningValidator) {
        this.warningValidator = warningValidator;
    }
    
    public void setErrorValidator(Validator errorValidator) {
        this.errorValidator = errorValidator;
    }
    
    public void setButtons(final JButton[] boptions) {
        this.fCustomButtons = boptions;
    }

    public void setOnlyShowCloseOption() {
        this.fCustomButtons = new JButton[]{};
        bCancel.setText("Close");
        this.fAddCancelButton = true;
    }

    private boolean performValidationBeforeOK() {
        if (errorValidator != null && !errorValidator.isValid()) {
            Errors errors = errorValidator.buildValidationFailedErrors();
            klog.error(errors.getName());
            String errorMsg = "";
            String sep = "";
            for (String err : errors.getErrorsAsStrings()) {
                errorMsg += sep + err;
                sep = SystemUtils.LINE_SEPARATOR;
            }
            Application.getWindowManager().showMessage(errors.getName(), errorMsg);
            return false;
        } else if (warningValidator != null && !warningValidator.isValid()) {
            Errors warnings = warningValidator.buildValidationFailedErrors();
            String warningMsg = "";
            String sep = "";
            for (String w : warnings.getErrorsAsStrings()) {
                warningMsg += sep + w;
                sep = SystemUtils.LINE_SEPARATOR;
            }
            
            boolean confirm = Application.getWindowManager().showConfirm(warnings.getName(), warningMsg);
            if (!confirm) { return false; }
            klog.warn(warnings.getName());
        }
        return true;
    }

    private JButton bOk;
    private JPanel buttonPanel;
    /**
     * class constant for cancel actions
     */
    public static final int CANCEL_OPTION = 2;
    /**
     * class constant for ok actions
     */
    public static final int OK_OPTION = 0;
    private static final Dimension DD_SIZE_WIDER = new Dimension(800, 400);
    private static final Dimension DD_SIZE = new Dimension(550, 400);

    private JPanel createButtonPanel() {
        this.buttonPanel = new JPanel();
        buttonPanel.setLayout(new BorderLayout());
        if (fCustomButtons != null) {
            for (int i = 0; i < fCustomButtons.length; i++) {
                buttonPanel.add(fCustomButtons[i]);
            }

            if (fAddCancelButton) {
                bCancel.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        fChoosenOption = DialogDescriptor.CANCEL_OPTION;
                        fDialog.setVisible(false);
                        fDialog.dispose();
                    }
                });
                buttonPanel.add(bCancel, BorderLayout.EAST);
            }
        } else { // generic options
            JPanel okCancelHolder = new JPanel();
            BoxLayout okCancelLayout = new BoxLayout(okCancelHolder, BoxLayout.X_AXIS);
            bOk = new JButton("OK");
            okCancelHolder.add(bOk);
            okCancelHolder.add(bCancel);
            buttonPanel.add(okCancelHolder, BorderLayout.EAST);

            if (showLicenseButton) {
                JButton bLicense = new JButton("MSigDB License", GuiHelper.ICON_HELP16);
                bLicense.addActionListener(new BrowserAction("MSigDB License", "MSigDB License Terms",
                        GuiHelper.ICON_HELP16, GseaWebResources.getGseaBaseURL() + "/license_terms_list.jsp"));
                okCancelHolder.add(bLicense);
            }

            JPanel infoHelpHolder = new JPanel();
            BoxLayout infoHelpLayout = new BoxLayout(infoHelpHolder, BoxLayout.X_AXIS);
            buttonPanel.add(infoHelpHolder, BorderLayout.WEST);
            if (fHelpAction_opt != null) {
                JButton bHelp = new JButton("Help");
                bHelp.setAction(fHelpAction_opt);
                infoHelpHolder.add(bHelp);
            }

            if (fInfoAction_opt != null) {
                JButton bInfo = new JButton("Info");
                bInfo.setAction(fInfoAction_opt);
                infoHelpHolder.add(bInfo);
            }

            bOk.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (!performValidationBeforeOK()) { return; }
                    
                    fChoosenOption = DialogDescriptor.OK_OPTION;
                    fDialog.setVisible(false);
                    fDialog.dispose();
                }
            });

            bCancel.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    fChoosenOption = DialogDescriptor.CANCEL_OPTION;
                    fDialog.setVisible(false);
                    fDialog.dispose();
                }
            });
        }

        return buttonPanel;
    }
}
