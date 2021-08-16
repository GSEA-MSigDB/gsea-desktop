/*
 * Copyright (c) 2003-2021 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.swing;

import edu.mit.broad.genome.Constants;
import edu.mit.broad.genome.JarResources;
import edu.mit.broad.xbench.core.api.Application;
import gnu.trove.TIntArrayList;

import org.apache.log4j.Logger;

import java.util.LinkedList;
import java.util.Queue;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.table.*;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import java.awt.*;
import java.awt.event.*;

/**
 * A collection of GUI related utilities and methods to aid UI related classes.
 * <p/>
 * <p/>
 * - central repository of UI related default constants and empty arrays etc.
 *
 * @author Aravind Subramanian
 * @see Ideoms#Helper
 */
public class GuiHelper implements Constants {

    // STANDARD SET OF ICONS USED
    public static final Icon ICON_ERROR16 = JarResources.getIcon("Error.gif");
    public static final Icon ICON_START16 = JarResources.getIcon("Run16.png");
    public static final Icon ICON_PAUSE16 = JarResources.getIcon("Pause16.gif");
    public static final Icon ICON_COPY16 = JarResources.getIcon("Copy16.gif");
    public static final Icon ICON_HELP16 = JarResources.getIcon("Help16_v2.gif");

    public static final Icon ICON_ELLIPSIS = JarResources.getIcon("Ellipsis.png");

    public static final Icon ICON_OPTIONPANE_INFO16 = JarResources.getIcon("Inform16.gif");

    // commonly used wondow size
    public static final Dimension DIMENSION_STANDARD_WINDOW = new Dimension(500, 500);
    public static final Font FONT_DEFAULT_BOLD = new Font("Helvetica", Font.BOLD, 12);
    public static final Font FONT_DEFAULT = new Font("Helvetica", Font.PLAIN, 12);

    public static final Color COLOR_LIGHT_ORANGE = new Color(255, 172, 89);
    public static final Color COLOR_LIGHT_YELLOW = Color.decode("#FFFF99");
    public static final Color COLOR_DARK_GREEN = new Color(0, 81, 0);
    public static final Color COLOR_LIGHT_RED = new Color(255, 108, 108);
    public static final Color COLOR_LIGHT_BLUE = new Color(150, 150, 255);
    public static final Color COLOR_DARK_BROWN = new Color(128, 64, 64);
    public static final Color COLOR_VERY_LIGHT_GRAY = new Color(239, 239, 239);
    public static final Color COLOR_DARK_BLUE = new Color(63, 64, 124);

    private static final Logger klog = Logger.getLogger(GuiHelper.class);
    private static final Dimension kPlaceholderSize = new Dimension(200, 50);

    private GuiHelper() { }

    public static TitledBorder createTitledBorderForComponent(String title) {
        Border b = BorderFactory.createLineBorder(Color.BLACK);
        return BorderFactory.createTitledBorder(b, title, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.CENTER,
                FONT_DEFAULT, Color.GRAY);
    }

    /**
     * call after setting frames size
     */
    public static void centerComponent(Component comp) {
        Dimension rootSize = Application.getWindowManager().getRootFrame().getSize();
        Point rootLocation = Application.getWindowManager().getRootFrame().getLocation();
        Dimension size = comp.getSize();

        comp.setLocation(rootLocation.x + (rootSize.width - size.width) / 2,
                rootLocation.y + (rootSize.height - size.height) / 2);
    }

    public static void fill(final JComponent filledcomp, final JComponent filler) {
        filledcomp.removeAll();

        if (filledcomp instanceof JScrollPane) {
            ((JScrollPane) filledcomp).getViewport().add(filler, null);
        } else {
            filledcomp.setLayout(new GridLayout(1, 1));
            filledcomp.add(filler);
        }

        filledcomp.revalidate();
    }

    public static JPanel createPlaceholderPanel(final Dimension prefsize, final TextIconPair pair) {
        return _createPanel(prefsize, pair.text, pair.icon);
    }

    public static JPanel createNaPlaceholder() {
        return createPlaceholderPanel(kPlaceholderSize, TextIconPair.NA_COMPONENT);
    }

    public static JPanel createWaitingPlaceholder() {
        return createPlaceholderPanel(kPlaceholderSize, TextIconPair.WAITING_FOR_TASK);
    }

    private static JPanel _createPanel(final Dimension prefsize, final String text, final Icon icon) {
        JPanel panel = new JPanel();
        panel.setPreferredSize(prefsize);

        JLabel label = new JLabel(text);
        label.setSize(prefsize);
        label.setIcon(icon);

        panel.setLayout(new FlowLayout(FlowLayout.CENTER));
        panel.add(label);
        panel.setBackground(Color.white);
        return panel;
    }

    public static void safeSelect(final JComboBox cb) {
        if (cb.getSelectedIndex() != -1) { return; }

        if (cb.getModel().getSize() > 0) {
            cb.setSelectedIndex(0);
        }
    }
    
    public static JPanel createGradientHeader(Color background, String title, Icon icon) {
        JPanel panel = new JPanel(new BorderLayout()) {
            private final Color control = UIManager.getColor("control");
            
            public void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                if (isOpaque()) {
                    final int h = getHeight();
                    final int w = getWidth();

                    Graphics2D graphics2d = (Graphics2D) graphics;
                    Paint storedPaint = graphics2d.getPaint();
                    graphics2d.setPaint(new GradientPaint(0, 0, getBackground(), w, 0, control));
                    graphics2d.fillRect(0, 0, w, h);
                    graphics2d.setPaint(storedPaint);
                }
            }
        };
        panel.setBackground(background);
        JLabel label = new JLabel(title, icon, SwingConstants.LEADING);
        label.setForeground(Color.white);
        label.setOpaque(false);
        panel.add(label, BorderLayout.WEST);
        panel.setBorder(BorderFactory.createEmptyBorder(3, 4, 3, 1));
        return panel;
    }

    /**
     * JButton related helper methods
     */
    public static class Button {
        /**
         * Button with standard "start" icon and specified text label
         */
        public static JButton createStartButton(final String text) {

            JButton but = new JButton(text, ICON_START16);

            but.setContentAreaFilled(false);
            but.setBorderPainted(true);

            return but;
        }

        // IMP seems buggy with some LnF's (the border doesnt get UNpainted)
        // works ok if the button is used in a JToolBar instead of in a JPanel
        public static void addMouseOverRollOverAction(final AbstractButton but) {
            addMouseOverRollOverAction(but, false);
        }

        public static void addMouseOverRollOverAction(final AbstractButton but, final boolean onlyBorders) {
            final Border bevel = new BevelBorder(BevelBorder.RAISED, GuiHelper.COLOR_DARK_BLUE, GuiHelper.COLOR_DARK_BLUE);

            but.setBorder(bevel);
            but.setBorderPainted(false);
            but.setRolloverEnabled(false); // needed for alloy lnf to work properly ??

            final Color bgColor = but.getBackground();

            but.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    but.setBorderPainted(true);

                    if (!onlyBorders) {
                        if (but.isSelected()) {
                            but.setBackground(GuiHelper.COLOR_DARK_BLUE);
                        } else {
                            but.setBackground(bgColor);
                        }
                    }
                }

                public void mouseExited(MouseEvent e) {
                    but.setBorderPainted(false);
                }
            });
        }

    }

    /**
     * Table related helper methods
     */
    public static class Table {
        public static void setColumnSize(int size, int col, JTable table, boolean alsoMax) {
            // the order of set calls is apparently important
            TableColumn column = table.getColumnModel().getColumn(col);

            column.setMinWidth(0);

            if (alsoMax) {
                column.setMaxWidth(size);
            }
            column.setPreferredWidth(size);
        }
    }

    public static class List2 {
        public static void setSelected(final Object[] selected_vals, final JList jlist, final DefaultListModel listModel) {
            if (selected_vals == null) {
                klog.error("Null arg for selected selected_vals");
                return;
            }

            TIntArrayList indices = new TIntArrayList();

            for (int i = 0; i < selected_vals.length; i++) {

                int index = listModel.indexOf(selected_vals[i]);
                if (index != -1) {
                    indices.add(index);
                }
            }

            jlist.setSelectedIndices(indices.toNativeArray());
        }
    }

    /**
     * JTree related helper methods
     */
    public static class Tree {
        
        /**
         * Expand a tree node and all its child nodes.
         *
         * @param tree The tree to expand.
         * @param path Path to the starting node.
         */
        public static void expandAll(JTree tree, TreePath path) {
            // Tail-recursive implementation of expandAll
            TreeModel model = tree.getModel();
            Queue<TreePath> pathsToDescend = new LinkedList<TreePath>();
            pathsToDescend.add(path);
            expandAllInternalNodes(tree, model, pathsToDescend);
        }
        
        private static void expandAllInternalNodes(JTree tree, TreeModel model, Queue<TreePath> pathsToDescend) {
            if (pathsToDescend.isEmpty()) return;
            
            TreePath path = pathsToDescend.remove();
            Object node = path.getLastPathComponent();
            if (model.isLeaf(node)) return;

            tree.expandPath(path);
            int num = model.getChildCount(node);

            for (int i = 0; i < num; i++) {
                TreePath subPath = path.pathByAddingChild(model.getChild(node, i));
                pathsToDescend.add(subPath);
            }
            
            expandAllInternalNodes(tree, model, pathsToDescend);
        }
    }
}
