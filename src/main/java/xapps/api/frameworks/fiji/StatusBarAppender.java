/*******************************************************************************
 * Copyright (c) 2003-2018 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package xapps.api.frameworks.fiji;

import com.jidesoft.popup.JidePopup;
import com.jidesoft.status.*;
import com.jidesoft.swing.JideBoxLayout;
import edu.mit.broad.genome.Conf;
import edu.mit.broad.genome.JarResources;
import edu.mit.broad.genome.viewers.SystemConsoleViewer;

import javax.swing.*;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serializable;

public class StatusBarAppender extends AbstractAppender {

    private StatusBar fJideStatusBar;

    private LabelStatusBarItem fStatusBarLabelItem;

    private SystemConsoleViewer fSystemConsoleComp;
    
    public StatusBarAppender(String name, Filter filter, Layout<? extends Serializable> layout) {
        super(name, filter, layout);
        
        this.fJideStatusBar = new StatusBar();
        this.fSystemConsoleComp = new SystemConsoleViewer();
        //fSystemConsoleComp.setBorder(BorderFactory.createTitledBorder("Process messages (for # of permutations)"));
        fSystemConsoleComp.setBorder(BorderFactory.createTitledBorder("Application messages"));

        fJideStatusBar.add(new TimeStatusBarItem(), JideBoxLayout.FIX);

        //this.fLogObjects = new ArrayList(MAX_SIZE);
        this.fStatusBarLabelItem = new LabelStatusBarItem();
        fStatusBarLabelItem.setIcon(JarResources.getIcon("expandall.png"));
        fStatusBarLabelItem.setToolTipText("Click for application messages (such as # of permutations complete)");

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

    @Override
    public void append(LogEvent event) {
        String txt = new String(getLayout().toByteArray(event));
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
        }
    }
}
