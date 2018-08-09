/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.explorer.filemgr;

import edu.mit.broad.genome.parsers.DataFormat;
import edu.mit.broad.xbench.core.api.Application;
import xapps.gsea.GseaAppConf;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

/**
 * @author Aravind Subramanian
 *         <p/>
 *         1) The usual JFile Chooser but in addition a pane that lists the recent DIRECTORIES used
 *         Double clicking a dir lists the contents of that in the file view
 *         2) Custom icons based on data format
 *         <p/>
 *         Use this ONLY for file choosers (not dirs)
 */
public class XFileChooserImpl extends JFileChooser implements XFileChooser {

    /**
     * Class constructor
     */
    public XFileChooserImpl(final JList recentFiles) {
        super();
        initHere(null, recentFiles);
    }

    private void initHere(final JList stdDirsList_opt, final JList recentFilesList) {

        setMouseListener(recentFilesList);
        if (stdDirsList_opt != null) {
            setMouseListener(stdDirsList_opt);
        }

        super.setAcceptAllFileFilterUsed(true);
        super.setMultiSelectionEnabled(true);

        super.setFileView(DataFormat.getParsableFileView());

        super.addChoosableFileFilter(GseaAppConf.createGseaFileFilter());

        /*
        FileFilter[] filters = Conf.Gsea.createAllFileFilters();
        for (int i = 0; i < filters.length; i++) {
            super.addChoosableFileFilter(filters[i]);
        }
        */

        // IMP -- dont say files only, then user cannt navigate through dirs
        this.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        //super.setEditable(true);

        // @note change the default -- make it taller hmm wider now
        Dimension PREF_SIZE = new Dimension(650, 450);
        super.setPreferredSize(PREF_SIZE);

        //recentDirs = RuntimeResources.getRecentDirs();
        //log.debug("# of recent dirs = " + recentDirs.length);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(PREF_SIZE.width - 250, PREF_SIZE.height));

        if (stdDirsList_opt == null) {
            panel.setBorder(BorderFactory.createTitledBorder("Recent folders (double click to list content)"));
            panel.add(new JScrollPane(recentFilesList), BorderLayout.CENTER);
        } else {

            JScrollPane sp = new JScrollPane(recentFilesList);
            sp.setBorder(BorderFactory.createTitledBorder("Recent folders (double click to list content)"));
            panel.add(sp, BorderLayout.CENTER);

            sp = new JScrollPane(stdDirsList_opt);
            sp.setPreferredSize(new Dimension(PREF_SIZE.width - 250, 150));
            sp.setBorder(BorderFactory.createTitledBorder("Application folders"));
            panel.add(sp, BorderLayout.SOUTH);

        }


        super.setAccessory(panel);
    }

    private void setMouseListener(final JList recentFilesList) {
        recentFilesList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int index = recentFilesList.locationToIndex(e.getPoint());
                    ListModel dlm = recentFilesList.getModel();
                    Object item = dlm.getElementAt(index);
                    recentFilesList.ensureIndexIsVisible(index);
                    //System.out.println("Double clicked on " + item);
                    File file = new File(item.toString());
                    if (file.exists()) {
                        if (file.isDirectory() == false) {
                            file = file.getParentFile();
                        }
                        setCurrentLocation(file.getPath());
                    } else {
                        throw new RuntimeException("File not found: " + file);
                    }
                }
            }
        });
    }

    public void setCurrentLocation(final File file) {
        if (file.isDirectory()) {
            super.setCurrentDirectory(file);
        } else {
            super.setSelectedFile(file);
        }
    }

    public void setCurrentLocation(final String path) {
        this.setCurrentLocation(new File(path));
    }

    public void setApproveButtonText(final String txt) {
        super.setApproveButtonText(txt);
    }

    public boolean showOpenDialog() {
        int retVal = super.showOpenDialog(Application.getWindowManager().getRootFrame());
        return retVal == JFileChooser.APPROVE_OPTION;
    }

} // End XFileChooserImpl
