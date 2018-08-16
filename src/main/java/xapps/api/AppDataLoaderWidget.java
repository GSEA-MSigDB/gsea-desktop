/*******************************************************************************
 * Copyright (c) 2003-2018 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xapps.api;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;

import com.jidesoft.swing.JideButton;
import com.jidesoft.swing.JideSplitPane;

import au.com.pegasustech.demos.layout.SRLayout;
import edu.mit.broad.genome.JarResources;
import edu.mit.broad.genome.parsers.AuxUtils;
import edu.mit.broad.genome.parsers.ParserWorker;
import edu.mit.broad.genome.swing.GseaSimpleInternalFrame;
import edu.mit.broad.genome.swing.GuiHelper;
import edu.mit.broad.genome.swing.dnd.DndTarget;
import edu.mit.broad.genome.swing.dnd.DropTargetDecorator;
import edu.mit.broad.xbench.actions.XAction;
import edu.mit.broad.xbench.actions.ext.BrowserAction;
import edu.mit.broad.xbench.core.Widget;
import edu.mit.broad.xbench.core.api.Application;
import edu.mit.broad.xbench.explorer.filemgr.JRecentFilesList;
import edu.mit.broad.xbench.explorer.filemgr.XFileChooser;
import edu.mit.broad.xbench.explorer.objmgr.ObjectTree;
import edu.mit.broad.xbench.tui.ReportStub;
import edu.mit.broad.xbench.tui.TaskManager;
import edu.mit.broad.xbench.tui.ToolRunnerControl;
import xapps.gsea.GseaAppConf;
import xapps.gsea.GseaWebResources;
import xtools.api.Tool;
import xtools.gsea.Gsea;

/**
 * Widget that builds the in a JTabbedPane:
 * 1) file system view along with a few easy access buttons
 * 2) a panel for the recently accessed file list
 * 3) opens up in last folder disp if possible
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class AppDataLoaderWidget extends GseaSimpleInternalFrame implements Widget {

    public static final String TITLE = "Load data";
    public static final Icon ICON = JarResources.getIcon("LocalFileExplorerWidget_16_v2.jpg");

    private JComponent previousFilesPanel;

    private AppDataLoaderWidget fInstance = this;

    /**
     * Class Constructor.
     */
    public AppDataLoaderWidget() {
        super(null, "<html><body><b>Load data</b>: Import data into the application</body></html>");

        init();
    }

    private void init() {

        jbInit();

    }

    // does the GUI building
    private void jbInit() {

        JPanel loadPanel = createLoadPanel();

        this.previousFilesPanel = JRecentFilesList.createComponent("<html><body><b>Recently used files</b> <br> " +
                "(double click to load, right click for more options)</body></html>");


        JideSplitPane split = new JideSplitPane(JideSplitPane.HORIZONTAL_SPLIT);
        split.setInitiallyEven(true);
        split.setShowGripper(true);
        split.add(previousFilesPanel);

        ObjectTree tree = new ObjectTree();
        tree.setBorder(null);
        GseaSimpleInternalFrame sif = new GseaSimpleInternalFrame("<html><body><b>Object cache</b> <br> " +
                "(objects already loaded & ready for use, right click for more options)</body></html>");
        sif.add(new JScrollPane(tree), BorderLayout.CENTER);
        split.add(sif);
        split.setInitiallyEven(true);
        split.setProportionalLayout(true);


        this.add(loadPanel, BorderLayout.CENTER);
        split.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        this.add(split, BorderLayout.SOUTH);
    }

    private JPanel createLoadPanel() {

        JideButton bBrowse = new JideButton(new FileOpenAction());
        bBrowse.setRolloverEnabled(true);
        bBrowse.setButtonStyle(JideButton.TOOLBAR_STYLE);
        bBrowse.setBorder(BorderFactory.createTitledBorder("Method 1:"));

        JideButton bLoadLast = new JideButton(new LoadLastAnalysisFilesAction());
        bLoadLast.setRolloverEnabled(true);
        bLoadLast.setButtonStyle(JideButton.TOOLBAR_STYLE);
        bLoadLast.setBorder(BorderFactory.createTitledBorder("Method 2:"));

        JLabel label = new JLabel("<html>\n" +
                "<body>\n" +
                "\n" +
                "    <b>Dataset</b>:\n" +
                "    <font color=\"#800000\"><i><b><font size=\"4\">res</font></b></i></font><font size=\"4\">\n" +
                "    </font>or <font color=\"#800000\" size=\"4\"><i><b>gct</b></i></font> \n" +
                "    (Broad/MIT)," +
                "<p>&nbsp;&nbsp&nbsp;&nbsp&nbsp;&nbsp&nbsp;&nbsp&nbsp;&nbsp&nbsp;&nbsp;&nbsp&nbsp;&nbsp;&nbsp&nbsp;&nbsp" +
                "<font color=\"#800000\" size=\"4\"><i><b>pcl</b></i></font> (Stanford)</p>\n" +
                "<p>&nbsp;&nbsp&nbsp;&nbsp&nbsp;&nbsp&nbsp;&nbsp&nbsp;&nbsp&nbsp;&nbsp;&nbsp&nbsp;&nbsp;&nbsp&nbsp;&nbsp" +
                "<font color=\"#800000\" size=\"4\"><i><b>txt</b></i></font> (tab-delim text)</p>\n" +
                "    <p>&nbsp;&nbsp<b>Phenotype</b> <b>labels</b>:\n" +
                "    <font color=\"#800000\" size=\"4\"><i><b>cls</b></i></font></p>\n" +
                "    <p>&nbsp;&nbsp<b>Gene sets</b>:\n" +
                "    <font color=\"#800000\" size=\"4\"><i><b>gmx</b></i></font> or\n" +
                "    <font color=\"#800000\" size=\"4\"><i><b>gmt</b></i></font> or\n" +
                "    <font color=\"#800000\" size=\"4\"><i><b>grp</b></i></font>\n" +
                "    <p>&nbsp;&nbsp<b>Annotations</b>:\n" +
                "    <font color=\"#800000\" size=\"4\"><i><b>chip</b></i></font>\n" +
                "\n" +
                "</body>\n" +
                "\n" +
                "</html>");

        label.setBorder(BorderFactory.createTitledBorder("Supported file formats"));

        JButton bFormatHelp = new JButton(new BrowserAction("More on file formats ...",
                "Online documentation on supported data formats", GuiHelper.ICON_HELP16,
                GseaWebResources.getGseaDataFormatsHelpURL()));

        JPanel panel = new JPanel(new SRLayout(3, 10));

        JPanel pan = new JPanel(new GridBagLayout());
        GridBagConstraints gbc1 = new GridBagConstraints();
        gbc1.gridx = 0;
        gbc1.gridy = 0;
        gbc1.fill = GridBagConstraints.HORIZONTAL;
        gbc1.ipady = 5;
        gbc1.weightx = 0.5;
        gbc1.weighty = 0.5;
        pan.add(bBrowse, gbc1);

        GridBagConstraints gbc2 = new GridBagConstraints();
        gbc2.gridx = 0;
        gbc2.gridy = 1;
        gbc2.fill = GridBagConstraints.HORIZONTAL;
        gbc2.ipady = 5;
        gbc2.weightx = 0.5;
        gbc2.weighty = 0.5;
        pan.add(bLoadLast, gbc2);

        panel.add(pan);
        panel.add(createDndPanel());

        JPanel sub = new JPanel(new BorderLayout());
        sub.add(label, BorderLayout.NORTH);
        sub.add(bFormatHelp, BorderLayout.SOUTH);
        panel.add(sub);
        return panel;
    }

    private JPanel createDndPanel() {
        final MyTextArea ta = new MyTextArea();
        ta.setBackground(Color.LIGHT_GRAY);

        final JButton bClear = new JButton("Clear");
        bClear.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent evt) {
                ta.clear();
            }
        });

        final JButton bLoad = new JButton("Load these files!", JarResources.getIcon("Dnd2.gif"));
        bLoad.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent evt) {
                final File[] files = ta.getFiles();
                if (files.length == 0) {
                    Application.getWindowManager().showMessage("No files to import!\nDrag and drop files into the box and try again");
                } else {
                    new ParserWorker(files).execute();                
                }
            }
        });

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Method 3: drag and drop files here"));
        panel.add(new JScrollPane(ta), BorderLayout.CENTER);

        JPanel bp = new JPanel(new SRLayout(2, 10));
        bp.add(bClear);
        bp.add(bLoad);
        panel.add(bp, BorderLayout.SOUTH);
        return panel;
    }

    static class MyTextArea extends JTextArea implements DndTarget {
        private java.util.List fFiles;

        MyTextArea() {
            super();
            super.setEditable(false);
            new DropTargetDecorator(this);
            this.fFiles = new ArrayList();
        }

        public Component getDroppableIntoComponent() {
            return this;
        }

        public File[] getFiles() {
            Set files = new HashSet();
            for (int i = 0; i < fFiles.size(); i++) {
                Object it = fFiles.get(i);
                if (it instanceof File) {
                    files.add(it);
                }
            }

            return (File[]) files.toArray(new File[files.size()]);
        }

        void clear() {
            this.fFiles.clear();
            this.setText("");
        }

        public void setDropData(Object obj) {

            if (obj == null) {
                return;
            }

            final java.util.List list = (java.util.List) obj;

            for (int i = 0; i < list.size(); i++) {
                if (list.get(i) instanceof File) {
                    fFiles.add(list.get(i));
                }
            }

            StringBuffer buf = new StringBuffer();
            for (int i = 0; i < fFiles.size(); i++) {
                Object it = fFiles.get(i);
                if (it instanceof File) {
                    buf.append(((File) it).getName()).append('\n');
                }
            }

            this.setText(buf.toString());
        }

        // accepts one or mroe files dnd into it
        public DataFlavor[] getDroppableFlavors() {
            return new DataFlavor[]{DataFlavor.javaFileListFlavor};
        }

    }

    public JComponent getWrappedComponent() {
        return this;
    }

    public String getAssociatedTitle() {
        return TITLE;
    }

    public Icon getAssociatedIcon() {
        return ICON;
    }

    public JMenuBar getJMenuBar() {
        return Widget.EMPTY_MENU_BAR;
    }

    /**
     * Inner class for opening a file chooser and loading data from files
     */

    class FileOpenAction extends XAction {

        /**
         * Class constructor
         *
         * @param name
         */
        FileOpenAction() {
            super("FileOpenAction", "Browse for files ...", 
                    "Open a File and Load its Data into the Application", 
                    JarResources.getIcon("Open16.gif"));
        }

        public void actionPerformed(final ActionEvent evt) {

            final XFileChooser fcd = Application.getFileManager().getFileChooser(GseaAppConf.createAllFileFilters());
            final boolean proceed = fcd.showOpenDialog();
            if (proceed) {
                final File[] files = fcd.getSelectedFiles();
                new ParserWorker(files).execute();

                // TODO: is this needed??
                for (int i = 0; i < files.length; i++) {
                    if (!AuxUtils.isAuxFile(files[i])) {
                        Application.getFileManager().registerRecentlyOpenedFile(files[i]);
                    }
                }
            }
        }
    }    // End FileOpenAction

    class LoadLastAnalysisFilesAction extends XAction {

        /**
         * Class constructor
         *
         * @param name
         */
        LoadLastAnalysisFilesAction() {
            super("LoadLastAction", "Load last dataset used", 
                    "Load datasets used the last time GSEA was run", 
                    JarResources.getIcon("History16_v2.gif"));
        }

        public void actionPerformed(ActionEvent e) {
            SwingWorker<Object, Void> worker = new SwingWorker<Object, Void>() {
                @Override
                protected Object doInBackground() throws Exception {
                    final ReportStub rs = Application.getToolManager().getLastReportStub(new Gsea().getName());

                    if (rs == null) {
                        Application.getWindowManager().showMessage("No history available, nothing loaded!");
                        return null;
                    }

                    // OK, go for it
                    try {
                        final Tool tool = TaskManager.createTool(rs.getReport(true).getProducer().getName());
                        final Properties source_params = rs.getReport(true).getParametersUsed();

                        Runnable runnable = ToolRunnerControl.createLoadToolTask(tool,
                                rs.getName(), true, source_params, fInstance, false);

                        Thread t = new Thread(runnable);
                        t.setPriority(Thread.MIN_PRIORITY);
                        t.start();

                        Application.getWindowManager().showMessage("Data from the last run of this tool is being automagically loaded. They will soon be available as parameter options");

                    } catch (Throwable t) {
                        Application.getWindowManager().showError(t);
                    }

                    return null;
                }
            };
            worker.execute();
        }
    }
}