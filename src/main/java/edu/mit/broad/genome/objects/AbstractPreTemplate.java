/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.objects;

import edu.mit.broad.genome.NotImplementedException;
import edu.mit.broad.genome.math.Vector;

import java.awt.*;

/**
 * @author Aravind Subramanian
 */
public abstract class AbstractPreTemplate extends AbstractObject implements PreTemplate {

    /**
     * Class constructor
     *
     * @param name
     */
    protected AbstractPreTemplate(String name) {
        super(name);
    }

    public int[] getProfilePositionsOrdered() {
        throw new NotImplementedException();
    }

    public Vector synchProfile(final Vector profile) {
        throw new NotImplementedException();
    }

    public Template.Item[] getItemsOrderedByProfilePos() {
        throw new NotImplementedException();

    }

    public Template.Item[] getItemsOrderedByClassFirstAndThenProfilePos() {
        throw new NotImplementedException();
    }

    public String getQuickInfo() {
        return null;
    }

    public void makeImmutable() {
        throw new NoSuchMethodError();
    }

    public String getClassName(int i) {
        throw new NoSuchMethodError();
    }

    public Class getClass(Item item) {
        throw new NoSuchMethodError();
    }

    public Template.Class getClass(int i) {
        throw new NoSuchMethodError();
    }

    public int getClassIndex(Template.Class cl) {
        throw new NoSuchMethodError();
    }

    public int getNumClasses() {
        throw new NoSuchMethodError();
    }

    public boolean isMemberClass(Template.Class cl) {
        throw new NoSuchMethodError();
    }

    public String getClassOfInterestName() {
        throw new NoSuchMethodError();
    }

    public Color getItemColor(int itemProfilePos) {
        throw new NoSuchMethodError();
    }

    public int getClassOfInterestIndex() {
        throw new NoSuchMethodError();
    }

    public Template cloneDeep(String newName) {
        throw new NoSuchMethodError();
    }

    public boolean isAscendingProfilePositions() {
        throw new NoSuchMethodError();
    }

    public Template.Item getItemByProfilePos(int primPos) {
        throw new NoSuchMethodError();
    }

    public int getNumItems() {
        throw new NoSuchMethodError();
    }

    public Vector toVector() {
        throw new NoSuchMethodError();
    }

    public String getAsString(boolean gc) {
        throw new NoSuchMethodError();
    }

    public Vector[] splitByTemplateClass(final Vector profile) {
        throw new NoSuchMethodError();
    }
}
