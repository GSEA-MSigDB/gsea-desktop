/*
 * Copyright (c) 2003-2024 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xapps.gsea;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.WindowConstants;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.broad.gsea.ui.DesktopIntegration;
import org.jfree.ui.about.AboutPanel;

import com.jgoodies.looks.HeaderStyle;
import com.jgoodies.looks.Options;

import edu.mit.broad.cytoscape.action.EnrichmentMapInputPanelAction;
import edu.mit.broad.genome.Conf;
import edu.mit.broad.genome.JarResources;
import edu.mit.broad.genome.swing.GuiHelper;
import edu.mit.broad.genome.swing.ImageComponent;
import edu.mit.broad.genome.swing.SystemConsole;
import edu.mit.broad.xbench.actions.ShowAppRuntimeHomeDirAction;
import edu.mit.broad.xbench.actions.ShowDefaultOutputDirAction;
import edu.mit.broad.xbench.actions.XAction;
import edu.mit.broad.xbench.actions.ext.BrowserAction;
import edu.mit.broad.xbench.core.WrappedComponent;
import edu.mit.broad.xbench.core.api.Application;
import edu.mit.broad.xbench.core.api.FileManager;
import edu.mit.broad.xbench.core.api.ToolManager;
import edu.mit.broad.xbench.core.api.ToolManagerImpl;
import edu.mit.broad.xbench.core.api.VdbManager;
import edu.mit.broad.xbench.core.api.WindowManager;
import edu.mit.broad.xbench.prefs.XPreferencesFactory;
import edu.mit.broad.xbench.tui.TaskManager;
import xapps.api.AppDataLoaderAction;
import xapps.api.AppToolLauncherAction;
import xapps.api.PastAnalysisAction;
import xapps.api.frameworks.WorkspaceToolBar;
import xapps.api.frameworks.fiji.StatusBarAppender;
import xapps.api.frameworks.fiji.WindowManagerImplJideTabbedPane;
import xtools.api.Tool;
import xtools.chip2chip.Chip2Chip;
import xtools.gsea.Gsea;
import xtools.gsea.GseaPreranked;
import xtools.munge.CollapseDataset;

public class GseaFijiTabsApplicationFrame extends JFrame implements Application.Handler {
    public static final Properties buildProps = JarResources.getBuildInfo();
    
    static {
        System.setProperty("GSEA", Boolean.TRUE.toString()); // needed for vdb manager to work properly
        if (StringUtils.isBlank(buildProps.getProperty("build.version"))) buildProps.setProperty("build.version", "[NO BUILD VERSION FOUND]");
        if (StringUtils.isBlank(buildProps.getProperty("build.number"))) buildProps.setProperty("build.number", "Error loading build.properties!");
    }

    private static final int INITIAL_LHS_WIDTH = 200;

    public static final String RPT_CACHE_BUILD_DATE = "April4_2006_build";

    private static String USER_VISIBLE_FRAME_TITLE = "GSEA " + buildProps.getProperty("build.version")
            + " (Gene set enrichment analysis)";

    private static final List<Image> FRAME_ICONS = Arrays.asList(
        JarResources.getImage("icon_16x16.png"),
        JarResources.getImage("icon_16x16@2x.png"),
        JarResources.getImage("icon_32x32.png"),
        JarResources.getImage("icon_32x32@2x.png"),
        JarResources.getImage("icon_128x128.png"),
        JarResources.getImage("icon_128x128@2x.png"),
        JarResources.getImage("icon_256x256.png"),
        JarResources.getImage("icon_256x256@2x.png"),
        JarResources.getImage("icon_512x512.png"),
        JarResources.getImage("icon_512x512@2x.png"));

    // Icon for the Preferences
    private static final Icon PREF_ICON = JarResources.getIcon("Preferences16.gif");

    // @note IMP IMP: this is the name under which docking prefs etc are stored
    // TODO: remove this, but first figure out if we can extract these for our
    // own use.
    public static final String PROFILE_NAME = "gsea";

    private StatusBarAppender fStatusBarAppender;

    private GseaFijiTabsApplicationFrame fFrame = this;

    private WindowAdapter fWindowListener;

    private MyWindowManagerImplJideTabbedPane fWindowManager;
    
    public GseaFijiTabsApplicationFrame() {
        super(USER_VISIBLE_FRAME_TITLE);

        fFrame.setVisible(false);

        fFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE); // we catch and ask
        fFrame.setIconImages(FRAME_ICONS);

        // add a widnow listener to do clear up when windows closing.
        fWindowListener = new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                exitApplication();
            }
        };

        if (SystemUtils.IS_OS_MAC_OSX) {
            try {
                DesktopIntegration.setAboutHandler(this);
                DesktopIntegration.setQuitHandler(this);
            } catch (Exception e) {
                e.printStackTrace(System.out);
                System.out.println("Error setting apple-specific about and quit handlers");
            }
        }

        fFrame.addWindowListener(fWindowListener);
        this.fWindowManager = new MyWindowManagerImplJideTabbedPane();
        this.fStatusBarAppender = SystemConsole.createStatusBarAppender("StatusBar");
        Application.registerHandler(this);
        fFrame.setJMenuBar(createMenuBar());
        jbInit();
    }

    public void showAboutDialog() {
        String buildInfo = formatBuildInfoForHelp();
        String buildTS = formatBuildTimestampForHelp();
        if (buildTS != null) {
            buildInfo += ", " + buildTS;
        }
        AboutPanel aboutPanel = new AboutPanel("Gene Set Enrichment Analysis (GSEA) v" + buildProps.getProperty("build.version"), 
                "Copyright (c) 2003-2025 Broad Institute, Inc., Massachusetts Institute of Technology, ",
                "and Regents of the University of California.  All rights reserved.", 
                buildInfo);
        JOptionPane.showMessageDialog(fFrame, aboutPanel, "About GSEA", JOptionPane.PLAIN_MESSAGE);
    }

    // TODO: can we save size, position for reuse on next launch?
    // Will probably use the Preferences API.
    // Should check if saved position is out of bounds for the current screen.  Likewise for size.
    // That is, would their use result in an unusable desktop?
    public void makeVisible() {
        int lastHeight = XPreferencesFactory.kAppHeight.getInt();
        int lastWidth = XPreferencesFactory.kAppWidth.getInt();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        if (lastHeight > screenSize.height || lastWidth > screenSize.width) {
            // If *either* the last height or width is larger than the current screen allows, 
            // use a smaller default size reasonable for this screen
            fFrame.setSize(screenSize.width * 3 / 4, screenSize.height * 3 / 4);
        } else {
            fFrame.setSize(lastWidth, lastHeight);
        }
        int lastX = XPreferencesFactory.kAppXPosition.getInt();
        int lastY = XPreferencesFactory.kAppYPosition.getInt();
        if (lastX > screenSize.width || lastX < 0 || lastY > screenSize.height || lastY < 0) {
            // If the last window position would be out-of-bounds for the current screen,
            // use a position reasonable for this screen
            fFrame.setLocation((screenSize.width - getWidth()) / 2, (screenSize.height - getHeight()) / 2);
        } else {
            fFrame.setLocation(lastX, lastY);
        }
        fFrame.setVisible(true);
        if (XPreferencesFactory.kAppMaximized.getBoolean()) {
            this.setExtendedState(this.getExtendedState() | JFrame.MAXIMIZED_BOTH);
        }
        
        fFrame.toFront();
    }

    private AppToolLauncherAction fGseaTool_launcher;
    private AppDataLoaderAction fAppDataLoaderAction;
    private Gsea fGseaTool;

    // contains routines that make future displays faster
    public void init_bg_while_splashing() {
        Application.getFileManager();
        fAppDataLoaderAction.getWidget(); // init it
        this.fGseaTool_launcher.getWidget();
        System.out.println("Done initing things while splashing");
    }

    private void jbInit() {
        fFrame.getContentPane().setLayout(new BorderLayout());

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.add(createToolBar());
        split.setContinuousLayout(false);
        split.add(fWindowManager.getTabbedPane());

        fWindowManager.openWindow(createStartupPanel());
        fFrame.getContentPane().add(split);

        // create one project tab for current project
        fFrame.getContentPane().add(fStatusBarAppender.getAsComponent(fFrame), BorderLayout.AFTER_LAST_LINE);
        UpdateChecker.oneTimeGseaUpdateCheck(this);
    }

    private JComponent createToolBar() {
        WorkspaceToolBar tb1 = new WorkspaceToolBar(JToolBar.VERTICAL);

        tb1.setFloatable(false);
        tb1.setBorder(BorderFactory.createTitledBorder("Steps in GSEA analysis"));

        // rhs buttons
        int width = 75;
        int height = 55;
        Font font = new Font("Arial", Font.BOLD, 12);

        int struth = 12;

        // put the main thing first

        fAppDataLoaderAction = new AppDataLoaderAction();

        fGseaTool = new Gsea();
        this.fGseaTool_launcher = new AppToolLauncherAction(fGseaTool, fGseaTool.getParamSet(),
                "Run Gsea", JarResources.getIcon("Gsea_app16_v2.png"));

        tb1.add(JarResources.toURL("LocalFileExplorerWidget32.gif"), "Load data          ",
                fAppDataLoaderAction, width, height, font, true);
        tb1.add(Box.createVerticalStrut(struth));

        tb1.add(JarResources.toURL("GseaApp24.gif"), "Run GSEA           ",
                fGseaTool_launcher, width, height, font, true);
        tb1.add(Box.createVerticalStrut(struth));

        tb1.add(JarResources.toURL("Lev32.gif"), "Leading edge analysis", new LeadingEdgeReportAction(), width, height, font, true);
        tb1.add(Box.createVerticalStrut(struth));
        
        //Add Enrichment Map analysis to GSEA steps
        tb1.add(JarResources.toURL("enrichmentmap_logo.gif"), "Enrichment Map Visualization", new EnrichmentMapInputPanelAction(),width, height, font,true);
        tb1.add(Box.createVerticalStrut(struth));
        
        WorkspaceToolBar tb2 = new WorkspaceToolBar(JToolBar.VERTICAL);
        tb2.setFloatable(false);
        tb2.setBorder(BorderFactory.createTitledBorder("Tools"));
        
        final GseaPreranked gsea_tool = new GseaPreranked();
        final CollapseDataset cd = new CollapseDataset();
        Tool fChip2ChipTool = new Chip2Chip();

        tb2.add(JarResources.toURL("GseaApp24.gif"), "Run GSEAPreranked  ", 
                new AppToolLauncherAction(gsea_tool, gsea_tool.getParamSet(),
                        "Run Gsea on a Pre-Ranked gene list", JarResources.getIcon("Gsea_app16_v2.png")
                ), 
                width, height, font, true);
        tb2.add(Box.createVerticalStrut(struth));
        
        tb2.add(JarResources.toURL("ProjectSpecific16.png"), "Collapse Dataset   ", 
                new AppToolLauncherAction(cd, cd.getParamSet(),
                        "Collapse Dataset from Probes to Symbols", JarResources.getIcon("ProjectSpecific16.png")
                ), 
                width, height, font, true);
        tb2.add(Box.createVerticalStrut(struth));
        tb2.add(JarResources.toURL("Chip2Chip24_b.gif"), "Chip2Chip mapping",
                new AppToolLauncherAction(fChip2ChipTool, fChip2ChipTool.getParamSet(),
                        "Chip2Chip", JarResources.getIcon("Chip2Chip16.gif")), width, height, font, true);
        tb2.add(Box.createVerticalStrut(struth));

        WorkspaceToolBar tb3 = new WorkspaceToolBar(JToolBar.VERTICAL);
        tb3.setBorder(BorderFactory.createEtchedBorder());
        tb3.setFloatable(false);

        tb3.add(JarResources.toURL("past_analysis32.gif"), "Analysis history", new PastAnalysisAction(), width, height, font, true);
        tb3.add(Box.createVerticalStrut(struth));

        WorkspaceToolBar tb = new WorkspaceToolBar(JToolBar.VERTICAL);
        tb.setFloatable(false);
        tb.add(tb1);
        tb.add(tb2);
        tb.add(tb3);

        JSplitPane pane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        pane.add(tb);
        pane.add(createProcessForToolBar());

        return pane;
    }

    private WrappedComponent createStartupPanel() {
        final JScrollPane sp = new JScrollPane(new ImageComponent(JarResources.getImage("intro_screen.jpg"), false));

        return new WrappedComponent() {
            public JComponent getWrappedComponent() {
                return sp;
            }

            public String getAssociatedTitle() {
                return "Home";
            }

            public Icon getAssociatedIcon() {
                return null;
            }
        };
    }

    private JComponent createProcessForToolBar() {
        TaskManager tm = TaskManager.getInstance();
        final JTable taskTable = tm.createTable();
        tm.setOnClickShowResultsInBrowserOnly(true); // @note

        JScrollPane sp = new JScrollPane(taskTable);
        sp.setPreferredSize(new Dimension(INITIAL_LHS_WIDTH, 350));

        final JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(new JLabel("<html><body>Processes: click 'status' field for results</body></html>"), BorderLayout.NORTH);
        leftPanel.setBackground(Color.LIGHT_GRAY);
        leftPanel.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                leftPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }

            public void mouseExited(MouseEvent e) {
                leftPanel.setCursor(Cursor.getDefaultCursor());
            }
        });

        taskTable.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                taskTable.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }

            public void mouseExited(MouseEvent e) {
                taskTable.setCursor(Cursor.getDefaultCursor());
            }
        });

        leftPanel.setBorder(BorderFactory.createTitledBorder("GSEA reports"));

        leftPanel.add(sp, BorderLayout.CENTER);

        JButton but = new JButton("Show results folder");
        but.setAction(new ShowDefaultOutputDirAction("Show results folder"));
        but.setRolloverEnabled(true);
        JPanel showResButPanel = new JPanel();
        BoxLayout layout = new BoxLayout(showResButPanel, BoxLayout.X_AXIS);
        showResButPanel.add(but);
        leftPanel.add(showResButPanel, BorderLayout.SOUTH);
        return leftPanel;
    }

    /**
     * @return
     * @note Placing this here rather in ActionFactory as a developing aid
     * - > quicker launch of XReg isnt initialized at startup
     * The applications menu bar is defined here.
     * @maint if new actions are added -> need to review to see if they should also
     * be added to the menu bar.
     */
    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // @note JGOODIES SUGGESTIONS
        menuBar.putClientProperty(Options.HEADER_STYLE_KEY, HeaderStyle.SINGLE);

        menuBar.add(createJMenu("File", new Object[]{
                new GseaPreferencesAction(),
                null,
                new ClearFileHistoryAction(),
                null,
                SystemUtils.IS_OS_MAC_OSX ? null : new MyExitAction()
        }));

        menuBar.add(createJMenu("Downloads", new Object[]{
                new BrowserAction("Download Human chip annotations", "Download Human array annotation files - useful to annotate GSEA reports",
                        null, GseaWebResources.getHumanArrayAnnotationsURL()),
                new BrowserAction("Download Mouse chip annotations", "Download Mouse array annotation files - useful to annotate GSEA reports",
                        null, GseaWebResources.getMouseArrayAnnotationsURL()),
                new BrowserAction("Download example datasets", "Download example datasets for GSEA - expression data, phenotype labels and gene sets files",
                        null, GseaWebResources.getGseaExamplesURL())}));
        
        menuBar.add(createJMenu("Help", new Object[]{
                new BrowserAction("GSEA web site", "Open the GSEA website in a web browser",
                        GuiHelper.ICON_HELP16, GseaWebResources.getGseaBaseURL()),
                new BrowserAction("GSEA documentation", "Online documentation of the GSEA algorithm and software",
                        GuiHelper.ICON_HELP16, GseaWebResources.getGseaHelpURL()),
                new BrowserAction("GSEA & MSigDB License Terms", "GSEA & MSigDB License Terms", GuiHelper.ICON_HELP16, 
                        GseaWebResources.getGseaBaseURL() + "/" + "license_terms_list.jsp"),
                        null,
                new ShowAppRuntimeHomeDirAction("Show GSEA home folder"),
                new ShowDefaultOutputDirAction("Show GSEA output folder (default location)"),
                null,
                new BrowserAction("Contact Us", "Contact Us", null, GseaWebResources.getGseaContactURL()),
                null,
                formatBuildInfoForHelp(),
                formatBuildTimestampForHelp()
        }));

        return menuBar;
    }

    private String formatBuildInfoForHelp() {
        String buildVer = buildProps.getProperty("build.version");
        String buildNum = buildProps.getProperty("build.number");
        String buildInfo = "GSEA v" + buildVer + " [build: " + buildNum + "]";
        return buildInfo;
    }
    
    private String formatBuildTimestampForHelp() {
        String buildTS = buildProps.getProperty("build.timestamp");
        if (StringUtils.isBlank(buildTS)) return null; 
        return "Built: " +  buildTS;
    }
    
    class ClearFileHistoryAction extends XAction {
        public ClearFileHistoryAction() {
            super("ClearAction", "Clear recent file history", "Clear recent file history (from the 'Load Data' panel)");
        }

        public void actionPerformed(final ActionEvent evt) {
            boolean res = Application.getWindowManager().showConfirm("Clear file history", "Are you sure you want to erase all file history");
            if (res) {
                Application.getFileManager().getRecentFilesStore().clearAll();
                if (fAppDataLoaderAction != null) {
                    fAppDataLoaderAction.getWidget();
                }
            }
        }
    }

    /**
     * @param name
     * @param objs -> array of Action objects interspersed with nulls wherever
     *             a seperator is needed.
     *             For example new Object[]{Foo, Bar, null, Zoo};
     * @return
     */
    private JMenu createJMenu(final String name, final Object[] objs) {

        JMenu menu = new JMenu(name, true);    // true -> can tear off

        for (int i = 0; i < objs.length; i++) {
            if (objs[i] == null) {
                menu.addSeparator();
            } else {
                if (objs[i] instanceof JMenuItem) {
                    menu.add((JMenuItem) objs[i]);
                } else if (objs[i] instanceof String) {
                    menu.add(objs[i].toString());
                } else {
                    menu.add(new JMenuItem((Action) objs[i]));
                }
            }
        }

        return menu;
    }
    
    public boolean exitApplication() {
        boolean ask = XPreferencesFactory.kAskBeforeAppShutdown.getBoolean();
        if (ask) {
            final boolean res = getWindowManager().showConfirm("Exit the application?");
            if (!res) {
                return false;
            }
        }

        fFrame.removeWindowListener(fWindowListener);
        fWindowListener = null;

        // save current position, size (etc) before exit
        try {
            XPreferencesFactory.kAppHeight.setValue(this.getHeight());
            XPreferencesFactory.kAppWidth.setValue(this.getWidth());
            XPreferencesFactory.kAppXPosition.setValue(this.getX());
            XPreferencesFactory.kAppYPosition.setValue(this.getY());
            XPreferencesFactory.kAppMaximized.setValue(this.getExtendedState() == JFrame.MAXIMIZED_BOTH);
            XPreferencesFactory.save();
        } catch (Exception e) {
            e.printStackTrace(System.out);
            System.out.println("Error saving window size and position");
        }
        
        fFrame.dispose();
        fFrame = null;
        if (Conf.isDebugMode() == false) {
            Conf.exitSystem(false);
        }
        return true;
    }

    /**
     * For gsea preferences
     */
    class GseaPreferencesAction extends AbstractAction {
        private JCheckBoxMenuItem fAsk;
        private JCheckBoxMenuItem fOnline;
        private JCheckBoxMenuItem fMedian;
        private JCheckBoxMenuItem fFix;
        private JCheckBoxMenuItem fBiased;

        public GseaPreferencesAction() {
            this.putValue(Action.NAME, getName());
            this.putValue(Action.SMALL_ICON, getAssociatedIcon());
            this.putValue(Action.SHORT_DESCRIPTION, getDescription());

            fAsk = XPreferencesFactory.createCheckBoxMenuItem(XPreferencesFactory.kAskBeforeAppShutdown);
            fOnline = XPreferencesFactory.createCheckBoxMenuItem(XPreferencesFactory.kOnlineMode);
            fMedian = XPreferencesFactory.createCheckBoxMenuItem(XPreferencesFactory.kMedian);
            fFix = XPreferencesFactory.createCheckBoxMenuItem(XPreferencesFactory.kFixLowVar);
            fBiased = XPreferencesFactory.createCheckBoxMenuItem(XPreferencesFactory.kBiasedVar);
        }

        public void actionPerformed(final ActionEvent evt) {
            // make this new every time so that the GUI doesn't cache settings
            // (i.e force read from prefs)

            GseaPreferencesDialog opt = new GseaPreferencesDialog(fFrame, "Preferences");
            opt.pack();
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            opt.setLocation((screenSize.width - getWidth()) / 2, (screenSize.height - getHeight()) / 2);
            opt.setVisible(true);

            // @note hack to get the menu in synch with the prefs. Lazy to make a listener scheme
            if (fAsk != null) {
                fAsk.setSelected(XPreferencesFactory.kAskBeforeAppShutdown.getBoolean());
                fOnline.setSelected(XPreferencesFactory.kOnlineMode.getBoolean());
                fMedian.setSelected(XPreferencesFactory.kMedian.getBoolean());
                fFix.setSelected(XPreferencesFactory.kFixLowVar.getBoolean());
                fBiased.setSelected(XPreferencesFactory.kBiasedVar.getBoolean());
            }
        }

        public String getName() {
            return "Preferences ...";
        }

        public Icon getAssociatedIcon() {
            return PREF_ICON;
        }

        public String getDescription() {
            return "View and modify application wide preferences";
        }
    }

    class MyWindowManagerImplJideTabbedPane extends WindowManagerImplJideTabbedPane {
        MyWindowManagerImplJideTabbedPane() {
            super(fFrame);
        }

        public JPopupMenu createPopupMenu(final Object obj) {
            return GseaActionRegistry.getInstance().createPopup(obj);
        }

        // simply import
        public boolean runDefaultAction(final Object obj) {
            return GseaActionRegistry.getInstance().runDefaultAction(obj);
        }
    }

    class MyExitAction extends XAction {
        public MyExitAction() {
            super("ExitAction", "Exit", "Quit the GSEA application");
        }

        public void actionPerformed(final ActionEvent evt) {
            exitApplication();
        }
    }

    private static final VdbManager fVdbmanager = new VdbManagerForGsea(RPT_CACHE_BUILD_DATE);

    private ToolManagerImpl fToolManager;
    private FileManager fFileManager;

    public String getName() {
        return "GSEA";
    }

    public ToolManager getToolManager() {
        if (fToolManager == null) {
            this.fToolManager = new ToolManagerImpl();
        }

        return fToolManager;
    }

    public FileManager getFileManager() {
        if (fFileManager == null) {
            this.fFileManager = new FileManager();
        }

        return fFileManager;
    }

    public VdbManager getVdbManager() {
        return fVdbmanager;
    }

    public WindowManager getWindowManager() throws HeadlessException {
        return fWindowManager;
    }

    public StatusBarAppender getStatusBarAppender() {
        return fStatusBarAppender;
    }
}
