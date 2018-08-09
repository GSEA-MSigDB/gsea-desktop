/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.models;

import edu.mit.broad.genome.parsers.ParserFactory;
import edu.mit.broad.genome.reports.api.Report;
import edu.mit.broad.genome.utils.DateUtils;
import edu.mit.broad.xbench.core.api.Application;
import edu.mit.broad.xbench.tui.ReportStub;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;

/**
 * Manages 2 things
 * 1) listens to reports being produced by the Parserfactory -> session
 * 2) looks up past reports (History)
 */
public class ReportModel {

    /**
     * excludenames -> so that during debugging and running under the junti cache thing
     * we dont show duplicate names of things in session or history
     */
    private Set fSessionReportNames;

    /**
     * Privatized class constructor
     * Only public static methods
     */
    public ReportModel() {
        fSessionReportNames = new HashSet();
    }

    /**
     * Class constructor
     *
     * @param treeModelGroup -> shared one, update when somethign gets added to the reports model
     */
    public DefaultMutableTreeNode createReportNode(final DefaultTreeModel treeModelGroup) {

        if (treeModelGroup == null) {
            throw new IllegalArgumentException("Parameter treeModelGroup cannot be null");
        }

        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Reports", true);
        DefaultMutableTreeNode sessionNode = new DefaultMutableTreeNode("Current Session", true);

        // now add all reports in the current session (i.le objcache) to the session node
        List pobs = ParserFactory.getCache().getCachedObjectsL(Report.class);
        Collections.sort(pobs, new ReportHistoryNodeComparator());
        for (int i = 0; i < pobs.size(); i++) {
            Report rpt = (Report) pobs.get(i);
            if (fSessionReportNames.contains(rpt.getName()) == false) {
                sessionNode.add(new DefaultMutableTreeNode(rpt, false));
                fSessionReportNames.add(rpt.getName());
            }
        }

        // find reports in cache and add those
        // (for development dont add those in current cache)
        DefaultMutableTreeNode historyNode = new DefaultMutableTreeNode("History", true);
        DefaultMutableTreeNode[] histnodes = createCachedReportHistoryNodesByDay();
        for (int i = 0; i < histnodes.length; i++) {
            historyNode.add(histnodes[i]);
        }

        rootNode.add(sessionNode);
        rootNode.add(historyNode);

        treeModelGroup.reload(sessionNode);
        treeModelGroup.reload(historyNode);

        ParserFactory.getCache().addReportAdditionsListener(new MySessionReportListener(treeModelGroup, sessionNode));

        return rootNode;
    }


    private DefaultMutableTreeNode[] createCachedReportHistoryNodesByDay() {
        ReportStub[] stubs = Application.getToolManager().getReportsInCache();

        Map dayStubsMap = new HashMap();

        for (int i = 0; i < stubs.length; i++) {
            if (fSessionReportNames.contains(stubs[i].getName())) {
                continue; // exclude the ones in this session
            }
            DayTimestamp dts = new DayTimestamp(stubs[i].getTimestamp(), 
                    DateUtils.formatAsDayMonthYear(stubs[i].getDate()));
            Object list = dayStubsMap.get(dts);
            if (list == null) {
                list = new ArrayList();
            }
            ((ArrayList) list).add(stubs[i]);
            dayStubsMap.put(dts, list);
        }

        Iterator it = dayStubsMap.keySet().iterator();

        List nodes = new ArrayList(dayStubsMap.size());

        while (it.hasNext()) {
            DayTimestamp dts = (DayTimestamp) it.next();
            List list = (List) dayStubsMap.get(dts);
            Collections.sort(list, new ReportHistoryNodeComparator());
            DefaultMutableTreeNode dayNode = new DefaultMutableTreeNode(dts, true);
            for (int i = 0; i < list.size(); i++) {
                dayNode.add(new DefaultMutableTreeNode(list.get(i)));
            }
            nodes.add(dayNode);
        }

        Collections.sort(nodes, new ReportHistoryNodeComparator());
        return (DefaultMutableTreeNode[]) nodes.toArray(new DefaultMutableTreeNode[nodes.size()]);

    }

    /**
     * for Listening to cache events and adding to recent files mechanism
     *
     * @author Aravind Subramanian
     * @version %I%, %G%
     */
    private class MySessionReportListener implements PropertyChangeListener {
        private DefaultTreeModel model2Reload;
        private DefaultMutableTreeNode sessionNode;

        private MySessionReportListener(DefaultTreeModel model2Reload, DefaultMutableTreeNode sessionNode) {
            this.model2Reload = model2Reload;
            this.sessionNode = sessionNode;
        }

        public void propertyChange(PropertyChangeEvent evt) {
            //klog.debug("Got change event: " + evt);
            Object obj = evt.getNewValue();
            if (obj instanceof Report) {
                Report rpt = (Report) obj;
                if (fSessionReportNames.contains(rpt.getName()) == false) {
                    //klog.debug("Adding reports: " + rpt);
                    sessionNode.add(new DefaultMutableTreeNode(rpt, false));
                    model2Reload.reload(sessionNode);
                    fSessionReportNames.add(rpt.getName());
                }
            }

            // while debugging because of teh caching thing need to watch put for doubel additions

        }
    }

    static class ReportHistoryNodeComparator implements Comparator {

        public int compare(Object pn1, Object pn2) {
            long ts1;
            long ts2;
            if (pn1 instanceof DefaultMutableTreeNode) {
                ts1 = ((DayTimestamp) ((DefaultMutableTreeNode) pn1).getUserObject()).ts;
                ts2 = ((DayTimestamp) ((DefaultMutableTreeNode) pn2).getUserObject()).ts;
            } else if (pn1 instanceof Report) {
                ts1 = ((Report) pn1).getTimestamp();
                ts2 = ((Report) pn2).getTimestamp();
            } else {
                ts1 = ((ReportStub) pn1).getTimestamp();
                ts2 = ((ReportStub) pn2).getTimestamp();
            }

            if (ts1 > ts2) {
                return -1;
            } else if (ts1 == ts2) {
                return 0;
            } else {
                return +1;
            }
        }

        public boolean equals(Object o2) {
            return false;
        }
    }    // End ReportHistoryNodeComparator

    /**
     * struc to capture the timestamp but represent it as a Day string
     * so that 1) more user friendly dispay
     * 2) allows us to sort easily by retaining the timestamp
     */
    class DayTimestamp {
        long ts;
        String day;

        DayTimestamp(long ts, String day) {
            this.ts = ts;
            this.day = day;
        }

        public int hashCode() {
            return day.hashCode();
        }


        public boolean equals(Object obj) {
            if (obj instanceof DayTimestamp) {
                return equals((DayTimestamp) obj);
            } else {
                return false;
            }
        }

        public boolean equals(DayTimestamp dts) {
            return dts.day.equals(this.day);
        }


        public String toString() {
            return day;
        }

    }

} // End ReportModel
