/*
 * Copyright (c) 2003-2022 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.xbench.tui;

import edu.mit.broad.genome.JarResources;
import edu.mit.broad.genome.models.ReportModel;
import edu.mit.broad.genome.reports.api.Report;
import edu.mit.broad.genome.swing.GuiHelper;
import edu.mit.broad.genome.utils.DateUtils;
import edu.mit.broad.xbench.ComparatorFactory2;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xtools.api.Tool;
import xtools.api.ToolCategory;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * <p>JTree of a collection of Tools. Organized by ToolCategory </p>
 * <p/>
 * Only builds the Tree (the lhs) and not the rhs panel (thats done ToolLauncher)
 * <p/>
 * We want to show Tools available and tools run in this session and past sessions
 * Actually reports not tools for the curr and past session.
 * So the tree represents Tools available and reports of Tools run in the (immeadiate and far) past
 *
 * @author Aravind Subramanian
 */
public class ToolSelectorTree extends JTree {
    private final Tool[] fTools = new Tool[]{};

    private final Logger log = LoggerFactory.getLogger(ToolSelectorTree.class);
    private final DefaultTreeModel fModel;

    /**
     * keys Tool values are their corres nodes
     * stored seperately for conv to expand to desired Tool
     */
    private final Map fToolNodeMap;

    public ToolSelectorTree() {
        this.setRootVisible(false);
        this.setShowsRootHandles(true);

        final DefaultTreeSelectionModel sm = new DefaultTreeSelectionModel();

        sm.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        this.setSelectionModel(sm);

        this.fToolNodeMap = new HashMap();
        this.fModel = new Model();

        this.setModel(fModel);
        this.setCellRenderer(new Renderer());
    }

    public Tool selectTool(final String toolName) {
        if (StringUtils.isBlank(toolName)) {
            return null;
        }

        for (int i = 0; i < fTools.length; i++) {
            if (fTools[i].getClass().getName().equalsIgnoreCase(toolName)) {
                TreePath tp = this.getPath(fTools[i]);
                this.setSelectionPath(tp);
                this.expandPath(tp);
                this.fireTreeExpanded(tp);
                this.revalidate();
                return fTools[i];
            }
        }

        log.debug("Unknown tool name, and could not select it: {}", toolName);

        return null;
    }

    public Component getComponent() {
        return this;
    }

    /**
     * will return the top level node if specified doesnt have a node
     *
     * @param tool
     * @return
     */
    public TreePath getPath(final Tool tool) {

        // @maint we knows its a dmtr
        TreeNode[] tns;
        Object obj = fToolNodeMap.get(tool);

        if (obj == null) {
            DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) fModel.getRoot();
            tns = fModel.getPathToRoot(rootNode);
        } else {
            DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode) obj;
            tns = fModel.getPathToRoot(dmtn);
        }

        return new TreePath(tns);
    }

    private class Model extends DefaultTreeModel {

        Model() {
            super(new DefaultMutableTreeNode("Tools", true));

            final DefaultMutableTreeNode toolNode = (DefaultMutableTreeNode) this.getRoot();
            final Map categoryMap = new TreeMap(new ComparatorFactory2.ToolCategoryComparator());

            for (int i = 0; i < fTools.length; i++) {
                final ToolCategory category = fTools[i].getCategory();
                if (categoryMap.containsKey(category)) {

                } else {
                    categoryMap.put(category, new ArrayList());
                }

                final List list = (List) categoryMap.get(category);

                list.add(fTools[i]);
            }

            Iterator it = categoryMap.keySet().iterator();

            while (it.hasNext()) {
                final ToolCategory category = (ToolCategory) it.next();
                final List tools = (List) categoryMap.get(category);
                final DefaultMutableTreeNode cnode = new DefaultMutableTreeNode(category, true);

                for (int i = 0; i < tools.size(); i++) {
                    DefaultMutableTreeNode dmtn = new DefaultMutableTreeNode(tools.get(i), false);
                    cnode.add(dmtn);
                    fToolNodeMap.put(tools.get(i), dmtn);
                }

                toolNode.add(cnode);
            }

            // add the reports as another node
            toolNode.add(new ReportModel().createReportNode(this));
        }
    }

    public static class Renderer extends DefaultTreeCellRenderer {

        private static final Icon ICON_RPT_SMALL = JarResources.getIcon("Rpt15.png"); // makes it less intrusive

        /**
         * TreeCellRenderer interface impl
         * See notes on implementation goals and logic above.
         */
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
                                                      boolean expanded, boolean leaf, int row, boolean hasFocus) {

            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

            //log.debug("value: " + value + " class: " + value.getClass());
            if (value instanceof DefaultMutableTreeNode) {
                Object node = ((DefaultMutableTreeNode) value).getUserObject();

                this.setBorder(BorderFactory.createLineBorder(Color.BLACK));
                this.setFont(GuiHelper.FONT_DEFAULT);

                //log.debug("user object: " + node);
                if (node instanceof ToolCategory) {
                    this.setFont(GuiHelper.FONT_DEFAULT_BOLD);
                    this.setBorder(null);
                    this.setText(((ToolCategory) node).getName());
                    this.setToolTipText(((ToolCategory) node).getDesc());
                    this.setIcon(((ToolCategory) node).getIcon());
                } else if (node instanceof Tool) {
                    StringTokenizer tok = new StringTokenizer(((Tool) node).getClass().getName(), ".");
                    String name = null;
                    while (tok.hasMoreTokens()) {
                        name = tok.nextToken();
                    }
                    this.setText(name);
                    this.setIcon(Tool.ICON);
                    this.setToolTipText(((Tool) node).getDesc());
                } else if (node instanceof Report) {
                    Report rpt = (Report) node;
                    prettyFormat(rpt, this);
                    this.setToolTipText(rpt.getQuickInfo());
                } else if (node instanceof ReportStub) {
                    ReportStub stub = (ReportStub) node;
                    prettyFormat(stub, this);
                } else {
                    this.setBorder(null);
                    this.setText(value.toString());
                }
            } else {
                this.setText("Unknown: " + value);
            }

            return this;
        }


        private void prettyFormat(ReportStub stub, JLabel label) {
            StringBuffer buf = new StringBuffer("<html><body>");
            buf.append(stub.getName_without_ts());
            buf.append("<font color=gray>");
            buf.append('[').append(DateUtils.formatAsHourMin(stub.getDate())).append(']');
            buf.append("</font>");
            buf.append("</body></html>");
            label.setIcon(ICON_RPT_SMALL);
            label.setText(buf.toString());
            label.setBorder(null);
        }

        private void prettyFormat(Report rpt, JLabel label) {
            StringBuffer buf = new StringBuffer("<html><body>");
            buf.append(rpt.getName());
            buf.append("<font color=gray>");
            buf.append('[').append(DateUtils.formatAsHourMin(rpt.getDate())).append("min").append(']');
            buf.append("</font>");
            buf.append("</body></html>");
            label.setIcon(ICON_RPT_SMALL);
            label.setText(buf.toString());
            label.setBorder(null);
        }
    }
}
