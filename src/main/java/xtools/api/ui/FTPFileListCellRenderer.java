/*
 * Copyright (c) 2003-2022 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package xtools.api.ui;

import java.awt.Component;
import java.awt.Font;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

import org.apache.commons.lang3.StringUtils;
import org.genepattern.uiutil.FTPFile;

import edu.mit.broad.xbench.RendererFactory2;

public class FTPFileListCellRenderer extends DefaultListCellRenderer {
    private final String highestVersionId;
    
    public FTPFileListCellRenderer(String highestVersionId) {
        this.highestVersionId = StringUtils.lowerCase(highestVersionId);
    }

    public Component getListCellRendererComponent(@SuppressWarnings("rawtypes") JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        if (value != null && value instanceof FTPFile) {
            String s = ((FTPFile) value).getName();
            final String slc = s.toLowerCase();
            if (slc.contains(highestVersionId)) {
                Font font = this.getFont();
                String fontName = font.getFontName();
                int fontSize = font.getSize();
                this.setFont(new Font(fontName, Font.BOLD, fontSize));
            }
            this.setText(s);
            this.setIcon(RendererFactory2.FTP_FILE_ICON);
        }

        return this;
    }
}
