/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.xbench.tui;

import edu.mit.broad.genome.reports.api.Report;
import edu.mit.broad.genome.swing.GseaSimpleInternalFrame;
import edu.mit.broad.genome.swing.GuiHelper;
import edu.mit.broad.genome.viewers.ReportViewer;
import edu.mit.broad.xbench.core.Widget;
import edu.mit.broad.xbench.core.api.Application;
import edu.mit.broad.xbench.prefs.XPreferencesFactory;

import org.apache.log4j.Logger;

import xtools.api.Tool;
import xtools.api.ToolCategory;
import xtools.api.param.ParamSet;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * UI to run previous analysis jobs, organized in a tree display
 * To be renamed...
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class ToolLauncherDefaultImpl extends JPanel implements ToolLauncher, MouseMotionListener, ToolRunnerControl.DisplayHook {

    private String fTitle = "Analysis history";
    private Icon fIcon = ReportViewer.ICON;
    private static final Logger klog = Logger.getLogger(ToolLauncherDefaultImpl.class);
    private final JSplitPane splitPane = new JSplitPane();
    private ToolSelectorTree fToolSelectorTree;

    /**
     * key -> Tool, value -> ParamSetDisplay
     */
    private final Map fToolParamSetDisplayMap = new WeakHashMap();
    private Tool fCurrTool;
    private ToolRunnerControl fToolRunner;

    /**
     * key toolcat, value -> desc of toolcat + name/desc of tools belonging to it
     */
    private final Map fToolCatDescMap = new WeakHashMap();

    /**
     * key -> Report, value -> ReportViewer Panel
     */
    private final Map fReportViewerPanelMap = new WeakHashMap();

    private ToolLauncherDefaultImpl fInstance = this;

    private Report fCurrReport;

    private ReportStub fCurrReportStub;

    public ToolLauncherDefaultImpl() {

        splitPane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
        this.fToolSelectorTree = new ToolSelectorTree();
        
        JScrollPane sp = new JScrollPane(fToolSelectorTree.getComponent());
        
        int width = XPreferencesFactory.getToolTreeWidth();
        int width_min = XPreferencesFactory.getToolTreeWidth_min();
        sp.setPreferredSize(new Dimension(width, splitPane.getHeight()));
        sp.setMinimumSize(new Dimension(width_min, 0));
        
        GseaSimpleInternalFrame sif = new GseaSimpleInternalFrame(fTitle);
        sif.setPreferredSize(new Dimension(width - 20, splitPane.getHeight() - 20));
        sif.setMinimumSize(new Dimension(width_min, 0));
        sif.add(sp);
        splitPane.add(sif, JSplitPane.LEFT);
        
        splitPane.add(GuiHelper.createNaPlaceholder(), JSplitPane.RIGHT);
        splitPane.setDividerLocation(XPreferencesFactory.getToolTreeDivLocation());
        splitPane.setOneTouchExpandable(false); // imp else makes it hard to get back to original
        splitPane.setDividerSize(3);
        //splitPane.setLastDividerLocation(270);
        splitPane.setLastDividerLocation(LHS_SIZE);
        
        fToolRunner = new ToolRunnerControl(this);
        
        this.setLayout(new BorderLayout());
        this.add(splitPane, BorderLayout.CENTER);

        fToolSelectorTree.addTreeSelectionListener(new TreeSelectionListener() {

            public void valueChanged(TreeSelectionEvent e) {

                try {
                    int divloc = splitPane.getDividerLocation();
                    TreePath path = fToolSelectorTree.getSelectionPath();
                    Object val;

                    if (path == null && fCurrReport != null) {
                        val = fCurrReport; // else the thing changes to disabled whern a new reports is added to cache
                    } else if (path == null && fCurrReportStub != null) {
                        val = fCurrReportStub;
                    } else if (path == null) {
                        disableLauncher();
                        splitPane.setDividerLocation(divloc);
                        return;
                    } else {
                        val = path.getLastPathComponent();
                    }

                    if (val == null) {
                        disableLauncher();
                        return;
                    } else if (val instanceof DefaultMutableTreeNode) {
                        fCurrReport = null;
                        fCurrReportStub = null;
                        val = ((DefaultMutableTreeNode) val).getUserObject();

                        if (val instanceof Tool) {
                            Tool tool = (Tool) val;

                            if (tool == fCurrTool) {
                            } else {
                                Object obj = fToolParamSetDisplayMap.get(tool);

                                if (obj == null) {
                                    obj = new ParamSetDisplay(tool.getTitle(), null, tool.getParamSet(), fInstance);
                                    ((ParamSetDisplay) obj).addMouseMotionListener(fInstance);
                                    fToolParamSetDisplayMap.put(tool, obj);
                                }

                                //log.debug("Setting table: " + obj);
                                ParamSetDisplay psd = (ParamSetDisplay) obj;
                                JScrollPane pane = new JScrollPane(psd.getAsComponent());
                                splitPane.setRightComponent(pane);
                                splitPane.revalidate();

                                fCurrTool = tool;
                            }

                            checkTable();
                        } else if (val instanceof ToolCategory) {
                            fCurrTool = null;
                            showToolCategoryInfo((ToolCategory) val);
                        } else if (val instanceof Report) {
                            fCurrTool = null; // so that on next click on the tool we can see it!
                            Report rpt = (Report) val;
                            fCurrReport = rpt;
                            Object obj = fReportViewerPanelMap.get(rpt);
                            if (obj == null) {
                                obj = new ReportViewer(rpt);
                                fReportViewerPanelMap.put(rpt, obj);
                            }
                            fToolRunner.setEnabledControls(false);
                            splitPane.setRightComponent((JPanel) obj); // comes with its own scrollers
                            splitPane.revalidate();
                        } else if (val instanceof ReportStub) {
                            fCurrTool = null;
                            ReportStub stub = (ReportStub) val;
                            try {
                                Report rpt = stub.getReport(false); // IMP dont use / add2 cache
                                fCurrReportStub = stub;
                                Object obj = fReportViewerPanelMap.get(rpt);
                                if (obj == null) {
                                    obj = new ReportViewer(rpt);
                                    fReportViewerPanelMap.put(rpt, obj);
                                }
                                fToolRunner.setEnabledControls(false);
                                splitPane.setRightComponent((JPanel) obj); // comes with its own scrollers
                                splitPane.revalidate();
                            } catch (Throwable t) {
                                Application.getWindowManager().showError("Bad reports file", t);
                                // cleanup
                                stub.getReportFile().deleteOnExit();
                                disableLauncher();
                            }
                        } else {
                            //log.debug("Selected value: " + val + " class: " + val.getClass());
                            disableLauncher();
                        }
                    } else if (val instanceof Report) {
                        fCurrTool = null; // so that on next click on the tool we can see it!
                        Report rpt = (Report) val;
                        fCurrReport = rpt;
                        Object obj = fReportViewerPanelMap.get(rpt);
                        if (obj == null) {
                            obj = new ReportViewer(rpt);
                            fReportViewerPanelMap.put(rpt, obj);
                        }
                        fToolRunner.setEnabledControls(false);
                        splitPane.setRightComponent((JPanel) obj); // comes with its own scrollers
                        splitPane.revalidate();

                        fCurrReportStub = null;
                    } else {
                        //log.debug("Selected value: " + val + " class: " + val.getClass());
                        disableLauncher();
                    }

                    splitPane.setDividerLocation(divloc);

                } catch (Throwable t) {
                    Application.getWindowManager().showError("Error creating param table", t);
                }
            }
        });

        /// must call *after* listeners have been added
        // also foxtrot insists that it be in the awt event loop
        // watch out for recursion as it causes an event
        initToLastToolRun();
    }

    

    //private JScrollPane sp_for_box;

    private static final int LHS_SIZE = 250;

    /**
     * ToolRunnerControl.DisplayHook impl.
     */
    public void resetParamSet() {
        if (getCurrentTool() != null) {
            getCurrentParamDisplayComponent().reset();
        }
    }

    /**
     * ToolRunnerControl.DisplayHook impl.
     * Might return null
     *
     * @return
     */
    public Tool getCurrentTool() {
        return fCurrTool;
    }

    public ParamSet getCurrentParamSet() {
        return getCurrentTool().getParamSet();
    }

    public boolean isRecordToolRun() {
        return true;
    }

    public JComponent getWrappedComponent() {
        return this;
    }

    public String getAssociatedTitle() {
        if (fTitle != null) {
            return fTitle;
        } else {
            return TITLE;
        }
    }

    public JMenuBar getJMenuBar() {
        return Widget.EMPTY_MENU_BAR;
    }

    public Icon getAssociatedIcon() {
        if (fIcon == null) {
            return ICON;
        } else {
            return fIcon;
        }
    }

    public void mouseDragged(final MouseEvent e) {
    }

    public void mouseMoved(final MouseEvent e) {
        checkTable();
    }

    // -------------------------------------------------------------------------------------------- //
    // ------------------------------------ PRIVATE METHODS -------------------------------------- //
    // -------------------------------------------------------------------------------------------- //

    /**
     * must be safe - dont barf if this isnt possible
     */
    private void initToLastToolRun() {

        try {

            final String p = Application.getToolManager().getLastToolName();

            if (p == null) {
                return;
            }

            fToolSelectorTree.selectTool(p);

        } catch (Throwable t) {
            klog.warn(t);
        }
    }

    /**
     * might return null
     *
     * @return
     */
    private ParamSetDisplay getCurrentParamDisplayComponent() {

        Tool p = getCurrentTool();

        if (p != null) {
            return (ParamSetDisplay) fToolParamSetDisplayMap.get(p);
        }

        return null;
    }

    private void checkTable() {
        ParamSet pset = fCurrTool.getParamSet();
        boolean ready = pset.isRequiredAllSet();
        //log.debug("checking for all done: " + ready);
        fToolRunner.setEnabledControls(ready);
    }

    private void disableLauncher() {
        fToolRunner.setEnabledControls(false);
        splitPane.setRightComponent(GuiHelper.createNaPlaceholder());
        splitPane.revalidate();
    }

    private void showToolCategoryInfo(final ToolCategory cat) {
        fToolRunner.setEnabledControls(false);
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BorderLayout());
        infoPanel.add(new JLabel(cat.getName(), cat.getIcon(), JLabel.LEFT), BorderLayout.NORTH);
        JTextArea ta = new JTextArea(fToolCatDescMap.get(cat).toString());
        ta.setWrapStyleWord(true);
        ta.setEditable(false);
        infoPanel.add(new JScrollPane(ta), BorderLayout.CENTER);
        splitPane.setRightComponent(infoPanel);
        splitPane.revalidate();
    }

}    // End ToolLauncherTreeImpl
