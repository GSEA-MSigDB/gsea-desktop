/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.cytoscape.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;


import xapps.api.vtools.ParamSetFormForAFew;
import xtools.api.param.DirParam;
import xtools.api.param.Param;
import xtools.api.param.ReportCacheChooserParam;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jidesoft.grid.SortableTable;
import com.jidesoft.swing.JideTabbedPane;

import edu.mit.broad.cytoscape.CytoscapeLaunch;
import edu.mit.broad.genome.JarResources;
import edu.mit.broad.genome.objects.esmatrix.db.EnrichmentDb;
import edu.mit.broad.genome.swing.GuiHelper;
import edu.mit.broad.genome.swing.fields.GDirFieldPlusChooser;
import edu.mit.broad.genome.swing.fields.GFieldPlusChooser;
import edu.mit.broad.genome.viewers.AbstractViewer;
import edu.mit.broad.xbench.core.api.Application;
import foxtrot.Task;
import foxtrot.Worker;

public class EnrichmentMapInputPanel extends AbstractViewer {
	public static final String NAME = "EnrichmentMapVisualizationWidget";

    public static final Icon ICON = JarResources.getIcon("enrichmentmap_logo.png");

    private ReportCacheChooserParam fReportParam;

    private DirParam fDirParam;

    private SortableTable sortableTable;

    private File curr_gseaResultDir;

    private EnrichmentDb curr_edb;

    private JComponent fFiller;

    private Component tableComponent;

    private EnrichmentMapInputPanel fInstance = this;

    /**
     * Builds a Viewer on specified FSet object
     *
     * @param gset
     */
    public EnrichmentMapInputPanel() {
        super(NAME, ICON, "Enrichment Map Visualization");
        jbInit();
      
        //launch cytoscape rest service
      	CytoscapeLaunch cyto = new CytoscapeLaunch();
      	cyto.launch();
    }


    private JideTabbedPane sharedTabbedPane;

    private void jbInit() {
    	JPanel buttons = new JPanel();
    	JButton bClear = new JButton("Clear");
    	bClear.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
            	
            	((GFieldPlusChooser) fReportParam.getSelectionComponent()).setValue("");
            	((GDirFieldPlusChooser) fDirParam.getSelectionComponent()).getTextField().setText("");

            }
        });
        JButton bBuild = new JButton("Load GSEA Results");

        bBuild.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                if (fReportParam.isSpecified() && fDirParam.isSpecified()) {
                    Application.getWindowManager().showMessage("Both cache and a brows'ed directory were specified. Only 1 can be specified. Delete one and try again");
                } else if (!fReportParam.isSpecified() && !fDirParam.isSpecified()) {
                    Application.getWindowManager().showMessage("No GSEA result folder was specified. Specify one and try again");
                } else {
                    try {
                        Worker.post(new Task() {
                            public Object run() throws Exception {

                                Application.getWindowManager().getRootFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                                String current_results = "";
                                if (fReportParam.isSpecified()) {
                                    
                                    current_results = fReportParam.getValue().toString();
                                    //check to see if there are multiple directories
                                    //Can load however many you want but can only do a two dataset analysis
                                                                                                                                            
                                    
                                } else {
                                		current_results = fDirParam.getValue().toString();
                                }
                                
                                String[] datasets = current_results.split(",");
                                
                                //curr_edb = edb;

                                boolean first = false;
                                if (sharedTabbedPane == null) {
                                    first = true;
                                    sharedTabbedPane = new JideTabbedPane();
                                    sharedTabbedPane.setHideOneTab(true);
                                    sharedTabbedPane.setTabEditingAllowed(true);
                                    sharedTabbedPane.setShowCloseButtonOnTab(true);

                                }
                                //add this analysis to tab
                                try {
                                		EnrichmentMapParameterPanel new_panel = new EnrichmentMapParameterPanel(datasets);
                                		JScrollPane scrollablePanel = new JScrollPane(new_panel);
                                		sharedTabbedPane.addTab("EM Analysis", scrollablePanel);                                
                                		sharedTabbedPane.setSelectedComponent(scrollablePanel);
                                } catch (Throwable t) {
                                		System.out.println("unable to initialize interface");
                                }


                                if (first) {
                                    fInstance.remove(fFiller);
                                    fInstance.add(sharedTabbedPane, BorderLayout.CENTER);
                                    //sharedTabbedPane.setTabClosableAt(0, false);
                                }


                                fInstance.revalidate();

                                return null;

                            }
                        });
                    } catch (Throwable t) {
                        Application.getWindowManager().showError("Trouble loading enrichment database", t);
                    } finally {
                        Application.getWindowManager().getRootFrame().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    }
                }
            }
        });

        this.fReportParam = new ReportCacheChooserParam("Select a GSEA result from the application cache", false);
        this.fDirParam = new DirParam("dir", "[ OR ] Locate a GSEA result folder from the file system", "[ OR ] Locate a GSEA report folder from the file system", false);
        final Param[] params = new Param[]{fReportParam, fDirParam};

        // -------------------------------------------------------------------------------------------- //
        StringBuffer colStr = _createColStr();
        StringBuffer rowStr = _createRowStr(params);

        PanelBuilder builder = ParamSetFormForAFew.createPanelBuilder(colStr, rowStr);
        CellConstraints cc = new CellConstraints();

        int rowcnt = 3;
        for (int i = 0; i < params.length; i++) {
            GFieldPlusChooser chooser = params[i].getSelectionComponent();

            if (params[i].isFileBased()) {
                if (chooser instanceof GDirFieldPlusChooser) {
                    (((GDirFieldPlusChooser) chooser)).getTextField().setBackground(ParamSetFormForAFew.LIGHT_GREEN);
                }
            }

            JLabel label = new JLabel(params[i].getHtmlLabel_v3());

            ParamSetFormForAFew.enableToolTips(label, params[i]);
            builder.add(label, cc.xy(1, rowcnt));
            builder.add(chooser.getComponent(), cc.xy(3, rowcnt));
            rowcnt += 2; // because the spaces also count as a row
        }

        builder.add(new JLabel(""), cc.xy(1, rowcnt));
        //add the two buttons to the buttons panel
        buttons.add(bClear);
        buttons.add(bBuild);
        
        builder.add(buttons, cc.xy(3, rowcnt));

        final JPanel paramPanel = builder.getPanel();
        // -------------------------------------------------------------------------------------------- //

        this.setLayout(new BorderLayout(10, 10));
        this.add(paramPanel, BorderLayout.NORTH);

        this.fFiller = GuiHelper.createWaitingPlaceholder();
        this.add(fFiller, BorderLayout.CENTER);

        //this.add(createControlPanel(), BorderLayout.SOUTH);

        this.revalidate();
    }


    public String[] getSelectedResultNames() {
        final int[] rows = sortableTable.getSelectedRows();
        final String[] names = new String[rows.length];
        for (int i = 0; i < rows.length; i++) {
            names[i] = sortableTable.getModel().getValueAt(rows[i], 1).toString();
        }

        return names;
    }
    private static StringBuffer _createColStr() {
        return new StringBuffer("220dlu,      4dlu,        200dlu,   4dlu,  4dlu"); // columns
    }

    private static StringBuffer _createRowStr(final Param[] params) {
        StringBuffer rowStr = new StringBuffer();
        rowStr.append("pref, 10dlu,"); // for the spacer
        for (int i = 0; i < params.length + 1; i++) { // +1 for the button
            rowStr.append("pref, 5dlu");
            if (params.length != i - 1) {
                rowStr.append(",");
            }
        }
        return rowStr;
    }


    public JMenuBar getJMenuBar() {
        return EMPTY_MENU_BAR;
    }

}
