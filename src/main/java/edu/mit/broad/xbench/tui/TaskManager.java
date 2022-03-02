/*
 * Copyright (c) 2003-2022 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.xbench.tui;

import edu.mit.broad.genome.JarResources;
import edu.mit.broad.genome.reports.api.Report;
import edu.mit.broad.genome.reports.api.ToolReport;
import edu.mit.broad.genome.swing.GuiHelper;
import edu.mit.broad.xbench.actions.XDCAction;
import edu.mit.broad.xbench.core.JObjectsList;
import edu.mit.broad.xbench.core.api.Application;
import gnu.trove.TIntObjectHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xtools.api.Tool;
import xtools.api.param.ParamSet;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Singleton
 * Runs tools on diff threads and keep their status & records their params / ouytput
 * Available as a table to use in a widget
 * <p/>
 * Placed the createTable method in here as easier to handle updates etc.
 *
 * @author Aravind Subramanian, David Eby
 */
public class TaskManager {
    private static final Logger klog = LoggerFactory.getLogger(TaskManager.class);

    /**
     * Column headers for table model
     */
    private static final String[] COL_HEADERS = new String[]{" ", "Name", "Status"};

    /**
     * @maint keep in synch with col names above
     */
    private static final int COL_NUM = 0;
    private static final int COL_NAME = 1;
    private static final int COL_STATUS = 2;
    private final Model fModel;

    private JTable fTaskTable;

    private boolean fOnClickShowResultsInBrowserOnly; // false by default

    /**
     * The singleton instance
     */
    private static TaskManager kInstance;

    /**
     * Holds ToolRunnable objects
     */
    private final List<ToolRunnable> fToolRunnables;

    /**
     * @return Get a ref to the singleton
     */
    public static TaskManager getInstance() {

        if (kInstance == null) {
            synchronized (TaskManager.class) {
                if (kInstance == null) {
                    kInstance = new TaskManager();
                }
            }
        }

        return kInstance;
    }

    /**
     * Privatized Class Constructor.
     * Use getInstance to get a ref to the singleton
     */
    private TaskManager() {
        fToolRunnables = new ArrayList<ToolRunnable>();

        // must be made now - cant be done lazily
        fModel = new Model();
    }

    public void setOnClickShowResultsInBrowserOnly(boolean value) {
        this.fOnClickShowResultsInBrowserOnly = value;
    }

    /**
     * Runs tool on a thread.
     * specified Tool is just a "template" -> reflection invoked another tool
     * and that is filled with specified paramset
     * Will throw exception if the Tool barfs.
     * Adds tool to the ones it manages
     *
     * @param tool
     * @throws Exception
     */
    public Tool run(Tool tool, ParamSet pset, int priority) throws Exception {
        //TODO: do we need priority here?  No longer allowing it to change

        if (tool == null) {
            throw new IllegalArgumentException("Param tool cannot be null");
        }
        if (pset == null) {
            throw new IllegalArgumentException("Param pset cannot be null");
        }

        // errors here are propagated right away
        // -- tool NOT added to table
        // no task created
        Tool clonedTool;

        try {
            clonedTool = createTool(tool, pset);
        } catch (Exception t) {
            ToolRunnable pstate = ToolRunnable.createParamErrorToolState(tool, pset, t);    // @note adding tool skeleton directly
            fToolRunnables.add(pstate);
            kInstance.updateTable();
            throw t;
        }

        ToolRunnable trunnable = new ToolRunnable(clonedTool);
        fToolRunnables.add(trunnable);
        kInstance.updateTable();

        Thread t = new Thread(trunnable);
        t.setPriority(priority);
        trunnable.owner_thread = t;

        t.start();
        klog.debug("Started executing Tool: {} priority: {}", clonedTool.getClass().getName(), priority);
        return clonedTool;
    }

    // unbelievably wierd problems with Tool  - class is NULL  -throws npe
    // format: test(xtools.gsea.Gsea)
    // maybe coz class discoverer is used??
    // Creates a NEW Tool -> spec one used as skeleton only
    // New Tool made is NOT run
    // Exceptions are all reflection related + if any params are not set

    public static Tool createTool(final Tool tool, final ParamSet pset) throws Exception {
        String toolName = tool.getClass().getName();
        Class toolClass = Class.forName(toolName);
        klog.debug("toolClass: {} pset: {}", toolClass, pset);
        Class[] initArgsClass = new Class[]{Properties.class};    // reqd to have a ParamSet constructor
        Constructor initArgsConstructor = toolClass.getConstructor(initArgsClass);
        klog.debug("{}", initArgsConstructor);
        Properties prp = pset.toProperties();
        prp.remove("help"); // @note imp else produces usage!!
        Properties[] initArgs = new Properties[]{prp};
        System.out.println(">> " + prp);
        return (Tool) initArgsConstructor.newInstance((Object[])initArgs);
    }

    public static Tool createTool(final String toolName) throws Exception {
        Class toolClass = Class.forName(toolName);
        //klog.debug("ToolName: " + toolName + " class: " + toolClass);
        Class[] initArgsClass = new Class[]{};
        Constructor initArgsConstructor = toolClass.getConstructor(initArgsClass);
        return (Tool) initArgsConstructor.newInstance(new Object[]{});
    }

    private void updateTable() {
        // it does a a jig, but thats ok as visual indicator of a change in state
        fModel.fireTableStructureChanged(); // needed for consistent updates
        fTaskTable.repaint();
        fTaskTable.revalidate();
        setColNumWidth(fTaskTable);
    }

    /**
     * Inner class
     *
     * @author Aravind Subramanian
     */
    private class Model extends AbstractTableModel {
        private TIntObjectHashMap fRowToolButtonMap;
        private TIntObjectHashMap fRowReportButtonMap;

        private Model() {
            fRowToolButtonMap = new TIntObjectHashMap();
            fRowReportButtonMap = new TIntObjectHashMap();
        }

        public int getRowCount() {
            return fToolRunnables.size();
        }

        public int getColumnCount() {
            return COL_HEADERS.length;
        }

        /*
        * JTable uses this method to determine the default renderer/
        * editor for each cell.  If we didn't implement this method,
        * then the components dont show up.
        */
        public Class getColumnClass(int col) {
            // synched with the Editor
            return Object.class;
        }

        public String getColumnName(int col) {
            return COL_HEADERS[col];
        }

        public boolean isCellEditable(int row, int col) {
            return col != COL_NUM;
        }

        /**
         * dont bother with setValueAt() ->> let the renderer here take care of things
         * <p/>
         * NO! Have to do it here as the Editor gets data from getValueAt
         * Let the renderer handle ->> always return the pstate
         *
         * @return object value at specified row and column.
         * <p/>
         * NO! Have to do it here as the Editor gets data from getValueAt
         * Let the renderer handle ->> always return the pstate
         * @return object value at specified row and column.
         */

        /**
         * NO! Have to do it here as the Editor gets data from getValueAt
         * Let the renderer handle ->> always return the pstate
         *
         * @return object value at specified row and column.
         */

        private final Icon LHS_TOOL_ICON = JarResources.getIcon("dirty_ov.gif");

        public Object getValueAt(int row, int col) {
            ToolRunnable trunnable = (ToolRunnable) fToolRunnables.get(row);

            if (col == COL_NUM) {
                return new Integer(row + 1);
            } else if (col == COL_NAME) {
                JButton but;
                if (fRowToolButtonMap.get(row) == null) {
                    //klog.debug("Making new button for row: " + row);
                    but = new JButton();
                    but.setBorderPainted(false);
                    but.setFocusPainted(false);
                    fRowToolButtonMap.put(row, but);
                } else {
                    but = (JButton) fRowToolButtonMap.get(row);
                }

                // @todo clone tool
                Tool tool = trunnable.tool;
                SingleToolLauncherAction action = new SingleToolLauncherAction(tool, trunnable.pset, null);
                but.setAction(action);
                but.setIcon(LHS_TOOL_ICON);
                return but;
            } else if (col == COL_STATUS) {
                JButton but;
                if (fRowReportButtonMap.get(row) == null) {
                    but = new JButton();
                    but.setBorderPainted(false);
                    but.setFocusPainted(false);
                    fRowReportButtonMap.put(row, but);
                } else {
                    but = (JButton) fRowReportButtonMap.get(row);
                }

                ToolRunnableStateAction action = new ToolRunnableStateAction(trunnable, fOnClickShowResultsInBrowserOnly);
                but.setAction(action);

                if (trunnable.state == ExecState.SUCCESS) {
                    Report report = trunnable.tool.getReport();
                    if (report == null) {
                        but.setText("No Report Produced");
                    } else {
                        int len = report.getNumPagesMade();
                        if (len == 0) {
                            but.setText("0 Result Objects");
                        } else {
                            but.setForeground(Color.GREEN);
                            but.setText("<html><body><font color=green>Success</font></body></html>");
                            but.setIcon(GuiHelper.ICON_ELLIPSIS);
                            but.setVerticalTextPosition(JButton.TOP);
                            but.setToolTipText(but.getText());
                        }
                    }
                } else {
                    but.setText(trunnable.state.name);
                }

                but.setHorizontalAlignment(SwingConstants.CENTER);
                but.setForeground(trunnable.state.color);
                but.setBorderPainted(false);

                return but;

            }
            //add a button to launch Enrichmentmaps in cytoscape (which is placed in the options column)
            //   maybe more options will become available later so try and make it more generic
            else {
                return "Bad col: " + col;
            }
        }
    }

    /**
     * A custom renderer for table cells that works in conjunction with the table
     * and the Model to display the components corerectly
     *
     * @author Aravind Subramanian
     */
    private class Renderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {

            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            //klog.debug("rendering value: " + value);
            if (value instanceof Component) {
                return (Component) value;
            } else {
                return this;
            }
        }
    }

    /**
     * Object that represents a Tool and its ExecState
     *
     * @author Aravind Subramanian
     */
    static class ToolRunnable implements Runnable {
        private Tool tool;

        // may NOT be same as Tool pset!! esp when creation failed
        // see createParamErrorToolState
        private ParamSet pset;
        private ExecState state;
        private Throwable throwable;

        private Thread owner_thread;

        private boolean wasKilled;

        /**
         * creates a new ToolState
         */
        private ToolRunnable(Tool p) {
            if (p == null) { throw new IllegalArgumentException("Param p cannot be null"); }

            this.tool = p;
            this.pset = p.getParamSet();    // same one -- instantiated correctly
            this.state = ExecState.WAITING;
            this.throwable = null;
        }

        private static ToolRunnable createParamErrorToolState(Tool p, ParamSet pset, Throwable throwable) {
            if (pset == null) { throw new IllegalArgumentException("Param pset cannot be null"); }

            ToolRunnable ps = new ToolRunnable(p);

            ps.state = ExecState.PARAM_ERROR;
            ps.throwable = throwable;
            ps.pset = pset;    // change it to specified one

            return ps;
        }

        public void run() {
            try {
                this.state = ExecState.RUNNING;

                kInstance.updateTable();
                this.tool.execute();
                ToolReport report = (ToolReport)this.tool.getReport();
                if (report == null) {
                    this.state = ExecState.EXEC_ERROR;
                } else if (wasKilled) {
                    this.state = ExecState.KILLED;
                } else if (report.getToolWarnings().isEmpty()) {
                    this.state = ExecState.SUCCESS;
                } else {
                    this.state = ExecState.SUCCESS_WARN;
                }
            } catch (Throwable t) {
                this.state = ExecState.EXEC_ERROR;
                this.throwable = t;
                klog.error("Tool exec error", t);
                if (tool != null && tool.getReport() != null) {
                    tool.getReport().setErroredOut();
                }
            } finally {
                kInstance.updateTable();
            }
        }
    }

    /**
     * IMP: the taskmanager doesnt handle updates to its datamodel well --> "repaint"
     * doesnt get fired and found it easier to just place the table making stuff in here
     * so that we have access and can handle the repaints.
     *
     * @return A JTable holding data represented by the task managers model
     */
    public JTable createTable() {
        this.fTaskTable = new JTable(fModel);

        // table visual properties
        fTaskTable.setRowSelectionAllowed(false);
        fTaskTable.setColumnSelectionAllowed(false);
        fTaskTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        fTaskTable.setShowVerticalLines(true);
        fTaskTable.setGridColor(Color.black);
        fTaskTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        fTaskTable.getTableHeader().setReorderingAllowed(false);
        fTaskTable.setDefaultRenderer(Object.class, new Renderer());

        TableCellEditor ed = new Editor();

        fTaskTable.setDefaultEditor(Object.class, ed);

        // we know what the columns are so set their size explicitly
        setColNumWidth(fTaskTable);

        return fTaskTable;
    }

    private static final Icon RESULTS_ICON = JarResources.getIcon("Results.gif");

    // needs to be called after every fire structure changed
    // else its ok at first, and then after first fire, it changes back to default width
    private void setColNumWidth(JTable table) {
        TableColumn column = table.getColumnModel().getColumn(COL_NUM);

        column.setMinWidth(0);
        column.setMaxWidth(20);
        column.setPreferredWidth(20);
    }

    /**
     * Display a window depending on what status the tools state is
     * Error, Param error -> display of the stack trace
     */
    private class ToolRunnableStateAction extends XDCAction {
        private final ToolRunnable trunnable;
        private final ToolRunnableStateAction fTrsaInstance = this;
        private boolean fDoBrowser;

        private ToolRunnableStateAction(final ToolRunnable trunnable, final boolean doBrowser) {
            super("ToolStateAction", "Tool State", "Details on the Tools State", null);

            this.trunnable = trunnable;
            this.fDoBrowser = doBrowser;
        }

        public void actionPerformed(final ActionEvent evt) {
            if (fOnlyDoubleClick) { // respond only to double clicks
                return;
            }
            klog.debug("running ToolRunnableStateAction");

            if (trunnable.state == ExecState.WAITING) {
                Application.getWindowManager().showMessage("Waiting for: " + trunnable.tool.getClass().getName());
            } else if (trunnable.state == ExecState.PARAM_ERROR) {
                kInstance.updateTable();
                Application.getWindowManager().showError("One or more parameter(s) were not specified",
                        trunnable.throwable);

            } else if (trunnable.state == ExecState.EXEC_ERROR) {
                kInstance.updateTable();
                Application.getWindowManager().showError("Tool execution error", trunnable.throwable);
            } else if ((trunnable.state == ExecState.RUNNING) || (trunnable.state == ExecState.PAUSED)) {
                kInstance.updateTable();
                
                // TODO: track down meaning & usage of PAUSED and see if we can drop it.
                
                // TODO: also need to look at Thread priority setting mechanism.

            } else if ((trunnable.state == ExecState.SUCCESS) || (trunnable.state == ExecState.SUCCESS_WARN) || (trunnable.state == ExecState.KILLED)) {
                kInstance.updateTable();
                // TODO: track down meaning and usage of KILLED.  Can a Thread still get into this state?  How?
                Report report = trunnable.tool.getReport();
                if (report == null) {
                    fTrsaInstance.setEnabled(false);// not clickable
                    Application.getWindowManager().showMessage("No report data produced");
                } else {
                			if (fDoBrowser) {
                				try {
                					URL url = report.getReportIndex().toURL();
                					if (url != null) {
                						Desktop.getDesktop().browse(url.toURI());                                
                					} else {
                						Application.getWindowManager().showMessage("No report produced");
                					}
                				} catch (Throwable t) {
                					Application.getWindowManager().showError(t);
                				}
                			} else {                				
                				JObjectsList jol = new JObjectsList(report.getFilesProduced());
                				JObjectsList.displayInWindow("Results for: " + trunnable.tool.getClass().getName(), RESULTS_ICON, jol);
                			}               		
                }
            } else {
                kInstance.updateTable();
                Application.getWindowManager().showMessage("No actions defined for this state " + trunnable.state + " " + trunnable.tool.getClass().getName());
            }
        }
    }

    private class Editor extends AbstractCellEditor implements TableCellEditor {
        private Object currVal;

        /**
         * TableCellEditor impl.
         * The core method to implement to acheive desired effect.
         */
        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int column) {
            currVal = value;

            if (value instanceof Component) {
                return (Component) value;
            } else {
                currVal = new JLabel(value.toString());
                return (JLabel) currVal;
            }
        }

        public Object getCellEditorValue() {
            return currVal;
        }
    }
}
