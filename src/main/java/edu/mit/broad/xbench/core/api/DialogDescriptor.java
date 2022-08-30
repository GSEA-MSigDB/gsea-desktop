/*
 * Copyright (c) 2003-2022 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.xbench.core.api;

import javax.swing.*;

import xtools.api.param.Validator;

import java.awt.*;

/**
 * has some magic to make jlists double clickable
 *
 * @author Aravind Subramanian, David Eby
 */
// TODO: collapse type hierarchy here.  There is and will only ever be one implementation 
public interface DialogDescriptor {

    public static Dimension DD_SIZE = new Dimension(550, 400);

    public static Dimension DD_SIZE_SMALLER = new Dimension(250, 200);

    public static Dimension DD_SIZE_WIDER = new Dimension(800, 400);

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

    public void setOnlyShowCloseOption();
    
    public void setDisplayWider();

    public void setWarningValidator(Validator warningValidator);
    
    public void setErrorValidator(Validator errorValidator);

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
}
