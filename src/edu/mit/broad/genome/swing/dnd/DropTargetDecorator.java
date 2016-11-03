/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.swing.dnd;

import edu.mit.broad.xbench.core.api.Application;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;

/**
 * Decorates a class with drop target functionality
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class DropTargetDecorator implements DropTargetListener {

    private DropTarget fDropTarget;
    private DndTarget fTarget;

    /**
     * the actions supported by this drop target
     */

    //private int fAcceptActions = DnDConstants.ACTION_COPY;
    //  private int fAcceptActions = DnDConstants.ACTION_MOVE;
    private final int fAcceptActions = DnDConstants.ACTION_COPY_OR_MOVE;

    /**
     * Class Constructor.
     */
    public DropTargetDecorator(DndTarget target) {

        if (target == null) {
            throw new IllegalArgumentException("DndTarget cannot be null");
        }

        this.fTarget = target;

        // component, ops, listener, accepting
        this.fDropTarget = new DropTarget(fTarget.getDroppableIntoComponent(), this.fAcceptActions,
                this, true);
    }

    /**
     * DropTargetListener impl
     * Called by isDragOk
     * Checks to see if the flavor drag flavor is acceptable
     *
     * @param e the DropTargetDragEvent object
     * @return whether the flavor is acceptable
     */
    private boolean isDragFlavorSupported(DropTargetDragEvent e) {

        DataFlavor[] flavors = fTarget.getDroppableFlavors();

        for (int i = 0; i < flavors.length; i++) {
            if (e.isDataFlavorSupported(flavors[i])) {
                return true;
            }
        }

        return false;
    }

    /**
     * DropTargetListener impl
     * Called by drop
     * Checks the flavors and operations
     *
     * @param e the DropTargetDropEvent object
     * @return the chosen DataFlavor or null if none match
     */
    private DataFlavor chooseDropFlavor(DropTargetDropEvent e) {

        DataFlavor[] flavors = fTarget.getDroppableFlavors();

        for (int i = 0; i < flavors.length; i++) {
            if (e.isDataFlavorSupported(flavors[i])) {
                return flavors[i];
            }
        }

        return null;
    }

    /**
     * DropTargetListener impl
     * Called by dragEnter and dragOver
     * Checks the flavors and operations
     *
     * @param e the event object
     * @return whether the flavor and operation is ok
     */
    private boolean isDragOk(DropTargetDragEvent e) {

        if (isDragFlavorSupported(e) == false) {

            //log.info("isDragOk:no flavors chosen");
            return false;
        }

        // the actions specified when the source
        // created the DragGestureRecognizer
        //      int sa = e.getSourceActions();
        // the docs on DropTargetDragEvent rejectDrag says that
        // the dropAction should be examined
        int da = e.getDropAction();

        //log.info("dt drop action " + da + " my acceptable actions " + fAcceptActions);
        // we're saying that these actions are necessary
        if ((da & fAcceptActions) == 0) {
            return false;
        }

        return true;
    }

    /**
     * DropTargetListener impl
     * start "drag under" feedback on component
     * invoke acceptDrag or rejectDrag based on isDragOk
     */
    public void dragEnter(DropTargetDragEvent e) {

        //log.info("dtlistener dragEnter");
        if (isDragOk(e) == false) {

            //log.info("enter not ok");
            e.rejectDrag();

            return;
        } else {

            //log.info("dt enter: accepting " + e.getDropAction());
            e.acceptDrag(e.getDropAction());
        }
    }

    /**
     * DropTargetListener impl
     * continue "drag under" feedback on component
     * invoke acceptDrag or rejectDrag based on isDragOk
     */
    public void dragOver(DropTargetDragEvent e) {

        if (isDragOk(e) == false) {

            //log.info("dtlistener dragOver not ok");
            DropTargetContext context = e.getDropTargetContext();

            context.getComponent().setCursor(DragSource.DefaultCopyNoDrop);
            context.getComponent().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            e.rejectDrag();

            return;
        } else {

            //log.info("dt over: accepting");
            e.acceptDrag(e.getDropAction());
        }
    }

    /**
     * DropTargetListener impl
     *
     * @param e
     */
    public void dropActionChanged(DropTargetDragEvent e) {

        if (isDragOk(e) == false) {

            //log.info("dtlistener changed not ok");
            e.rejectDrag();

            return;
        } else {

            //log.info("dt changed: accepting" + e.getDropAction());
            e.acceptDrag(e.getDropAction());
        }
    }

    /**
     * DropTargetListener impl.
     *
     * @param e
     */
    public void dragExit(DropTargetEvent e) {

        //log.info("dtlistener dragExit");
    }

    /**
     * DropTargetListener impl.
     * perform action from getSourceActions on
     * the transferrable
     * invoke acceptDrop or rejectDrop
     * invoke dropComplete
     * if its a local (same JVM) transfer, use StringTransferable.localStringFlavor
     * find a match for the flavor
     * check the operation
     * get the transferable according to the chosen flavor
     * do the transfer
     */
    public void drop(DropTargetDropEvent e) {

        //log.info("dtlistener drop");
        DataFlavor chosen = chooseDropFlavor(e);

        if (chosen == null) {

            //log.info("No flavor match found");
            e.rejectDrop();

            return;
        }

        // the actions that the source has specified with DragGestureRecognizer
        int sa = e.getSourceActions();

        //log.info("drop: sourceActions: " + sa + " drop: dropAction: " + da);
        if ((sa & fAcceptActions) == 0) {

            //log.info("No action match found");
            e.rejectDrop();

            return;
        }

        Object data = null;

        try {
            e.acceptDrop(this.fAcceptActions);

            // e.acceptDrop(DnDConstants.ACTION_MOVE);
            //e.acceptDrop(DnDConstants.ACTION_COPY);
            //e.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
            data = e.getTransferable().getTransferData(chosen);

            if (data == null) {
                throw new NullPointerException("Tarnsferable data was null");
            }

            //log.info("Got data: " + data.getClass().getName());
            fTarget.setDropData(data);
            e.dropComplete(true);
        } catch (Throwable t) {
            Application.getWindowManager().showError("Bad drop -- couldn't get transfer data", t);
            e.dropComplete(false);
        }
    }
}
