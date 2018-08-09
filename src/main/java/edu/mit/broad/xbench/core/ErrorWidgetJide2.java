/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.core;

import com.jidesoft.dialog.ButtonPanel;
import com.jidesoft.dialog.StandardDialog;
import com.jidesoft.dialog.StandardDialogPane;
import com.jidesoft.swing.JideBoxLayout;
import edu.mit.broad.genome.*;
import xtools.api.param.MissingReqdParamException;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ErrorWidgetJide2 extends StandardDialog {

    private JComponent _detailsPanel;

    private Throwable[] fErrors;

    private String fError_msg;

    /**
     * Class constructor
     *
     * @param parent
     * @param title
     * @param t_opt
     * @param error_msg_opt
     * @throws HeadlessException
     */
    public ErrorWidgetJide2(final Frame parent,
                            final String title,
                            final Throwable t_opt,
                            final String error_msg_opt) throws HeadlessException {
        super(parent, _title(title));

        Throwable[] tss = null;
        if (t_opt != null) {
            tss = new Throwable[]{t_opt};
        }

        init(title, tss, error_msg_opt);
    }

    /**
     * Class constructor
     *
     * @param parent
     * @param errors
     * @throws HeadlessException
     */
    public ErrorWidgetJide2(final Frame parent,
                            final Errors errors) throws HeadlessException {
        super(parent, _title(errors.getName()));
        init(errors.getName(), errors.getErrors(), errors.getErrors(false));
    }

    public ErrorWidgetJide2(final Frame parent,
                            final String errMsg) throws HeadlessException {
        super(parent, _title(errMsg));
        init(errMsg, null, errMsg);
    }

    private static String _title(String title) {
        if (title != null && title.length() > 80) {
            return "Error: " + title.substring(0, 80);
        } else if (title != null) {
            return "Error: " + title;
        } else {
            return "Error";
        }
    }

    // Lots of logix about how errors are displayed is in here
    private void init(final String title,
                      final Throwable[] t_opt,
                      String error_msg_opt) {

        //log.debug(">> title: " + title);
        //log.debug(">> t_opt: " + t_opt);
        //log.debug(">> error_msg: " + error_msg_opt);

        boolean dontTruncate = false;

        if (error_msg_opt == null && t_opt != null && t_opt.length > 0 && t_opt[0] != null) {
            error_msg_opt = t_opt[0].getMessage();

            Throwable t = t_opt[0];
            Throwable tc = t.getCause();
            if (t instanceof MissingReqdParamException) {
                error_msg_opt = ((MissingReqdParamException) t).getMessageLongInHtml();
                dontTruncate = true;
            } else if (tc instanceof MissingReqdParamException) {
                error_msg_opt = ((MissingReqdParamException) tc).getMessageLongInHtml();
                dontTruncate = true;
            }

        }

        if (error_msg_opt != null && error_msg_opt.length() > 80 && !dontTruncate) {
            this.fError_msg = error_msg_opt.substring(0, 80) + " ...";
        } else {
            this.fError_msg = error_msg_opt;
        }

        this.fErrors = t_opt;
    }

    public JComponent createBannerPanel() {
        return null;
    }

    public JComponent createDetailsPanel() {

        final JTextArea textArea = new JTextArea();
        textArea.setColumns(80);
        textArea.setRows(20);
        textArea.setCaretPosition(0);

        StringBuffer buf = new StringBuffer("<Error Details>\n\n");
        buf.append("---- Full Error Message ----\n");

        if (fError_msg != null) {
            buf.append(fError_msg);
        } else {
            buf.append("na");
        }

        buf.append("\n\n");

        buf.append("---- Stack Trace ----\n");
        buf.append(TraceUtils.getAsString(fErrors));

        JLabel label = new JLabel("Details:");
        textArea.setText(buf.toString());
        textArea.setEditable(false);

        JPanel butPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton bCopy = new JButton("Copy");
        butPanel.add(bCopy);
        bCopy.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                StringSelection stringSelection = new StringSelection(textArea.getText());
                clipboard.setContents(stringSelection, stringSelection);
            }
        });


        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.add(new JScrollPane(textArea), BorderLayout.CENTER);
        panel.add(butPanel, BorderLayout.SOUTH);
        panel.add(label, BorderLayout.BEFORE_FIRST_LINE);
        label.setLabelFor(textArea);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        return panel;
    }

    protected StandardDialogPane createStandardDialogPane() {
        return new DefaultStandardDialogPane() {
            protected void layoutComponents(Component bannerPanel, Component contentPanel, ButtonPanel buttonPanel) {
                setLayout(new JideBoxLayout(this, BoxLayout.Y_AXIS));
                if (bannerPanel != null) {
                    add(bannerPanel);
                }
                if (contentPanel != null) {
                    add(contentPanel);
                }
                add(buttonPanel, JideBoxLayout.FIX);
                _detailsPanel = createDetailsPanel();
                add(_detailsPanel, JideBoxLayout.VARY);
                _detailsPanel.setVisible(false);
            }
        };
    }

    public JComponent createContentPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 40, 40, 40));

        JLabel label;

        if (fError_msg != null) {
            label = new JLabel(fError_msg);
        } else {
            label = new JLabel("There was an error. Click the details button for more information.");
        }

        label.setHorizontalAlignment(SwingConstants.CENTER);

        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    public ButtonPanel createButtonPanel() {
        ButtonPanel buttonPanel = new ButtonPanel();
        JButton closeButton = new JButton();
        JButton detailButton = new JButton();
        detailButton.setMnemonic('D');
        closeButton.setName(OK);
        buttonPanel.addButton(closeButton, ButtonPanel.AFFIRMATIVE_BUTTON);
        buttonPanel.addButton(detailButton, ButtonPanel.OTHER_BUTTON);

        closeButton.setAction(new AbstractAction("Close") {
            public void actionPerformed(ActionEvent e) {
                setDialogResult(RESULT_AFFIRMED);
                setVisible(false);
                dispose();
            }
        });


        if (fErrors != null) {
            Action help_action = null;
            for (int i = 0; i < fErrors.length; i++) {
                if (fErrors[i] instanceof StandardException) {
                    help_action = JarResources.createHelpAction((StandardException) fErrors[i]);
                }
            }

            if (help_action != null) {
                JButton helpButton = new JButton("Help");
                helpButton.setAction(help_action);
                buttonPanel.addButton(helpButton, ButtonPanel.HELP_BUTTON);
            }

        }

        detailButton.setAction(new AbstractAction("Details >>") {
            public void actionPerformed(ActionEvent e) {
                if (_detailsPanel.isVisible()) {
                    _detailsPanel.setVisible(false);
                    putValue(Action.NAME, "Details <<");
                    pack();
                } else {
                    _detailsPanel.setVisible(true);
                    putValue(Action.NAME, "<< Details");
                    pack();
                }
            }
        });

        setDefaultCancelAction(closeButton.getAction());
        setDefaultAction(closeButton.getAction());
        getRootPane().setDefaultButton(closeButton);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        //buttonPanel.setSizeContraint(ButtonPanel.NO_LESS_THAN); // since the checkbox is quite wide, we don't want all of them have the same size.
        return buttonPanel;
    }
}
