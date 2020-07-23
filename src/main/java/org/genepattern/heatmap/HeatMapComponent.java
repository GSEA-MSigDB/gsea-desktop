/*
 * Copyright (c) 2003-2020 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package org.genepattern.heatmap;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.CellConstraints.Alignment;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import com.jidesoft.plaf.xerto.VerticalLabelUI;

import edu.mit.broad.xbench.core.api.Application;

import org.apache.commons.lang3.SystemUtils;
import org.genepattern.annotation.*;
import org.genepattern.data.expr.ExpressionConstants;
import org.genepattern.data.expr.ExpressionData;
import org.genepattern.data.expr.IExpressionData;
import org.genepattern.data.expr.Util;
import org.genepattern.data.matrix.IExpressionDataUtil;
import org.genepattern.heatmap.image.DisplaySettings;
import org.genepattern.heatmap.image.FeatureAnnotator;
import org.genepattern.heatmap.image.HeatMap;
import org.genepattern.heatmap.image.SampleAnnotator;
import org.genepattern.io.ImageUtil;
import org.genepattern.menu.FileMenu;
import org.genepattern.menu.FindAction;
import org.genepattern.menu.PlotAction;
import org.genepattern.module.VisualizerUtil;
import org.genepattern.plot.ProfilePlot;
import org.genepattern.table.GPTable;
import org.genepattern.uiutil.CenteredDialog;
import org.genepattern.uiutil.UIUtil;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * @author Joshua Gould
 */
public class HeatMapComponent extends JComponent {
    private static class OptionsDialog extends CenteredDialog {

        public OptionsDialog(final Frame parent,
                             final HeatMapComponent heatMapComponent,
                             final HeatMapPanel heatMapPanel, boolean showColumnNames,
                             boolean showRowNames, boolean showRowDescriptions) {
            super(parent);
            setTitle("Options");
            setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            FormLayout fl = new FormLayout("pref", "");
            CellConstraints cc = new CellConstraints();

            JPanel optionsPanel = new JPanel(fl);

            fl.appendRow(new RowSpec("pref"));

            JPanel p1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JLabel colorSchemeMenu = new JLabel("Color Scheme: ");
            p1.add(colorSchemeMenu);

            final JRadioButton rowButton = new JRadioButton("Relative");
            rowButton.setSelected(heatMapPanel.getNormalization() == HeatMapPanel.NORMALIZATION_ROW);
            p1.add(rowButton);
            final JRadioButton globalButton = new JRadioButton("Global");
            globalButton.setSelected(heatMapPanel.getNormalization() == HeatMapPanel.NORMALIZATION_GLOBAL);
            p1.add(globalButton);
            ButtonGroup bg = new ButtonGroup();
            bg.add(rowButton);
            bg.add(globalButton);
            if (heatMapComponent.showColorSchemeOptions) {
                optionsPanel.add(p1, cc.xy(1, fl.getRowCount()));
            }

            fl.appendRow(new RowSpec("pref"));
            JPanel p2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
            final JCheckBox showGridCheckBox = new JCheckBox("Show Grid");
            showGridCheckBox.setSelected(heatMapPanel.isDrawGrid());
            p2.add(showGridCheckBox);
            optionsPanel.add(p2, cc.xy(1, fl.getRowCount()));

            fl.appendRow(new RowSpec("pref"));
            JPanel p3b = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JLabel gridLabel = new JLabel("Grid Size: ");
            final JSlider gridSizeSlider = new JSlider(2, 30, heatMapPanel.getRowSize());
            p3b.add(gridLabel);
            p3b.add(gridSizeSlider);
            final JTextField gridSizeTextField = new JTextField(String
                    .valueOf(heatMapPanel.getRowSize()) + "  ");
            p3b.add(gridSizeTextField);
            optionsPanel.add(p3b, cc.xy(1, fl.getRowCount()));

            fl.appendRow(new RowSpec("pref"));
            JPanel p3 = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JLabel rowSizeLabel = new JLabel("Row Size: ");
            final JTextField rowSizeTextField = new JTextField(10);
            rowSizeTextField.setText(String.valueOf(heatMapPanel.getRowSize()));
            p3.add(rowSizeLabel);
            p3.add(rowSizeTextField);
            // optionsPanel.add(p3, cc.xy(1, fl.getRowCount()));

            fl.appendRow(new RowSpec("pref"));
            JPanel p4 = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JLabel columnSizeLabel = new JLabel("Column Size: ");
            final JTextField columnSizeTextField = new JTextField(10);
            columnSizeTextField.setText(String.valueOf(heatMapPanel
                    .getColumnSize()));
            p4.add(columnSizeLabel);
            p4.add(columnSizeTextField);
            // optionsPanel.add(p4, cc.xy(1, fl.getRowCount()));

            final JCheckBox showFeatureDescriptionsCheckBox = new JCheckBox(
                    "Show " + heatMapComponent.featureUIString
                            + " Descriptions");
            showFeatureDescriptionsCheckBox.setSelected(heatMapComponent
                    .isShowRowDescriptions());
            if (showRowDescriptions) {
                fl.appendRow(new RowSpec("pref"));
                JPanel p5 = new JPanel(new FlowLayout(FlowLayout.LEFT));
                p5.add(showFeatureDescriptionsCheckBox);
                optionsPanel.add(p5, cc.xy(1, fl.getRowCount()));
            }

            final JCheckBox showRowNamesCheckBox = new JCheckBox("Show "
                    + heatMapComponent.featureUIString + " Names");
            showRowNamesCheckBox.setSelected(heatMapComponent.isShowRowNames());
            if (showRowNames) {
                fl.appendRow(new RowSpec("pref"));
                JPanel p6 = new JPanel(new FlowLayout(FlowLayout.LEFT));
                p6.add(showRowNamesCheckBox);
                optionsPanel.add(p6, cc.xy(1, fl.getRowCount()));
            }

            final JCheckBox showColumnNamesCheckBox = new JCheckBox("Show "
                    + heatMapComponent.sampleUIString + " Names");
            showColumnNamesCheckBox.setSelected(heatMapComponent
                    .isShowColumnNames());
            if (showColumnNames) {
                fl.appendRow(new RowSpec("pref"));
                JPanel p7 = new JPanel(new FlowLayout(FlowLayout.LEFT));
                p7.add(showColumnNamesCheckBox);
                optionsPanel.add(p7, cc.xy(1, fl.getRowCount()));
            }

            fl.appendRow(new RowSpec("pref"));
            JPanel btnPanel = new JPanel();
            // JButton cancelButton = new JButton("Cancel");
            // final JButton saveButton = new JButton("Save");

            ChangeListener cl = new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    JSlider source = (JSlider) e.getSource();
                    int value = source.getValue();
                    if (value != heatMapPanel.getRowSize()) {
                        heatMapComponent.setRowSize(value);
                    }
                    if (value != heatMapPanel.getColumnSize()) {
                        heatMapComponent.setColumnSize(value);
                    }
                    gridSizeTextField.setText(String.valueOf(value));
                    heatMapComponent.featureTable.invalidate();
                    heatMapComponent.featureTable.validate();
                    heatMapPanel.repaint();
                }
            };
            gridSizeSlider.setMinorTickSpacing(1);
            gridSizeSlider.setPaintTicks(true);
            gridSizeSlider.setPaintLabels(true);
            Hashtable<Integer, JLabel> labels = new Hashtable<Integer, JLabel>();

            labels.put(new Integer(2), new JLabel("" + 2, JLabel.CENTER));
            labels.put(new Integer(10), new JLabel("" + 10, JLabel.CENTER));
            labels.put(new Integer(20), new JLabel("" + 20, JLabel.CENTER));
            labels.put(new Integer(30), new JLabel("" + 30, JLabel.CENTER));

            gridSizeSlider.setLabelTable(labels);
            gridSizeSlider.addChangeListener(cl);

            ActionListener l = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (e.getSource() == gridSizeTextField) {

                        try {
                            int value = Integer.parseInt(gridSizeTextField
                                    .getText());
                            if (value != heatMapPanel.getRowSize()) {
                                heatMapComponent.setRowSize(value);
                            }
                            if (value != heatMapPanel.getColumnSize()) {
                                heatMapComponent.setColumnSize(value);
                            }
                            gridSizeSlider.setValue(value);
                        } catch (NumberFormatException e1) {
                            UIUtil.showErrorDialog(parent,
                                    "Grid size is not a number.");
                            return;
                        }

                    } else if (e.getSource() == rowSizeTextField) {
                        int rowSize;
                        try {
                            rowSize = Integer.parseInt(rowSizeTextField
                                    .getText());
                            if (rowSize != heatMapPanel.getRowSize()) {
                                heatMapComponent.setRowSize(rowSize);
                            }
                        } catch (NumberFormatException e1) {
                            UIUtil.showErrorDialog(parent,
                                    "Row size is not a number.");
                            return;
                        }
                    } else if (e.getSource() == columnSizeTextField) {
                        int columnSize;
                        try {
                            columnSize = Integer.parseInt(columnSizeTextField
                                    .getText());
                            if (columnSize != heatMapPanel.getColumnSize()) {
                                heatMapComponent.setColumnSize(columnSize);
                            }
                        } catch (NumberFormatException e1) {
                            UIUtil.showErrorDialog(parent,
                                    "Column size is not a number.");
                            return;
                        }
                    } else if (e.getSource() == showColumnNamesCheckBox) {
                        boolean showColumnNames = showColumnNamesCheckBox
                                .isSelected();
                        heatMapComponent.setShowColumnNames(showColumnNames);
                    } else if (e.getSource() == showGridCheckBox) {
                        boolean showGrid = showGridCheckBox.isSelected();
                        heatMapPanel.setDrawGrid(showGrid);
                    } else if (e.getSource() == showRowNamesCheckBox) {
                        boolean showRowNames = showRowNamesCheckBox
                                .isSelected();
                        heatMapComponent.setShowRowNames(showRowNames);
                    } else if (e.getSource() == showFeatureDescriptionsCheckBox) {
                        boolean showRowDescriptions = showFeatureDescriptionsCheckBox
                                .isSelected();
                        heatMapComponent
                                .setShowRowDescriptions(showRowDescriptions);
                    } else if (e.getSource() == rowButton
                            || e.getSource() == globalButton) {
                        int colorScheme = rowButton.isSelected() ? HeatMapPanel.NORMALIZATION_ROW
                                : HeatMapPanel.NORMALIZATION_GLOBAL;
                        heatMapPanel.setNormalization(colorScheme);
                    }

                    heatMapComponent.featureTable.invalidate();
                    heatMapComponent.featureTable.validate();
                    heatMapPanel.repaint();

                }

            };

            rowSizeTextField.addActionListener(l);
            columnSizeTextField.addActionListener(l);
            showColumnNamesCheckBox.addActionListener(l);
            showGridCheckBox.addActionListener(l);
            showRowNamesCheckBox.addActionListener(l);
            showFeatureDescriptionsCheckBox.addActionListener(l);
            rowButton.addActionListener(l);
            globalButton.addActionListener(l);

            getContentPane().add(optionsPanel, BorderLayout.CENTER);
            getContentPane().add(btnPanel, BorderLayout.SOUTH);
            pack();
            setResizable(false);
            show();
        }
    }

    private class SaveDataset {
        final JDialog d;

        SaveDataset() {
            d = new CenteredDialog(parent);
            d.setTitle("Save Dataset");
            JLabel label = new JLabel("Output File:");
            JPanel filePanel = new JPanel();
            FormLayout f = new FormLayout(
                    "left:pref:none, 3dlu, left:pref:none, left:pref:none",
                    "pref, 5dlu, pref");
            filePanel.setLayout(f);
            final JTextField input = new JTextField(30);
            JButton btn = new JButton("Browse...");
            btn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    File f = showSaveDatasetDialog();
                    if (f != null) {
                        try {
                            input.setText(f.getCanonicalPath());
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    }
                }
            });
            CellConstraints cc = new CellConstraints();
            JLabel instructionsLabel = new JLabel(
                    "The selected columns and rows will be included in the dataset.");
            filePanel.add(label, cc.xy(1, 1));
            filePanel.add(input, cc.xy(3, 1));
            filePanel.add(btn, cc.xy(4, 1));

            final JButton cancelBtn = new JButton("Cancel");
            final JButton importBtn = new JButton("Save");
            JPanel buttonPanel = new JPanel();
            buttonPanel.add(cancelBtn);
            buttonPanel.add(importBtn);
            d.getContentPane().add(instructionsLabel, BorderLayout.NORTH);
            d.getContentPane().add(filePanel, BorderLayout.CENTER);
            d.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
            d.pack();
            ActionListener l = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (e.getSource() == importBtn) {
                        String pathname = input.getText().trim();
                        if (pathname.equals("")) {
                            UIUtil.showErrorDialog(parent,
                                    "Please enter an output file.");
                            return;
                        }
                        String outputFileFormat = Util.containsData(data,
                                ExpressionConstants.CALLS) ? "res" : "gct";

                        if (outputFileFormat.equals("gct")
                                && !pathname.toLowerCase().endsWith(".gct")) {
                            pathname += ".gct";
                        } else if (outputFileFormat.equals("res")
                                && !pathname.toLowerCase().endsWith(".res")) {
                            pathname += ".res";
                        }

                        int[] columns = sampleTable.getSelectedColumns();
                        if (columns.length == 0) {
                            columns = null;
                        }
                        int[] rows = featureTable.getSelectedRows();
                        if (rows.length == 0) {
                            rows = null;
                        }
                        IExpressionData slicedData;
                        if (data instanceof ExpressionData) {
                            slicedData = ((ExpressionData) data).slice(rows,
                                    columns);
                        } else {
                            slicedData = IExpressionDataUtil.sliceView(data,
                                    rows, columns);
                        }

                        VisualizerUtil.write(parent, slicedData,
                                outputFileFormat, pathname, false);
                        d.dispose();
                    } else {
                        d.dispose();
                    }
                }

            };
            importBtn.addActionListener(l);
            cancelBtn.addActionListener(l);
        }

        void show() {
            d.setVisible(true);
        }
    }

    private File showSaveDatasetDialog() {
        FileDialog fileDialog = Application.getFileManager().getHeatMapSaveDatasetFileDialog();
        if (SystemUtils.IS_OS_WINDOWS) {
        	fileDialog.setFile("*.gct;*.res;*.txt");
        } else {
        	fileDialog.setFile("dataset.gct");
        }
        fileDialog.setVisible(true);
        File[] files = fileDialog.getFiles();
        if (files != null && files.length > 0) {
            return files[0];
        }
        return null;
    }

    public static Map<String, List<Color>> getFeatureName2ColorsMap(SparseClassVector featureCV,
                                               IExpressionData data, boolean byRow) {

        Map<String, List<Color>> featureName2Colors = new HashMap<String, List<Color>>();

        for (int i = 0, end = byRow ? data.getRowCount() : data
                .getColumnCount(); i < end; i++) {
            // TODO: fix type safety.  Requires modification to SparseClassVector class.
            // Skipping that now to avoid going to deep with the current work
            List numbers = featureCV.getClassNumbers(i);
            if (numbers != null && numbers.size() > 0) {
                String name = byRow ? data.getRowName(i) : data
                        .getColumnName(i);
                List<Color> colors = new ArrayList<Color>();
                featureName2Colors.put(name, colors);
                for (int k = 0; k < numbers.size(); k++) {
                    Integer n = (Integer) numbers.get(k);
                    colors.add(featureCV.getColor(n));
                }
            }
        }
        return featureName2Colors;

    }

    private Component accessoryComponent;

    private boolean allowChangeColumnNameVisibility = true;

    private boolean allowChangeRowDescriptionsVisibility = true;

    private boolean allowChangeRowNameVisibility = true;

    private JPanel bottomPanel;

    private IExpressionData data;

    private FeatureAnnotatorPanel featureAnnotatorPanel;

    private JTable featureTable;

    private AbstractTableModel featureTableModel;

    private String featureUIString = "Feature";

    private JPanel heatMapAndAnnotatorPanel;

    private HeatMapPanel heatMapPanel;

    protected OptionsDialog optionsDialog;
    
    private Frame parent;

    private ProfilePlot plot;

    private JPopupMenu popupMenu;

    private boolean showPopupMenu = true;

    private SampleAnnotatorPanel sampleAnnotatorPanel;

    private JTable sampleTable;

    private AbstractTableModel sampleTableModel;

    private String sampleUIString = "Sample";

    private SaveDataset saveDataset;

    private JScrollPane scrollPane;

    private SetAnnotator setFeatureAnnotator;

    private SetAnnotator setSampleAnnotator;

    private boolean showColorSchemeOptions = true;

    private boolean showColumnNames = true;

    private boolean showRowDescriptions = true;

    private boolean showRowNames = true;

    private JLabel statusLabel = new JLabel("");

    private JPanel topPanel;

    private HeatMap heatMap = null;

    /**
     * @param _data
     * @param accessoryComponent panel to be shown above sample names or <code>null</code>
     */
    public HeatMapComponent(Frame parent, IExpressionData _data, Component accessoryComponent) {
        this.accessoryComponent = accessoryComponent;
        this.parent = parent;
        popupMenu = new JPopupMenu();
        JMenuItem profile2MenuItem = new JMenuItem("Profile");
        profile2MenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showProfilePlot();
            }
        });
        popupMenu.add(profile2MenuItem);
        this.data = _data;
        
        // TODO: Refactor to separate GUI code from calc & reporting/plotting
        
        this.saveDataset = (parent != null) ? new SaveDataset() : null;
        heatMapPanel = new HeatMapPanel(data);
        featureTableModel = createRowTableModel();
        String[] rowNames = new String[data.getRowCount()];
        for (int i = 0, rows = data.getRowCount(); i < rows; i++) {
            rowNames[i] = data.getRowName(i);
        }
        
        setFeatureAnnotator = new SetAnnotator(parent, new SetAnnotatorModel() {

            public int getFeatureCount() {
                return data.getRowCount();
            }

            public int getIndex(String name) {
                return data.getRowIndex(name);
            }

            public String getName(int index) {
                return data.getRowName(index);
            }

        }, true);


        featureTable = new GPTable(featureTableModel) {
            public void processMouseEvent(MouseEvent e) {
                if (e.isPopupTrigger() && showPopupMenu) {
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                } else {
                    super.processMouseEvent(e);
                }
            }
        };

        featureTable.setColumnSelectionAllowed(false);

        featureTable.setGridColor(new Color(239, 239, 255));

        featureTable.setRowHeight(heatMapPanel.getRowSize());
        featureTable.setFont(new Font("monospaced", Font.PLAIN, heatMapPanel
                .getRowSize()));

        sampleTableModel = new AbstractTableModel() {

            // TODO: fix type safety
            public Class getColumnClass(int c) {
                return String.class;
            }

            public int getColumnCount() {
                return data.getColumnCount();
            }

            public int getRowCount() {
                return showColumnNames ? 1 : 0;
            }

            public Object getValueAt(int rowIndex, int columnIndex) {
                return data.getColumnName(columnIndex);
            }

        };

        setSampleAnnotator = new SetAnnotator(parent, new SetAnnotatorModel() {

            public int getFeatureCount() {
                return data.getColumnCount();
            }

            public int getIndex(String name) {
                return data.getColumnIndex(name);
            }

            public String getName(int index) {
                return data.getColumnName(index);
            }

        }, false);

        sampleTable = new GPTable(null) {
            public void addColumn(TableColumn c) {
                super.addColumn(c);
                layoutSampleTableColumn(c, heatMapPanel.getColumnSize());
            }
        };
        VerticalLabelUI renderer = new VerticalLabelUI(false) {
            public void installUI(JComponent c) {
                super.installUI(c);
                c.setBackground(UIManager.getDefaults().getColor("Table.background"));
            } 
        };
        
        ((JLabel) sampleTable.getDefaultRenderer(String.class))
        .setUI(renderer);

        sampleTable.setRowSelectionAllowed(false);
        sampleTable.setModel(sampleTableModel);

        sampleTable.setGridColor(new Color(239, 239, 255));

        sampleTable.setColumnSelectionAllowed(true);

        System.setProperty(PlotAction.SHOW_ACCELERATORS_PROPERTY, "false");
        plot = new ProfilePlot(parent, data);
        System.setProperty(PlotAction.SHOW_ACCELERATORS_PROPERTY, "true");

        FormLayout fl = new FormLayout("pref, pref, pref:g(1)", "pref");
        CellConstraints cc = new CellConstraints();
        bottomPanel = new JPanel(fl);
        heatMapAndAnnotatorPanel = new JPanel(new FormLayout("pref, pref",
                "pref"));

        heatMapAndAnnotatorPanel.add(heatMapPanel, cc.xy(1, 1,
                CellConstraints.FILL, CellConstraints.TOP));
        featureAnnotatorPanel = new FeatureAnnotatorPanel(setFeatureAnnotator
                .getClassVector(), _data, heatMapPanel.getRowSize(),
                heatMapAndAnnotatorPanel);

        heatMapAndAnnotatorPanel.add(featureAnnotatorPanel, cc.xy(2, 1,
                CellConstraints.LEFT, CellConstraints.TOP));

        bottomPanel.add(heatMapAndAnnotatorPanel, cc.xy(2, 1,
                CellConstraints.LEFT, CellConstraints.TOP));

        JPanel temp = new JPanel(new BorderLayout());
        temp.setBorder(BorderFactory.createEmptyBorder(2, 1, 0, 0));
        temp.add(featureTable);
        bottomPanel.add(temp, cc.xy(3, 1, CellConstraints.FILL,
                CellConstraints.TOP));

        topPanel = new JPanel(new FormLayout("pref, pref:g", "pref, pref, pref"));
        // accessoryPanel
        // sample table, feature table header
        // sample annotations

        topPanel.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0));
        topPanel.add(sampleTable, cc.xy(1, 2));
        topPanel.add(featureTable.getTableHeader(), cc.xy(2, 3,
                CellConstraints.FILL, CellConstraints.BOTTOM));
        setAccessoryComponent(accessoryComponent, 1);

        sampleAnnotatorPanel = new SampleAnnotatorPanel(setSampleAnnotator
                .getClassVector(), _data, heatMapPanel.getColumnSize(),
                topPanel);

        topPanel.add(sampleAnnotatorPanel, cc.xywh(1, 3, 2, 1,
                CellConstraints.LEFT, CellConstraints.TOP));

        scrollPane = new JScrollPane(bottomPanel);
        scrollPane.setColumnHeaderView(topPanel);

        setColumnSize(heatMapPanel.getColumnSize());
        layoutSampleTable(heatMapPanel.getColumnSize());

        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
    }

    public HeatMap getHeatMap() {
        if (heatMap == null) {
            heatMap = buildHeatMap();
        }
        return heatMap;
    }
    
    public JMenuBar createMenuBar(boolean standalone, boolean showFind,
                                  boolean showAnnotations, boolean showViewMenu) {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);

        if (showAnnotations) {
            fileMenu.add(setFeatureAnnotator.getOpenFeaturesMenuItem());
            fileMenu.add(setSampleAnnotator.getOpenFeaturesMenuItem());
        }
        final JMenuItem saveDataMenu = new JMenuItem("Save Dataset...");
        fileMenu.add(saveDataMenu);
        JMenu saveImageMenu = new JMenu("Save Image");
        final JMenuItem saveJpeg = new JMenuItem("jpeg...");
        final JMenuItem savePng = new JMenuItem("png...");
        final JMenuItem saveSvg = new JMenuItem("svg...");
        saveImageMenu.add(saveJpeg);
        saveImageMenu.add(savePng);
        saveImageMenu.add(saveSvg);
        fileMenu.add(saveImageMenu);

        if (standalone) {
            fileMenu.add(FileMenu.createExitMenuItem());
        }

        JMenu editMenu = new JMenu("Edit");

        JMenu viewMenu = new JMenu("View");

        if (showAnnotations) {
            editMenu.add(setFeatureAnnotator.getViewFeatureListsMenuItem());
            editMenu.add(setSampleAnnotator.getViewFeatureListsMenuItem());
        }
        final JMenuItem legendMenuItem = new JMenuItem("Legend");
        viewMenu.add(legendMenuItem);

        final JMenuItem profileMenuItem = new JMenuItem("Profile");
        viewMenu.add(profileMenuItem);

        editMenu.add(new JSeparator());
        JMenuItem optionsMenu = new JMenuItem("Display Options...");
        editMenu.add(optionsMenu);

        ActionListener l = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == saveDataMenu) {
                    saveDataset.show();
                } else if (e.getSource() == profileMenuItem) {
                    showProfilePlot();
                } else if (e.getSource() == legendMenuItem) {
                    JDialog d = new CenteredDialog(parent);
                    d.setTitle("Legend");
                    Component p = heatMapPanel.getColorConverter().getLegend();
                    d.getContentPane().add(p);
                    d.pack();
                    d.setVisible(true);
                } else if (e.getSource() == saveJpeg) {
                    showSaveImageDialog(Application.getFileManager().getHeatMapSaveJpgFileDialog(), "jpeg");
                } else if (e.getSource() == savePng) {
                    showSaveImageDialog(Application.getFileManager().getHeatMapSavePngFileDialog(), "png");
                } else if (e.getSource() == saveSvg) {
                    showSaveImageDialog(Application.getFileManager().getHeatMapSaveSvgFileDialog(), "svg");
                } else {
                    showOptionsDialog();
                }
            }
        };
        legendMenuItem.addActionListener(l);
        profileMenuItem.addActionListener(l);
        optionsMenu.addActionListener(l);
        saveJpeg.addActionListener(l);
        savePng.addActionListener(l);
        saveSvg.addActionListener(l);
        saveDataMenu.addActionListener(l);

        if (showFind) {
            editMenu.add(new JSeparator());
            editMenu.add(new FindAction(parent, featureTable, data, 0));
        }
        menuBar.add(editMenu);
        if (showViewMenu) {
            menuBar.add(viewMenu);
        }

        return menuBar;
    }

    private AbstractTableModel createRowTableModel() {
        AbstractTableModel rowTableModel = new AbstractTableModel() {

            // TODO: fix type safety
            public Class getColumnClass(int col) {
                return String.class;
            }

            public int getColumnCount() {
                int columns = 0;
                if (showRowNames) {
                    columns++;
                }
                if (showRowDescriptions) {
                    columns++;
                }

                return columns;
            }

            public String getColumnName(int col) {
                if (showRowNames && col == 0) {
                    return featureUIString;
                } else if (showRowNames && showRowDescriptions && col == 1) {
                    return "Description";
                } else if (!showRowNames && showRowDescriptions && col == 0) {
                    return "Description";
                }
                return ""; // FIXME
            }

            public int getRowCount() {
                return data.getRowCount();
            }

            public Object getValueAt(int row, int col) {
                if (showRowNames && col == 0) {
                    return data.getRowName(row);
                } else if (showRowNames && showRowDescriptions && col == 1) {
                    return data.getRowMetadata(row, ExpressionConstants.DESC);
                } else if (!showRowNames && showRowDescriptions && col == 0) {
                    return data.getRowMetadata(row, ExpressionConstants.DESC);
                }

                // TODO: confirm this can be removed.  Seems to be unused.
                int offset = 0;
                if (showRowNames) {
                    offset++;
                }
                if (showRowDescriptions) {
                    offset++;
                }
                return null;
            }
        };
        return rowTableModel;
    }

    private void fireFeatureTableChanged() {

        featureTableModel.fireTableChanged(new TableModelEvent(
                featureTableModel, TableModelEvent.HEADER_ROW));
    }

    public IExpressionData getExpressionData() {
        return this.data;
    }

    public JTable getFeatureTable() {
        return featureTable;
    }

    public HeatMapPanel getHeatMapPanel() {
        return heatMapPanel;
    }

    public JTable getSampleTable() {
        return sampleTable;
    }

    public boolean isShowColumnNames() {
        return showColumnNames;
    }

    public boolean isShowRowDescriptions() {
        return showRowDescriptions;
    }

    public boolean isShowRowNames() {
        return showRowNames;
    }

    private void layoutSampleTable(int columnSize) {
        for (int i = 0; i < sampleTable.getColumnCount(); i++) {
            TableColumn c = sampleTable.getColumnModel().getColumn(i);
            c.setMinWidth(columnSize);
            c.setMaxWidth(columnSize);
            c.setPreferredWidth(columnSize);
            c.setWidth(columnSize);
        }
    }

    private void layoutSampleTableColumn(TableColumn c, int columnSize) {
        c.setMinWidth(columnSize);
        c.setMaxWidth(columnSize);
        c.setPreferredWidth(columnSize);
        c.setWidth(columnSize);
    }

    /**
     * Adds a component either aligned with sample names (column==1) or with
     * feature names (column==2)
     *
     * @param c      the component
     * @param column 1 to align with sample names, 2 to align with feature names
     */
    public void setAccessoryComponent(Component c, int column) {
        if (accessoryComponent != null) {
            topPanel.remove(accessoryComponent);
        }
        this.accessoryComponent = c;
        if (accessoryComponent != null) {
            CellConstraints cc = new CellConstraints();
            int gridWidth = column == 1 ? 2 : 1;
            Alignment alignment = column == 1 ? CellConstraints.TOP
                    : CellConstraints.BOTTOM;
            topPanel.add(accessoryComponent, cc.xywh(column, 1, gridWidth, 1,
                    CellConstraints.LEFT, alignment));
            // FIXME can't have sampleTree and accessoryPanel
        }
    }

    public void setColorConverter(ColorScheme colorConverter) {
        heatMapPanel.setColorConverter(colorConverter);
    }

    public void setColumnSize(int columnSize) {
        heatMapPanel.setColumnSize(columnSize);
        layoutSampleTable(columnSize);
        sampleTable.setFont(new Font("monospaced", Font.PLAIN, columnSize));
        JLabel test = new JLabel();
        test.setFont(new Font("monospaced", Font.PLAIN, columnSize));
        int size = 0;
        for (int j = 0; j < data.getColumnCount(); j++) {
            test.setText(data.getColumnName(j));
            size = Math.max(size, test.getPreferredSize().width);
        }
        sampleAnnotatorPanel.setColumnSize(columnSize);
        sampleTable.setRowHeight(size + 10);
        heatMapAndAnnotatorPanel.invalidate();
        heatMapAndAnnotatorPanel.validate();
    }

    /**
     * Sets the data to display
     *
     * @param d the dataset
     */
    public void setExpressionData(IExpressionData d) {
        this.data = d;
        heatMapPanel.setExpressionData(d);

        featureTableModel.fireTableStructureChanged();
        sampleTableModel.fireTableStructureChanged();
        setColumnSize(heatMapPanel.getColumnSize());
        plot = new ProfilePlot(parent, data);
    }

    public void setFeatureUIString(String s) {
        this.featureUIString = s;
    }

    /**
     * Sets the options the user can change in the display options dialog
     *
     * @param allowChangeColumnNameVisibility
     *
     * @param allowChangeRowNameVisibility
     * @param allowChangeRowDescriptionsVisibility
     *
     */
    public void setOptionsDialogOptions(
            boolean allowChangeColumnNameVisibility,
            boolean allowChangeRowNameVisibility,
            boolean allowChangeRowDescriptionsVisibility) {
        this.allowChangeColumnNameVisibility = allowChangeColumnNameVisibility;
        this.allowChangeRowNameVisibility = allowChangeRowNameVisibility;
        this.allowChangeRowDescriptionsVisibility = allowChangeRowDescriptionsVisibility;
    }

    public void setRowSize(int rowSize) {
        featureAnnotatorPanel.setRowSize(rowSize);
        featureTable.setRowHeight(rowSize);
        heatMapPanel.setRowSize(rowSize);
        featureTable.setFont(new Font("monospaced", Font.PLAIN, rowSize));
    }

    public void setSampleUIString(String s) {
        this.sampleUIString = s;
    }

    public void setShowColorSchemeOptions(boolean b) {
        showColorSchemeOptions = b;
    }

    public void setShowColumnNames(boolean showColumnNames) {
        if (this.showColumnNames == showColumnNames) {
            return;
        }
        this.showColumnNames = showColumnNames;
        sampleTableModel.fireTableStructureChanged();
    }

    public void setShowFeatureAnnotator(boolean b) {
        setFeatureAnnotator.getOpenFeaturesMenuItem().setVisible(b);
        setFeatureAnnotator.getViewFeatureListsMenuItem().setVisible(b);
    }

    /**
     * Sets whether to display the header for features
     *
     * @param b
     */
    public void setShowFeatureTableHeader(boolean b) {
        featureTable.getTableHeader().setVisible(b);
    }

    public void setShowRowDescriptions(boolean showRowDescriptions) {
        if (this.showRowDescriptions == showRowDescriptions) {
            return;
        }
        this.showRowDescriptions = showRowDescriptions;
        fireFeatureTableChanged();
    }

    public void setShowRowNames(boolean showRowNames) {
        if (this.showRowNames == showRowNames) {
            return;
        }
        this.showRowNames = showRowNames;
        fireFeatureTableChanged();
    }

    public void setShowSampleAnnotator(boolean b) {
        setSampleAnnotator.getOpenFeaturesMenuItem().setVisible(b);
        setSampleAnnotator.getViewFeatureListsMenuItem().setVisible(b);
    }

    public void showOptionsDialog() {
        if (optionsDialog != null && optionsDialog.isShowing()) {
            optionsDialog.toFront();
        } else {
            optionsDialog = new OptionsDialog(parent, HeatMapComponent.this,
                    heatMapPanel, allowChangeColumnNameVisibility,
                    allowChangeRowNameVisibility,
                    allowChangeRowDescriptionsVisibility);
        }
    }

    public void showProfilePlot() {
        final int[] indices = featureTable.getSelectedRows();
        if (indices.length == 0) {
            UIUtil.showMessageDialog(parent, "Please select "
                    + Character.toLowerCase(featureUIString.charAt(0))
                    + featureUIString.substring(1, featureUIString.length())
                    + "s to view.");
            return;
        }
        SwingWorker<Object, Void> worker = new SwingWorker<Object, Void>() {
            protected Object doInBackground() throws Exception {
                plot.plot(indices);
                return null;
            }
        };
        worker.execute();
    }

    private void showSaveImageDialog(FileDialog fileDialog, String... formats) {
        if (SystemUtils.IS_OS_WINDOWS) {
            StringBuilder sb = new StringBuilder("*.").append(formats[0]);
            for (int i = 1; i < formats.length; i++) {
                sb.append(";*.").append(formats[1]);
            }
            fileDialog.setFile(sb.toString());
        } else {
            fileDialog.setFile("image." + formats[0]);
        }
        fileDialog.setVisible(true);
        File[] files = fileDialog.getFiles();
        if (files != null && files.length > 0) {
            File f = files[0];
            SwingWorker<Object, Void> worker = new SwingWorker<Object, Void>() {
                protected Object doInBackground() throws Exception {
                    saveImageToFile(f, formats[0]);
                    return null;
                }
            };
            worker.execute();
        }
    }

    private HeatMap buildHeatMap() {
        final Map<String, List<Color>> featureName2Colors = getFeatureName2ColorsMap(
                setFeatureAnnotator.getClassVector(), 
                data, true);

        final Map<String, List<Color>> sampleName2Colors = getFeatureName2ColorsMap(
                setSampleAnnotator.getClassVector(), 
                data, false);
        DisplaySettings ds = new DisplaySettings();
        ds.columnSize = heatMapPanel.getColumnSize();
        ds.rowSize = heatMapPanel.getRowSize();
        ds.drawGrid = heatMapPanel.isDrawGrid();
        ds.drawRowNames = showRowNames;
        ds.drawRowDescriptions = showRowDescriptions;
        ds.gridLinesColor = Color.black;
        ds.colorConverter = heatMapPanel.getColorConverter();
        ds.upperTriangular = heatMapPanel.isUpperTriangular();
        final int columns = 0;

        // TODO: confirm this can be removed.  Seems to never be populated anywhere.
        final List<String[]> columnDataList = new ArrayList<String[]>();

        FeatureAnnotator fa = new FeatureAnnotator() {

            public String getAnnotation(String feature, int j) {
                String[] columnData = columnDataList.get(j);
                return columnData[data.getRowIndex(feature)];
            }

            public List<Color> getColors(String featureName) {
                return featureName2Colors.get(featureName);
            }

            public int getColumnCount() {
                return columns;
            }

        };

        SampleAnnotator sa = new SampleAnnotator() {

            public List<Color> getColors(String sampleName) {
                return sampleName2Colors.get(sampleName);
            }

            public String getLabel(int i) {
                return null;
            }

            public Color getPhenotypeColor(String sampleName) {
                return null;
            }

            public boolean hasPhenotypeColors() {
                return false;
            }
        };
        
        return HeatMap.createHeatMap(data, ds, fa, sa);
    }
    
    public void saveImageToFile(final File f, final String format) {
        try {
            HeatMap heatMap = getHeatMap();
            ImageUtil.savePlotImage(heatMap, f, format);

        } catch (IOException e) {
            UIUtil.showErrorDialog(parent, "An error occurred while saving the image '" + f.getName() + "'");
        } catch (OutOfMemoryError ome) {
            UIUtil.showErrorDialog(parent, "Not enough memory available to save the image.");
        }
    }
}