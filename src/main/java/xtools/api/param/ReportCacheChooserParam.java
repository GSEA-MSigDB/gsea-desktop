/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xtools.api.param;

import edu.mit.broad.genome.parsers.DataFormat;
import edu.mit.broad.genome.parsers.ParserFactory;
import edu.mit.broad.genome.reports.api.Report;
import edu.mit.broad.genome.swing.fields.GFieldPlusChooser;
import edu.mit.broad.genome.swing.fields.GOptionsFieldPlusChooser;
import edu.mit.broad.genome.utils.DateUtils;
import edu.mit.broad.xbench.core.api.Application;
import edu.mit.broad.xbench.tui.ReportStub;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;

/**
 * Custom impl as i want the file listing to happen slowly
 *
 * @author Aravind Subramanian
 */
public class ReportCacheChooserParam extends AbstractObjectChooserParam {

    public ReportCacheChooserParam(final String nameEnglish) {
        super(RPT_DIR, nameEnglish, Report.class, RPT_DIR_DESC, new Report[]{}, new Report[]{}, false);
    }

    // @note this is the magix -> we set the hints lazilly
    public GFieldPlusChooser getSelectionComponent() {
        // load this every time so that we always get the latest cache

        super.setHints(_getReportsInCache());
        GOptionsFieldPlusChooser chooser = (GOptionsFieldPlusChooser) super.getSelectionComponent();
        chooser.getJListWindow().getJList().setCellRenderer(new MyListRenderer());
        chooser.setListSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);

        return super.getSelectionComponent();
    }

    public File getReportDir() throws Exception {

        Object val = getValue();

        if (val == null) {
            throw new NullPointerException("Null param value. Always check isSpecified() before calling");
        }

        // HACK
        File file = new File(val.toString());
        if (file.exists() && file.isDirectory()) {
            return file;
        }

        // this is the only thing that should be needed
        return getReport().getReportDir();
    }

    public Report getReport() throws Exception {
        Object val = getValue();

        if (val == null) {
            throw new NullPointerException("Null param value. Always check isSpecified() before calling");
        }

        if (val instanceof Report) {
            return (Report) val;
        } else {
            return ParserFactory.readReport(new File(val.toString()), true);
        }

    }

    // @note this is also the magix ->L format as a rs and not as a tostring
    protected static String format(final Object[] vals) {

        if (vals == null) {
            return "";
        }

        StringBuffer buf = new StringBuffer();

        for (int i = 0; i < vals.length; i++) {
            if (vals[i] instanceof Report) {
                final Report report = (Report) vals[i];
                buf.append(report.getReportDir().getPath());
                //buf.append(report.get);
                if (i != vals.length - 1) {
                    buf.append(',');
                }
            } else {
                klog.warn("Illegal state: " + vals[i]);
            }
        }

        return buf.toString();
    }

    protected ActionListener getActionListener() {
        return new MyActionListener();
    }

    // we impl custom here as dont want existing text to be wiped out if the button is clicked but
    // no choice made
    // Plus so that we can select the current ones if possible
    class MyActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {

            setHints(_getReportsInCache()); // refresh it

            Object prev = getValue();
            Object[] sels;
            if (prev != null && prev instanceof Object[]) {
                sels = fChooser.getJListWindow().show(getHints(), (Object[]) prev);
            } else {
                sels = fChooser.getJListWindow().show(getHints(), new Object[]{});
            }

            //log.debug("Got selections: " + sels);

            if ((sels == null) || (sels.length == 0)) { // <-- @note

            } else {
                String str = format(sels);
                fChooser.setText(str);
            }
        }
    }


    private Report[] _getReportsInCache() {
        final File[] files = getReportFiles();
        java.util.List<Report> reports = new ArrayList<Report>();
        for (int i = 0; i < files.length; i++) {
            try {
                ReportStub stub = new ReportStub(files[i]);
                if (stub.getName().indexOf("Gsea") != -1) {
                    Report report = stub.getReport(false);
                    File rptDir = report.getReportDir();
                    File edbDir = new File(rptDir, "edb");
                    if (edbDir.exists()) {
                        reports.add(report);
                    }
                }
            } catch (Throwable t) {
                log.error(t);
            }
        }

        Comparator<Report> reportComparator = new Comparator<Report>() {
            @Override
            public int compare(Report o1, Report o2) {
                // NOTE: descending
                return o2.getDate().compareTo(o1.getDate());
            }
        };
        
        reports.sort(reportComparator);
        
        return reports.toArray(new Report[reports.size()]);
    }

    private File[] getReportFiles() {
        final File dir = Application.getVdbManager().getReportsCacheDir();

        if (dir.exists() == false) {
            log.warn("Report cache: " + dir + " not found");
            return new File[]{};
        }

        return dir.listFiles(DataFormat.RPT_FORMAT.getFilenameFilter());
    }


    static class MyListRenderer extends DefaultListCellRenderer {

        public Component getListCellRendererComponent(final JList list,
                                                      final Object value,
                                                      final int index,
                                                      final boolean isSelected,
                                                      final boolean cellHasFocus) {

            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof Report) {
                Report rs = (Report) value;
                this.setToolTipText(rs.getReportDir().getPath());
                String text = rs.getName() + " [" + DateUtils.formatAsDayMonthYear(rs.getDate()) + "]";
                this.setText(text);
            }

            return this;
        }
    }    // End CommonLookListRenderer


} // End class ReportCacheChooserParam
