/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xapps.api.frameworks.fiji;

import com.jidesoft.popup.JidePopup;
import com.jidesoft.status.*;
import com.jidesoft.swing.JideBoxLayout;
import edu.mit.broad.genome.Conf;
import edu.mit.broad.genome.JarResources;
import edu.mit.broad.genome.XLogger;
import edu.mit.broad.genome.viewers.SystemConsoleViewer;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * @author Aravind Subramanian
 */
public class StatusBarJideImpl extends AppenderSkeleton implements edu.mit.broad.xbench.core.StatusBar {

    private StatusBar fJideStatusBar;

    private LabelStatusBarItem fStatusBarLabelItem;

    // Because i init and set the system consle at startup, the logging is auto in it
    //private java.util.List fLogObjects;

    //private static int MAX_SIZE = 50;

    private SystemConsoleViewer fSystemConsoleComp;

    /**
     * Class constructor
     */
    public StatusBarJideImpl() {

        this.fJideStatusBar = new StatusBar();
        this.fSystemConsoleComp = new SystemConsoleViewer();
        //fSystemConsoleComp.setBorder(BorderFactory.createTitledBorder("Process messages (for # of permutations)"));
        fSystemConsoleComp.setBorder(BorderFactory.createTitledBorder("Application messages"));

        fJideStatusBar.add(new TimeStatusBarItem(), JideBoxLayout.FIX);

        //this.fLogObjects = new ArrayList(MAX_SIZE);
        this.fStatusBarLabelItem = new LabelStatusBarItem();
        fStatusBarLabelItem.setIcon(JarResources.getIcon("expandall.png"));
        fStatusBarLabelItem.setToolTip("Click for application messages (such as # of permutations complete)");

        fJideStatusBar.add(fStatusBarLabelItem, JideBoxLayout.FLEXIBLE);

        final MemoryStatusBarItem gc = new MemoryStatusBarItem();
        gc.setPreferredWidth(75);
        fJideStatusBar.add(gc, JideBoxLayout.FIX);

        final ResizeStatusBarItem resize = new ResizeStatusBarItem();
        resize.setPreferredWidth(20);
        resize.setBorder(BorderFactory.createEmptyBorder());
        fJideStatusBar.add(resize, JideBoxLayout.FIX);

        this.fStatusBarLabelItem.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                showPopup(fStatusBarLabelItem);
            }

            public void mouseEntered(MouseEvent e) {
                fStatusBarLabelItem.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }

            public void mouseExited(MouseEvent e) {
                fStatusBarLabelItem.setCursor(Cursor.getDefaultCursor());
            }

        });


    }

    public JComponent getAsComponent() {
        return fJideStatusBar;
    }


    private void showPopup(final JComponent owner) {

        final JidePopup popup = new JidePopup();
        popup.setMovable(true);
        popup.getContentPane().setLayout(new BorderLayout());

        /*
        JTextArea logOutput = new JTextArea();
        logOutput.setRows(25);
        logOutput.setColumns(80);
        logOutput.setBorder(BorderFactory.createTitledBorder("Application messages"));
         for (int i = 0; i < msgs.size(); i++) {
            logOutput.append(msgs.get(i).toString());
        }

        logOutput.setEditable(false);
        //popup.getContentPane().add(new JScrollPane(logOutput), BorderLayout.CENTER);
        */

        /*        
        JideSplitPane split = new JideSplitPane(JideSplitPane.HORIZONTAL_SPLIT);
        split.add(new JScrollPane(logOutput));
        split.add(new JScrollPane(fSystemConsoleComp));
        split.setInitiallyEven(true);
        split.setPreferredSize(new Dimension(700, 350));
        popup.getContentPane().add(split);
        popup.setDefaultFocusComponent(logOutput);
        */

        //JScrollPane sp = new JScrollPane(fSystemConsoleComp);
        //sp.setPreferredSize(new Dimension(700, 350));
        fSystemConsoleComp.setPreferredSize(new Dimension(700, 350));
        popup.getContentPane().add(fSystemConsoleComp);
        popup.setDefaultFocusComponent(fSystemConsoleComp);
        popup.setOwner(owner);
        popup.setResizable(true);
        popup.setMovable(true);
        if (popup.isPopupVisible()) {
            popup.hidePopup();
        } else {
            popup.showPopup();
        }

    }

    // -------------------------------------------------------------------------------------------- //
    // ------------------------------LOG4J APPENDER IMPL--------------------------------------- //
    // -------------------------------------------------------------------------------------------- //

    public void append(final LoggingEvent event) {

        String txt = XLogger.getSimpleLayout().format(event);

        Level level = event.getLevel();


        if (level == Level.INFO) {
            fStatusBarLabelItem.setForeground(Color.BLACK);
            fStatusBarLabelItem.setText(txt);
        } else if (level == Level.DEBUG && Conf.isDebugMode()) {
            fStatusBarLabelItem.setForeground(Color.DARK_GRAY);
            fStatusBarLabelItem.setText(txt);
        } else if (level == Level.WARN) {
            fStatusBarLabelItem.setForeground(Color.ORANGE);
            fStatusBarLabelItem.setText(txt);
        } else if (level == Level.FATAL || level == Level.ERROR) {
            // @note dont show it scares people
            // /fStatusBarLabelItem.setForeground(Color.RED);
            //fStatusBarLabelItem.setForeground(Color.RED);
            //fStatusBarLabelItem.setText(txt);
        }

        /*
        fLogObjects.add(txt);
        if (fLogObjects.size() >= MAX_SIZE) {
            fLogObjects.remove(0);
        }
        */

    }

    public void close() {
    }

    public boolean requiresLayout() {
        return false;
    }

} // End class StatusBarJideImpl
