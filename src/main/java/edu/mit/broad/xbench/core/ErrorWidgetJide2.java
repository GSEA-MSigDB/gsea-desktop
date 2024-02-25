/*
 * Copyright (c) 2003-2024 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.xbench.core;

import xtools.api.param.MissingReqdParamException;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

import edu.mit.broad.genome.Errors;
import edu.mit.broad.genome.JarResources;
import edu.mit.broad.genome.StandardException;
import edu.mit.broad.genome.TraceUtils;

public class ErrorWidgetJide2 extends JDialog {
    private JComponent _detailsPanel;
    private Throwable[] fErrors;
    private String fError_msg;

    public ErrorWidgetJide2(final Frame parent, final String title, final Throwable t_opt,
            final String error_msg_opt) throws HeadlessException {
        super(parent, _title(title));

        Throwable[] tss = null;
        if (t_opt != null) { tss = new Throwable[]{t_opt}; }

        init(title, tss, error_msg_opt);
    }

    public ErrorWidgetJide2(final Frame parent, final Errors errors) throws HeadlessException {
        super(parent, _title(errors.getName()));
        init(errors.getName(), errors.getErrors(), errors.getErrors(false));
    }

    public ErrorWidgetJide2(final Frame parent, final String errMsg) throws HeadlessException {
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

    // Lots of logic about how errors are displayed is in here
    private void init(final String title, final Throwable[] t_opt, String error_msg_opt) {
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

        if (error_msg_opt != null && error_msg_opt.length() > 110 && !dontTruncate) {
            this.fError_msg = error_msg_opt.substring(0, 110) + " ...";
        } else {
            this.fError_msg = error_msg_opt;
        }

        this.fErrors = t_opt;
        
        this.setModal(true);
        this.setLayout(new BorderLayout());
        add(createContentPanel(), BorderLayout.NORTH);
        add(createButtonPanel(), BorderLayout.CENTER);
        _detailsPanel = createDetailsPanel();
        add(_detailsPanel, BorderLayout.SOUTH);
        _detailsPanel.setVisible(false);
        this.setAlwaysOnTop(true);
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

    public JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new BorderLayout());
        JPanel closeDetailPanel = new JPanel(new FlowLayout());
        JButton closeButton = new JButton();
        JButton detailButton = new JButton();
        detailButton.setMnemonic('D');
        closeButton.setName("OK");
        closeDetailPanel.add(closeButton);
        closeDetailPanel.add(detailButton);
        buttonPanel.add(closeDetailPanel, BorderLayout.EAST);

        closeButton.setAction(new AbstractAction("Close") {
            public void actionPerformed(ActionEvent e) {
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
                buttonPanel.add(helpButton, BorderLayout.WEST);
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

        getRootPane().setDefaultButton(closeButton);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return buttonPanel;
    }
}
