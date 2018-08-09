/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.tui;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import edu.mit.broad.genome.XLogger;
import edu.mit.broad.genome.swing.GuiHelper;
import edu.mit.broad.genome.swing.fields.GDirFieldPlusChooser;
import edu.mit.broad.genome.swing.fields.GFieldPlusChooser;

import org.apache.log4j.Logger;

import xtools.api.param.Param;
import xtools.api.param.ParamSet;
import xtools.api.param.ReportDirParam;
import xtools.api.param.ReportLabelParam;

import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

/**
 * Displays the params in a jgoodies form
 */
public class ParamSetFormDisplay implements ParamSetDisplay {

    private Logger log = XLogger.getLogger(ParamSetFormDisplay.class);

    private ParamSet fParamSet;

    private String fTitle;
    private Icon fIcon;

    private JPanel fBasePanel;

    private boolean fShowBasic;
    private boolean fShowAdvanced;

    private JButton bBasic;
    private JButton bAdvanced;

    private static final Color LIGHT_GREEN = Color.decode("#EAFFEA");

    private static final int DEFAULT_INITIAL_DELAY = ToolTipManager.sharedInstance().getInitialDelay();

    private MouseMotionListener fMouseListener;

    /**
     * Class constructor
     *
     * @param title
     * @param icon
     * @param pset
     * @param ml
     */
    public ParamSetFormDisplay(final String title, final Icon icon, final ParamSet pset, final MouseMotionListener ml) {
        if (pset == null) {
            throw new IllegalArgumentException("Null pset param not allowed");
        }

        this.fTitle = title;
        this.fIcon = icon;
        this.fParamSet = pset;
        this.fMouseListener = ml;

        if (pset.getNumParams() > 10) { // @note heuristic
            this.fShowBasic = false;
        } else {
            this.fShowBasic = true;
        }
        
        this.fShowAdvanced = false;

        jbInit();
    }

    private void jbInit() {
        this.fBasePanel = new JPanel(new GridBagLayout());
        this.fBasePanel.addMouseMotionListener(fMouseListener);

        this.bBasic = new JButton();
        this.bBasic.setSize(10, 10);
        this.bAdvanced = new JButton();
        this.bAdvanced.setSize(10, 10);

        this.buildPanel();

        bBasic.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                fShowBasic = !fShowBasic;
                buildPanel();
            }
        });

        bAdvanced.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                fShowAdvanced = !fShowAdvanced;
                buildPanel();
            }
        });

    }

    private Param[] _getAllReqd() {

        Param[] reqd = fParamSet.getParams(Param.REQUIRED, true);
        Param[] ps_reqd = fParamSet.getParams(Param.PSEUDO_REQUIRED, true);
        Param[] all_reqd = new Param[reqd.length + ps_reqd.length];

        Param save_output = null; // just so that it is always last on the list of reqd params

        int cnt = 0;
        for (int i = 0; i < reqd.length; i++) {
            if (reqd[i] instanceof ReportDirParam) {
                save_output = reqd[i];
            } else {
                all_reqd[cnt++] = reqd[i];
            }
        }

        for (int i = 0; i < ps_reqd.length; i++) {
            all_reqd[cnt++] = ps_reqd[i];
        }

        if (save_output != null) {
            all_reqd[cnt] = save_output;
        }

        return all_reqd;
    }

    private Param[] _orderBasic(Param[] params) {
        java.util.List list = new ArrayList();
        for (int i = 0; i < params.length; i++) {
            if (params[i] instanceof ReportLabelParam) { // put this first
                list.add(params[i]);
            }
        }

        for (int i = 0; i < params.length; i++) {
            if (params[i] instanceof ReportLabelParam) {
            } else {
                list.add(params[i]);
            }
        }

        return (Param[]) list.toArray(new Param[list.size()]);
    }

    /**
     * Determines and answers the header's background color.
     * Tries to lookup a special color from the L&amp;F.
     * In case it is absent, it uses the standard internal frame background.
     *
     * @return the color of the header's background
     */
    protected Color getHeaderBackground() {
        return GuiHelper.COLOR_LIGHT_BLUE;
    }

    private void buildPanel() {

        this.fBasePanel.removeAll();
        JPanel gradientPanel = GuiHelper.createGradientHeader(getHeaderBackground(), fTitle, fIcon);

        GridBagConstraints gbc1 = new GridBagConstraints();
        gbc1.gridx = 0;
        gbc1.gridy = 0;
        gbc1.fill = GridBagConstraints.HORIZONTAL;
        gbc1.weightx = 0.5;
        this.fBasePanel.add(gradientPanel, gbc1);

        GridBagConstraints gbc2 = new GridBagConstraints();
        gbc2.gridx = 0;
        gbc2.gridy = 1;
        gbc2.fill = GridBagConstraints.HORIZONTAL;
        gbc2.weightx = 0.5;
        this.fBasePanel.add(createReqdParamsPanel(_getAllReqd()), gbc2);

        Param[] basics = fParamSet.getParams(Param.BASIC, true);
        basics = _orderBasic(basics);
        
        GridBagConstraints gbc3 = new GridBagConstraints();
        gbc3.gridx = 0;
        gbc3.gridy = 2;
        gbc3.fill = GridBagConstraints.HORIZONTAL;
        gbc3.weightx = 0.5;
        this.fBasePanel.add(createBasicParamsPanel(basics), gbc3);

        GridBagConstraints gbc4 = new GridBagConstraints();
        gbc4.gridx = 0;
        gbc4.gridy = 3;
        gbc4.fill = GridBagConstraints.BOTH;
        gbc4.weightx = 0.5;
        gbc4.weighty = 1.0;
        this.fBasePanel.add(createAdvancedParamsPanel(fParamSet.getParams(Param.ADVANCED, true)), gbc4);

        // needs both calls to work properly
        this.fBasePanel.revalidate();
        this.fBasePanel.repaint();
    }

    private JPanel createReqdParamsPanel(final Param[] params) {

        StringBuffer colStr = _createColStr();
        StringBuffer rowStr = _createRowStr(params);

        PanelBuilder builder = _createPanelBuilder(colStr, rowStr);
        CellConstraints cc = new CellConstraints();

        builder.addSeparator("Required fields", cc.xyw(1, 1, 4));

        int rowcnt = 3;
        for (int i = 0; i < params.length; i++) {
            GFieldPlusChooser chooser = params[i].getSelectionComponent();

            if (params[i].isFileBased()) {
                if (chooser instanceof GDirFieldPlusChooser) {
                    (((GDirFieldPlusChooser) chooser)).getTextField().setBackground(LIGHT_GREEN);
                }
            }

            JLabel label = new JLabel(params[i].getHtmlLabel_v2());
            enableToolTips(label, params[i]);
            builder.add(label, cc.xy(1, rowcnt));
            builder.add(chooser.getComponent(), cc.xy(3, rowcnt));
            // builder.add(new ParamDescButton(i, this, fParamSet).getButton(), cc.xy(5, rowcnt));
            //builder.add(new JButton("?"), cc.xy(5, rowcnt));
            rowcnt += 2; // because the spaces also count as a row
        }

        return builder.getPanel();
    }

    private JPanel createBasicParamsPanel(final Param[] params) {

        /*
        if (kBoldFont == null) {
            Font defaultFont = fBasePanel.getFont();
            kBoldFont = new Font(defaultFont.getName(), Font.BOLD, defaultFont.getSize());
        }
        */

        StringBuffer colStr = _createColStr();
        StringBuffer rowStr = _createRowStr(params);

        PanelBuilder builder = _createPanelBuilder(colStr, rowStr);
        CellConstraints cc = new CellConstraints();

        int sepwidth;
        if (fShowBasic) {
            bBasic.setText("Hide");
            sepwidth = 3;
        } else {
            bBasic.setText("Show");
            sepwidth = 3;
        }

        if (params.length == 0) {
            builder.addSeparator("Basic fields - none available", cc.xyw(1, 1, sepwidth));
            return builder.getPanel();
        }

        builder.add(bBasic, cc.xy(5, 1));
        builder.addSeparator("Basic fields", cc.xyw(1, 1, sepwidth));

        if (fShowBasic) {
            int rowcnt = 3;
            for (int i = 0; i < params.length; i++) {
                GFieldPlusChooser chooser = params[i].getSelectionComponent();

                if (params[i] instanceof ReportLabelParam) {
                    //System.out.println(">>> " + params[i].getName());
                    chooser.getComponent().setBackground(GuiHelper.COLOR_LIGHT_YELLOW);
                }

                final JLabel label = new JLabel(params[i].getHtmlLabel_v2());
                enableToolTips(label, params[i]);
                builder.add(label, cc.xy(1, rowcnt));
                //builder.add(label_cmd, cc.xy(3, rowcnt));
                builder.add(chooser.getComponent(), cc.xy(3, rowcnt));
                rowcnt += 2; // because the spaces also count as a row
            }
        }

        return builder.getPanel();
    }

    private static void enableToolTips(final JLabel label, final Param param) {

        label.setToolTipText(param.getDesc());
        label.addMouseListener(new MouseAdapter() {

            public void mouseEntered(MouseEvent e) {
                //System.out.println("entered");
                //Application.setCursor(kAppHandCursor);
                // Show tool tips immediately
                ToolTipManager.sharedInstance().setInitialDelay(0);
            }

            public void mouseExited(MouseEvent e) {
                //System.out.println("exited");
                //Application.setCursor(kAppReadyCursor);
                ToolTipManager.sharedInstance().setInitialDelay(DEFAULT_INITIAL_DELAY);
            }

            //table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                // Show tool tips immediately
                ToolTipManager.sharedInstance().setInitialDelay(0);
            }

        });


    }

    private JPanel createAdvancedParamsPanel(final Param[] params) {

        StringBuffer colStr = _createColStr();
        StringBuffer rowStr = _createRowStr(params);

        PanelBuilder builder = _createPanelBuilder(colStr, rowStr);
        CellConstraints cc = new CellConstraints();

        int sepwidth;
        if (fShowAdvanced) {
            bAdvanced.setText("Hide");
            sepwidth = 3;
        } else {
            bAdvanced.setText("Show");
            sepwidth = 3;
        }

        if (params.length == 0) {
            builder.addSeparator("Advanced fields - none available", cc.xyw(1, 1, sepwidth));
            return builder.getPanel();
        }

        builder.addSeparator("Advanced fields", cc.xyw(1, 1, sepwidth));
        builder.add(bAdvanced, cc.xy(5, 1));

        if (fShowAdvanced) {
            int rowcnt = 3;
            for (int i = 0; i < params.length; i++) {
                GFieldPlusChooser chooser = params[i].getSelectionComponent();
                JLabel label = new JLabel(params[i].getHtmlLabel_v2());
                enableToolTips(label, params[i]);
                builder.add(label, cc.xy(1, rowcnt));
                builder.add(chooser.getComponent(), cc.xy(3, rowcnt));
                rowcnt += 2; // because the spaces also count as a row
            }
        }

        return builder.getPanel();
    }

    public void addMouseMotionListener(MouseMotionListener l) {
        //builder.getPanel().addMouseMotionListener(l);
    }

    public Component getAsComponent() {
        return fBasePanel;
    }

    /**
     * restores defaults as got from the ParamSet
     */
    public void reset() {

        log.debug("Resetting params to defaults");

        for (int i = 0; i < fParamSet.getNumParams(); i++) {
            Param param = fParamSet.getParam(i);

            // dont set directly -- the comp doesnt get updated in that case
            final Object def = param.getDefault();
            if (def instanceof Object[]) {
                Object[] obj = (Object[]) def;
                StringBuffer buf = new StringBuffer();
                for (int j = 0; j < obj.length; j++) {
                    buf.append(obj[j].toString());
                    if (j != obj.length - 1) {
                        buf.append(',');
                    }
                }
                param.getSelectionComponent().setValue(buf.toString());
            } else {
                param.getSelectionComponent().setValue(def);
            }
        }

        this.buildPanel();

    }

    private static StringBuffer _createColStr() {
        return new StringBuffer("130dlu,      4dlu,        225dlu,   4dlu,  40dlu"); // columns
    }

    private static StringBuffer _createRowStr(final Param[] params) {
        StringBuffer rowStr = new StringBuffer();
        rowStr.append("pref, 5dlu,"); // for the spacer
        for (int i = 0; i < params.length; i++) {
            rowStr.append("pref, 3dlu");
            if (params.length != i - 1) {
                rowStr.append(",");
            }
        }
        return rowStr;
    }

    private static PanelBuilder _createPanelBuilder(StringBuffer colStr, StringBuffer rowStr) {
        FormLayout layout = new FormLayout(colStr.toString(), rowStr.toString());
        PanelBuilder builder = new PanelBuilder(layout);
        builder.setDefaultDialogBorder();
        return builder;
    }

} // End class ParamSetForm
