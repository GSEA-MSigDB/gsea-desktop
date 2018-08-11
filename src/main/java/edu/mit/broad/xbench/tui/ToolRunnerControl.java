/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.tui;

import edu.mit.broad.genome.JarResources;
import edu.mit.broad.genome.parsers.ParserFactory;
import edu.mit.broad.genome.swing.GuiHelper;
import edu.mit.broad.xbench.actions.ext.BrowserAction;
import edu.mit.broad.xbench.core.ApplicationDialog;
import edu.mit.broad.xbench.core.api.Application;
import foxtrot.Job;
import foxtrot.Task;
import foxtrot.Worker;
import org.apache.log4j.Logger;
import xtools.api.Tool;
import xtools.api.param.ParamSet;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

/**
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class ToolRunnerControl extends JPanel {


    private static final Logger klog = Logger.getLogger(ToolRunnerControl.class);
    private JButton bRun;
    private JButton bResetDefaults;
    private JButton bCmd;
    private JButton bHelp;
    private JButton bLastRunParams;
    private ToolRunnerControl.DisplayHook fHook;

    private JComponent fCompOpt;

    private ToolRunnerControl fInstance = this;

    /**
     * Class Constructor.
     *
     * @param hook
     */
    public ToolRunnerControl(final ToolRunnerControl.DisplayHook hook) {
        this(hook, null);
    }

    public ToolRunnerControl(final ToolRunnerControl.DisplayHook hook, JComponent compOpt) {
        if (hook == null) {
            throw new IllegalArgumentException("Param hook cannot be null");
        }

        this.fHook = hook;
        this.fCompOpt = compOpt;
        init();
    }


    private void init() {
        String run_str = "";
        String cmd_str = "";
        String reset_str = "";
        String last_str = "";
        Dimension dim = new Dimension(25, 25);
        Dimension cdim = new Dimension(25, 25);
        if (fCompOpt == null) {
            run_str = "Run";
            cmd_str = "Command";
            last_str = "Last";
            reset_str = "Reset";
            dim = new Dimension(75, 25);
            cdim = new Dimension(100, 25);
        }

        Dimension hdim = new Dimension(100, 25);

        //bResetDefaults = new JButton(reset_str, JarResources.getIcon("RestoreDefaults16.gif"));
        bResetDefaults = new JButton(reset_str, JarResources.getIcon("Reset16.gif"));
        bResetDefaults.setEnabled(false);
        bResetDefaults.setToolTipText("Reset to default parameters");
        bResetDefaults.setPreferredSize(dim);
        bResetDefaults.setSize(dim);


        bLastRunParams = new JButton(last_str, JarResources.getIcon("History16_v2.gif"));
        bLastRunParams.setEnabled(true);
        bLastRunParams.setToolTipText("Set to the previous run");
        bLastRunParams.setPreferredSize(dim);
        bLastRunParams.setSize(dim);

        bRun = GuiHelper.Button.createStartButton(run_str);
        bRun.setEnabled(false);
        bRun.setToolTipText("Execute the tool with specified parameters");
        bRun.setPreferredSize(dim);
        bRun.setSize(dim);

        bCmd = new JButton(cmd_str, JarResources.getIcon("CommandLine16_v2.gif"));
        bCmd.setEnabled(false);
        bCmd.setToolTipText("Commandline representation (for running from a unix terminal, dos window etc)");
        bCmd.setPreferredSize(cdim);
        bCmd.setSize(cdim);

        //bHelp = GuiHelper.Button.createHelpButton("Help");
        bHelp = new JButton("", JarResources.getIcon("Help16_v2.gif"));

        bHelp.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent evt) {
                klog.debug("launching help for: " + fHook.getCurrentTool());
                if (fHook.getCurrentTool() != null) {
                    String url = fHook.getCurrentTool().getHelpURL();
                    BrowserAction ba = new BrowserAction("Help", "Online documentation for this tool", GuiHelper.ICON_HELP16, url);
                    ba.actionPerformed(evt);
                }
            }
        });

        bHelp.setEnabled(true);
        bHelp.setToolTipText("Online HELP!! for this tool");
        bHelp.setPreferredSize(hdim);
        bHelp.setSize(dim);

        // Then actions
        bRun.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {

                try {
                    Worker.post(new Task() {

                        public Object run() throws Exception {

                            Tool tool = fHook.getCurrentTool();
                            ParamSet pset = fHook.getCurrentParamSet();

                            //klog.debug("Tool = " + tool + " pset: " + pset + "\n" + pset.toProperties());

                            if (fHook.isRecordToolRun()) {
                                try {
                                    Application.getToolManager().setLastToolName(tool);
                                } catch (Throwable t) {
                                    klog.warn("Unable to save last tool info", t);
                                }
                            }

                            if (tool != null) {
                                TaskManager.getInstance().run(tool, pset, Thread.NORM_PRIORITY);
                            }

                            return null;
                        }
                    }); // The exception stuff below for a nice popup dispaly rather than a error red tbing
                } catch (Throwable t) {
                    Application.getWindowManager().showError(t);
                }
            }
        });

        bResetDefaults.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {

                StringBuffer msg = new StringBuffer("Confirm reset parameters to defaults");

                boolean res = Application.getWindowManager().showConfirm("Confirm reset", msg.toString());

                if (res) {
                    fHook.resetParamSet();
                }
            }
        });

        bLastRunParams.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {

                Worker.post(new Job() {
                    public Object run() {

                        final ReportStub rs = Application.getToolManager().getLastReportStub(fHook.getCurrentTool().getName());

                        if (rs == null) {
                            Application.getWindowManager().showMessage("No history available for: " + fHook.getCurrentTool().getName());
                            return null;
                        }

                        boolean proc = Application.getWindowManager().showConfirm("Load data files from the last analysis: " + rs.getName());
                        if (!proc) {
                            return null;
                        }

                        // OK, go for it
                        try {
                            //final Tool tool = ToolDiscoverer.getInstance().createTool(fReport.getProducer());
                            final Properties source_params = rs.getReport(true).getParametersUsed();

                            //final Tool tool = fHook.getCurrentTool();
                            // lets use a clone

                            final Tool tool = TaskManager.createTool(fHook.getCurrentTool().getClass().getName());

                            Runnable runnable = ToolRunnerControl.createLoadToolTask(tool,
                                    rs.getName(), true, source_params, fInstance, false);

                            runnable.run();

                            Application.getWindowManager().showMessage("Data from the last run of this tool was automagically loaded in. They are now available as parameter options");

                            //t.setPriority(Thread.MIN_PRIORITY);
                            //t.start();

                        } catch (Throwable t) {
                            Application.getWindowManager().showError(t);
                        }

                        return null;
                    }
                });
            }
        });

        bCmd.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {

                JButton bCopy = new JButton("Copy", GuiHelper.ICON_COPY16);

                StringBuffer buf = new StringBuffer("java -Xmx512m ").append(fHook.getCurrentTool().getName()).append(' ');
                buf.append(fHook.getCurrentParamSet().getAsCommand(false, true, false).trim());

                final JTextArea ta = new JTextArea(buf.toString(), 5, 50);

                bCopy.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent ae) {

                        StringSelection stsel = new StringSelection(ta.getText());
                        Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
                        clip.setContents(stsel, stsel);
                    }
                });

                ApplicationDialog dd = new ApplicationDialog("Command Line for: " + fHook.getCurrentTool().getName(), new JScrollPane(ta));
                dd.setButtons(new JButton[]{bCopy});
                dd.show();
            }
        });


        this.setLayout(new BorderLayout());
        JPanel lhsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lhsPanel.add(bHelp, BorderLayout.WEST);
        this.add(lhsPanel, BorderLayout.WEST);

        set(lhsPanel);
        set(bCmd);
        set(bHelp);
        set(bLastRunParams);
        set(bResetDefaults);
        set(bRun);
        
        JPanel rhsPanel;

        if (fCompOpt == null) {
            rhsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

            rhsPanel.add(bResetDefaults);
            rhsPanel.add(bLastRunParams);
            rhsPanel.add(bCmd);

            rhsPanel.add(bRun);
        } else {
            rhsPanel = new JPanel(new FlowLayout());
            rhsPanel.add(fCompOpt);
            rhsPanel.add(bResetDefaults);
            rhsPanel.add(bLastRunParams);
            rhsPanel.add(bRun);
            rhsPanel.add(bCmd);
        }

        set(rhsPanel);
        
        JScrollPane sp = new JScrollPane(rhsPanel, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sp.setBorder(BorderFactory.createEmptyBorder());

        this.add(sp, BorderLayout.CENTER);
    }


    private void set(JComponent comp) {
        comp.setBackground(GuiHelper.COLOR_VERY_LIGHT_GRAY);
        if (comp instanceof JPanel) {
            comp.setBorder(BorderFactory.createEmptyBorder());
        }

        if (comp instanceof JButton) {
            GuiHelper.Button.addMouseOverRollOverAction((JButton) comp);
        }
    }

    public void setEnabledControls(boolean enabled) {
        bRun.setEnabled(enabled);
        bCmd.setEnabled(enabled);
        bResetDefaults.setEnabled(true); // dont disable this ever!
    }

    /**
     * Interface for this class to call back to set/get currently displayed
     * Tool info.
     *
     * @author Aravind Subramanian
     * @version %I%, %G%
     */
    public interface DisplayHook {

        public Tool getCurrentTool();

        public ParamSet getCurrentParamSet();

        public void resetParamSet();

        public boolean isRecordToolRun();

    } // End innerclass DisplayHook


    public static Runnable createLoadToolTask(final Tool fill_this_tool,
                                              final String rptName,
                                              final boolean loadFiles,
                                              final Properties source_params,
                                              final Component parentComponent,
                                              final boolean launchANewToolWindow) {

        return new Runnable() {

            public void run() {

                try {

                    //log.debug("tool name: " + tool.getName());
                    // imp have to fill and pass the tool's param set
                    // we have to manually check for rhs files as the params silently
                    // suppress any file not found kind of error
                    ParamSet.FoundMissingFile fmf = fill_this_tool.getParamSet().fileCheckingFill(source_params);
                    //log.debug(tool.getParamSet().toProperties());
                    if (fmf.missingFiles.length != 0) {
                        if (showMissingDialog(fmf.missingFiles) == false) {
                            // do nothing
                        }
                    }

                    StringBuffer errs = new StringBuffer("<html><body>There were parsing errors<pre>");
                    boolean atleastoneerr = false;
                    if (loadFiles) {
                        for (int i = 0; i < fmf.foundFiles.length; i++) {
                            try {
                                if (fmf.foundFiles[i].isFile()) {
                                    String path = fmf.foundFiles[i].getPath();
                                    klog.debug("Trying to parse: " + path + " for param: " + fmf.foundFilesParamNames[i]);
                                    ProgressMonitorInputStream pis = new ProgressMonitorInputStream(parentComponent, "Loading: " + path, new FileInputStream(fmf.foundFiles[i]));
                                    ParserFactory.read(path, pis);
                                }
                            } catch (Throwable t) {
                                klog.debug("Parsing error for file from param: " + fmf.foundFilesParamNames[i] + " file >" + fmf.foundFiles[i].getPath(), t);
                                atleastoneerr = true;
                                errs.append(t.getMessage()).append("<br>");
                            }
                        }
                        errs.append("</pre></body></html>");
                    }

                    if (atleastoneerr) {
                        Application.getWindowManager().showMessage(errs.toString());
                    }

                    if (launchANewToolWindow) {
                        SingleToolLauncherAction a = new SingleToolLauncherAction(fill_this_tool, fill_this_tool.getParamSet(), rptName);
                        a.createTask(null).run();
                        Application.getWindowManager().showMessage("Created a new ToolRunner with parameters from the earlier run. Data files (when found) were automagically imported");
                    }


                } catch (Throwable t) {
                    Application.getWindowManager().showError(t);
                }
            }
        };
    }

    private static boolean showMissingDialog(final File[] files) {
        StringBuffer buf = new StringBuffer("<html><body>The following file(s) were not found on the local file system<br>");
        buf.append("<pre>");
        for (int i = 0; i < files.length; i++) {
            buf.append(files[i]).append("<br>");
        }

        buf.append("</pre></body></html>");

        return Application.getWindowManager().showConfirm("Some Files Missing", buf.toString());
    }

}    // End ToolRunnerControl
