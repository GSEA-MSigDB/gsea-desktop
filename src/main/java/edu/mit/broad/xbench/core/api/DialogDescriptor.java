/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.xbench.core.api;

import javax.swing.*;
import java.awt.*;

/**
 * has some magic to make jlists double clickable
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public interface DialogDescriptor {

    public static Dimension DD_SIZE = new Dimension(550, 400);

    public static Dimension DD_SIZE_SMALLER = new Dimension(250, 200);

    /**
     * class constant for oks
     */
    public static final int OK_OPTION = 0;

    /**
     * class constant for cancel actions
     */
    public static final int CANCEL_OPTION = 2;

    public static final Integer OKI = new Integer(OK_OPTION);

    public static final Integer CANCELI = new Integer(CANCEL_OPTION);

    /**
     * Modal does NOT work properly with the show() option
     * If you need modal behaviour, use the show(dp) option
     *
     * @param modal No ok, cancel etc buttons will be displayed
     *              <p/>
     *              No ok, cancel etc buttons will be displayed
     *              <p/>
     *              No ok, cancel etc buttons will be displayed
     *              <p/>
     *              No ok, cancel etc buttons will be displayed
     *              <p/>
     *              No ok, cancel etc buttons will be displayed
     */
    //public void setModal(boolean modal);

    //public void setOptionType(int type);

    public void setOnlyShowCloseOption();

    //public void setPreferredSize();

    /**
     * If modal, a regular jdialog is always used
     * If not modal:
     * 1) if application (desktop) is available then the dialog is shown in a jif
     * (why? ->else the dial dissapears behind the desktop)
     * 2) If app is null, then the dialog iks shows in a jdialog
     * (same as modal, except jdialog is not modal)
     * <p/>
     * Known issue -> This does not work when launched from within JUNIT -> it always
     * makes a jdialog
     *
     * @return One of OK or CANCEL
     */
    public int show();


    /**
     * only makes sense if the specified jlist is a component that is
     * displayed in the dialog descriptor window
     * Double click / enter on the jlist == a OK button click
     * Simpley closes the dialog and returns void when double clicked (the OK is implied)
     */
    public void enableDoubleClickableJList(final JList jl);
}    // End DialogDescriptor

//  public void dispose();

// public void setPreferredSize(Dimension d);

//  public Dimension getSize();

//  public void setPreferredSize(int w, int h);
