/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.viewers;

import au.com.pegasustech.demos.layout.SCLayout;

import com.jidesoft.grid.SortableTable;

import edu.mit.broad.genome.JarResources;
import edu.mit.broad.genome.reports.api.Report;
import edu.mit.broad.genome.swing.GuiHelper;
import edu.mit.broad.xbench.core.JObjectsList;
import edu.mit.broad.xbench.core.api.Application;
import edu.mit.broad.xbench.tui.SingleToolLauncher;
import edu.mit.broad.xbench.tui.TaskManager;
import edu.mit.broad.xbench.tui.ToolRunnerControl;
import foxtrot.Job;
import foxtrot.Worker;
import xtools.api.Tool;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;

/**
 * Report viewer.
 * Functionality:
 * <p/>
 * Table with params
 * label for reports name, time run
 * button to relaunch a tool with those params (deprecated ones ignored)
 * list of files produced with clickability
 * <p/>
 * Works in 2 modes wrt tools:
 * 1) tool lauinched in a new SingleToolLauncher window
 * 2) tool launched in the super ToolLauncher
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class ReportViewer extends AbstractViewer {

    public static final String NAME = "ReportViewer";
    public static final Icon ICON = JarResources.getIcon("past_analysis16.gif");
    private final Report fReport;

    private ReportViewer fInstance = this;

    private final Properties fParams;

    private static final String[] COL_HEADERS = new String[]{"Parameter name", "Parameter value"};

    /**
     * Class constructor
     *
     * @param rpt
     */
    public ReportViewer(final Report rpt) {
        super(NAME, ICON, rpt);

        this.fReport = rpt;
        this.fParams = fReport.getParametersUsed();
        jbInit();
    }

    private void jbInit() {
        SCLayout scl = new SCLayout(3);
        scl.setRowScale(0, 0.10);
        scl.setRowScale(1, 0.45);
        scl.setRowScale(2, 0.45);
        this.setLayout(scl);

        Date d = new Date(fReport.getTimestamp());
        StringBuffer buf = new StringBuffer("<html><body><b><f bold>Report: </b><font color=blue>").append(fReport.getName()).append("</font><br>");
        buf.append("<b>Date  : </b><font color=blue>").append(d.toString());
        buf.append("</font></body></html>");

        JLabel label = new JLabel(buf.toString());
        this.add(label);

        this.add(createParamPanel(fReport.getName()));

        JObjectsList jol = new JObjectsList(fReport.getFilesProduced());
        jol.setBorder(GuiHelper.createTitledBorderForComponent("Files produced as part of this analysis (double-click to view)"));
        this.add(new JScrollPane(jol));
        this.revalidate();


    }

    private JPanel createParamPanel(final String rptName) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        //JTable table = new JTable(new Model());
        SortableTable table = new SortableTable(new Model()); // @note changed for jide

        table.setColumnSelectionAllowed(true);
        table.setRowSelectionAllowed(true);

        //table.getTableHeader().setDefaultRenderer(new RendererFactory2.BoldHeaderRenderer());
        table.getTableHeader().setReorderingAllowed(false);
        setColumnSize(75, 0, table, false);

        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel cp = new JPanel();

        final JCheckBox cxLoad = new JCheckBox("Load data", true);
        cxLoad.setToolTipText("Follow files specified in params and load their data");
        cp.add(cxLoad);

        // this parses stuff right away - dont want that
        //SingleToolLauncherAction a = new SingleToolLauncherAction(tool, tool.getParamSet());

        JButton bRelaunch = new JButton("Show in ToolRunner", SingleToolLauncher.ICON);
        bRelaunch.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                Worker.post(new Job() {
                    public Object run() {

                        // @note IMP make assumption that an xtool
                        try {
                            final Tool tool = TaskManager.createTool(fReport.getProducer().getName());
                            Runnable runnable = ToolRunnerControl.createLoadToolTask(tool, rptName,
                                    cxLoad.isSelected(), fParams, fInstance, true);
                            Thread t = new Thread(runnable);
                            t.setPriority(Thread.MIN_PRIORITY);
                            t.start();

                        } catch (Throwable t) {
                            Application.getWindowManager().showError(t);
                        }

                        return null;
                    }
                });
            }

        });

        cp.add(bRelaunch);

        panel.add(cp, BorderLayout.SOUTH);

        return panel;
    }

    public JMenuBar getJMenuBar() {
        return EMPTY_MENU_BAR;
    }

    /**
     * @author Aravind Subramanian
     * @version %I%, %G%
     */
    private class Model extends AbstractTableModel {

        private final String[] keys;

        private Model() {
            keys = new String[fParams.size()];

            int cnt = 0;
            Enumeration en = fParams.keys();
            while (en.hasMoreElements()) {
                keys[cnt++] = en.nextElement().toString();
                //log.debug("added: " + keys[cnt-1]);
            }

            //log.debug("Number of rows = " + keys.length);

        }

        public int getRowCount() {
            return keys.length;
        }

        public int getColumnCount() {
            return COL_HEADERS.length;
        }

        public String getColumnName(int col) {
            return COL_HEADERS[col];
        }

        public boolean isCellEditable(int row, int col) {
            return true;
        }

        public Object getValueAt(int row, int col) {
            if (col == 0) {
                return keys[row];
            } else {
                return fParams.getProperty(keys[row]);
            }
        }
    }    // End Model

}        // End ReportViewer
