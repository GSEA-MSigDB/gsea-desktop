/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.xbench.tui;

import edu.mit.broad.genome.JarResources;
import edu.mit.broad.xbench.core.Widget;
import org.apache.log4j.Logger;
import xtools.api.Tool;
import xtools.api.param.ParamSet;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.StringTokenizer;

/**
 * Ditto as ToolLauncher except works on one specified
 * Tool rather than the discovery + tree display thing
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class SingleToolLauncher extends JPanel implements Widget, MouseMotionListener, ToolRunnerControl.DisplayHook {

    public static final String TITLE = "Tool Launcher";
    public static final Icon ICON = JarResources.getIcon("ToolLauncher.gif");
    private static final Logger klog = Logger.getLogger(SingleToolLauncher.class);
    private Tool fTool;
    private ParamSet fParamSet;
    private ToolRunnerControl fToolRunner;
    private ParamSetDisplay fDisplay;
    private String fName;
    private boolean fShowTopBufferPanel;
    private Icon fIcon;
    private String fTitle;

    private boolean fMakeNormalTheDefault;
    private boolean fShowGrayHelpText;

    /**
     * Class Constructor.
     */
    public SingleToolLauncher(final Tool tool,
                              final ParamSet pset,
                              final boolean showTopBufferPanel,
                              final boolean makeNormalTheDefault,
                              final boolean showGrayHelptext,
                              final String optTitle,
                              final Icon icon) {

        super();

        if (tool == null) {
            throw new IllegalArgumentException("Param tool cannot be null");
        }

        if (pset == null) {
            throw new IllegalArgumentException("Param pset cannot be null");
        }

        this.fTool = tool;
        this.fTitle = optTitle;
        this.fIcon = icon;
        this.fParamSet = pset;
        this.fShowTopBufferPanel = showTopBufferPanel;
        this.fMakeNormalTheDefault = makeNormalTheDefault;
        this.fShowGrayHelpText = showGrayHelptext;

        // use the specified one (may or maynot be same as the one in tool)
        this.fDisplay = new ParamSetDisplay(fTool.getTitle(), fIcon, fParamSet, this);
        this.fToolRunner = new ToolRunnerControl(this);
        this.fDisplay.addMouseMotionListener(this);
        checkTable();
        this.setLayout(new BorderLayout());

        if (fShowTopBufferPanel) {
            JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
            JLabel label = new JLabel("<html><body>Initialized to: " + "<b>" + fTool.getClass().getName() + "</b></body></html>");
            int height = 30;
            topPanel.add(label);
            //panel.setBackground(Color.DARK_GRAY);
            topPanel.setPreferredSize(new Dimension(this.getWidth(), height));
            this.add(topPanel, BorderLayout.NORTH);
        }

        this.add(Box.createHorizontalStrut(5), BorderLayout.EAST);
        this.add(Box.createHorizontalStrut(5), BorderLayout.WEST);

        this.add(new JScrollPane(fDisplay.getAsComponent()), BorderLayout.CENTER);
        this.add(fToolRunner, BorderLayout.SOUTH);
    }

    /**
     * ToolRunnerControl.DisplayHook impl.
     */
    public void resetParamSet() {
        fDisplay.reset();
    }

    /**
     * ToolRunnerControl.DisplayHook impl.
     * Might return null
     *
     * @return
     */
    public Tool getCurrentTool() {
        return fTool;
    }

    public ParamSet getCurrentParamSet() {
        return fParamSet;
    }

    public boolean isRecordToolRun() {
        return false;
    }

    public JComponent getWrappedComponent() {
        return this;
    }

    public String getAssociatedTitle() {
        if (fTitle != null) { // specified if custom (i.e to force opening even if same name)
            return fTitle;
        }

        // shorter is nicer
        if (fName == null) {
            StringTokenizer tok = new StringTokenizer(fTool.getClass().getName(), ".");
            while (tok.hasMoreTokens()) {
                fName = tok.nextToken();
            }
        }

        return fName;
    }

    public JMenuBar getJMenuBar() {
        return Widget.EMPTY_MENU_BAR;
    }

    public Icon getAssociatedIcon() {
        return fIcon;
    }

    private void checkTable() {
        ParamSet pset = fTool.getParamSet();
        boolean ready = pset.isRequiredAllSet();
        fToolRunner.setEnabledControls(ready);
    }

    public void mouseDragged(MouseEvent e) {
    }

    public void mouseMoved(MouseEvent e) {
        checkTable();
    }

}    // End SingleToolLauncher
