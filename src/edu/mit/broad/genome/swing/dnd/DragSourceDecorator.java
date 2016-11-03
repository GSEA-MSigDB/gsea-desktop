/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.swing.dnd;

import edu.mit.broad.genome.XLogger;
import org.apache.log4j.Logger;

import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;

/**
 * Decorates a class with drag source ability.
 * <p/>
 * DragGestureListener -> a listener that will start the drag.
 * <p/>
 * DragSourceListener -> a listener that will track the state of the DnD operation
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class DragSourceDecorator implements DragGestureListener, DragSourceListener {

    private final Logger log = XLogger.getLogger(DragSourceDecorator.class);
    private DragSource fDragSource;
    private final int fDragAction = DnDConstants.ACTION_COPY_OR_MOVE;
    private DndSource fSource;

    /**
     * Class Constructor.
     */
    public DragSourceDecorator(DndSource source) {

        if (source == null) {
            throw new IllegalArgumentException("Param DndSource cannot be null");
        }

        this.fSource = source;
        this.fDragSource = DragSource.getDefaultDragSource();

        // component, action, listener
        if (fSource.getDraggableComponent() == null) {
            log.warn("Null draggable component for: " + fSource);
        }

        //log.debug("Using drag source comp: " + fSource.getDraggableComponent() + " class: " + fSource.getDraggableComponent().getClass());
        this.fDragSource.createDefaultDragGestureRecognizer(fSource.getDraggableComponent(),
                this.fDragAction, this);
    }

    /**
     * DragGestureListener impl
     * Start the drag if the operation is ok.
     * uses java.awt.datatransfer.StringSelection to transfer
     * the label's data
     *
     * @param e the event object
     */
    public void dragGestureRecognized(DragGestureEvent e) {

        // if the action is ok we go ahead, otherwise we punt
        //log.info("dragGestureRecognized: " + e.getDragAction());
        if ((e.getDragAction() & fDragAction) == 0) {
            return;
        }

        try {

            //log.info("kicking off drag");
            // initial cursor, transferrable, dsource listener
            Transferable trf = fSource.getTransferable();
            if (trf != null) {
                e.startDrag(DragSource.DefaultCopyNoDrop, trf, this);
            }
        } catch (InvalidDnDOperationException idoe) {
            log.error(idoe);
        }
    }

    /**
     * DragSourceListener impl.
     * Does nothing
     *
     * @param e the event
     */
    public void dragDropEnd(DragSourceDropEvent e) {

        // we do nothing
        // if(e.getDropSuccess() == false) {
        //log.info("not successful");
        //return;
        // }

        /*
         * the dropAction should be what the drop target specified
         * in acceptDrop
         */

        //log.info("dragdropend action " + e.getDropAction());
        // this is the action selected by the drop target
        //if(e.getDropAction() == DnDConstants.ACTION_MOVE) {
        //log.info("Success");
        //}
    }

    /**
     * DragSourceListener impl.
     *
     * @param e the event
     */
    public void dragEnter(DragSourceDragEvent e) {

        //log.info("DragArea enter " + e);
        DragSourceContext context = e.getDragSourceContext();

        //intersection of the users selected action, and the source and target actions
        int myaction = e.getDropAction();

        if ((myaction & fDragAction) != 0) {
            context.setCursor(DragSource.DefaultCopyDrop);
        } else {
            context.setCursor(DragSource.DefaultCopyNoDrop);
        }
    }

    /**
     * We do nothing
     * DragSourceListener impl.
     *
     * @param e the event
     */
    public void dragOver(DragSourceDragEvent e) {

        /*
        DragSourceContext context = e.getDragSourceContext();
        int               sa      = context.getSourceActions();
        int               ua      = e.getUserAction();
        int               da      = e.getDropAction();
        int               ta      = e.getTargetActions();

        log.info("dl dragOver source actions" + sa + " user action" + ua + " drop actions" + da
                 + " target actions" + ta);
        */
    }

    /**
     * DragSourceListener
     *
     * @param e the event
     */
    public void dragExit(DragSourceEvent e) {

        //log.info("DragArea exit " + e);
        // so that we change back to NO no can drop when we leave target area
        DragSourceContext context = e.getDragSourceContext();

        context.setCursor(DragSource.DefaultCopyNoDrop);
    }

    /**
     * DragSourceListener
     * for example, press shift during drag to change to
     * a link action
     *
     * @param e the event
     */
    public void dropActionChanged(DragSourceDragEvent e) {

        DragSourceContext context = e.getDragSourceContext();

        context.setCursor(DragSource.DefaultCopyNoDrop);
    }
}    // End DragSourceDecorator
