/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.objects;

import edu.mit.broad.genome.math.Vector;

import java.awt.*;

/**
 * @author Aravind Subramanian
 */
public interface Template extends PersistentObject {


    /**
     * IMP IMP IMP
     * Items has ve no intirinsic ordering -> they onluy have profile positions
     * Clases DO have an ordering -> class1 , class 2 ...
     */

    public static final String NUMERIC = "#numeric";
    public static final String NUMERIC_1 = "# numeric";
    public static final String PROBES = "#probes";

    public void makeImmutable();

    public Color getItemColor(int itemProfilePos);

    public Template cloneDeep(final String newName);

    public boolean isAscendingProfilePositions();

    public boolean isAux();

    /**
     * Is this template continuous?
     */
    public boolean isContinuous();

    public boolean isCategorical();

    public Class getClass(final Item item);

    public Template.Class getClass(final int classIndex);

    public String getClassName(final int classIndex);

    public int getClassIndex(Template.Class cl);

    public int getNumClasses();

    public boolean isMemberClass(final Template.Class cl);

    public String getClassOfInterestName();

    /**
     * coi -> the class we are interested in finding markers for
     */
    public int getClassOfInterestIndex();

    public int[] getProfilePositionsOrdered();

    public Template.Item getItemByProfilePos(int profilePos);

    public Template.Item[] getItemsOrderedByProfilePos();

    public Template.Item[] getItemsOrderedByClassFirstAndThenProfilePos();

    public int getNumItems();

    public Vector toVector();

    /**
     * Munge this Template object into a String. Just to keep things familiar
     * use the cls format.
     */
    public String getAsString(boolean gcFormat);

    /**
     * Splits a Vector by the specified template. Vector gets split into as many sub-vectors
     * as there are classes in the Template.
     * Template ordering is respected (i.e classes neednt be one after the other for this
     * method to work)
     * For example: Vector <- (4, 6, 8, 2, 4)
     * when split by Template 1 1 0 1 0
     * becomes: v0 <- 8, 4
     * and      v1 <- 4, 6, 2
     * <p/>
     * Not all elements of the vector might be used. e.g  only those profile
     * positions defined in the template are used. And hence No ned for the profile
     * length to equal the number of items in the Template
     */
    public Vector[] splitByTemplateClass(final Vector profile);

    public Vector synchProfile(final Vector profile);

    /**
     * A Template.Class is the replacement for ma's ClusterData.
     * <p/>
     * Accessor methods are public
     * Modifier methods are private/protected
     */
    public static interface Class {

        /**
         * The name of this Template.Class
         */
        public String getName();

        public Template.Item getItem(final int i);

        public Template.Item[] getItemsOrderedByProfilePos();

        /**
         * Number of TemplateItems that are members
         */
        public int getSize();

        public String getMembershipInfo();

    }    // End Template.Class


    /**
     * Class Item
     *
     * @author Aravind Subramanian
     * @version %I%, %G%
     */
    public static interface Item {

        // aka clone deep
        public Template.Item cloneDeep();

        public String getId();

        public int getProfilePosition();

        public float floatValue() throws NumberFormatException;

    }// End class Item

} // End interface Template
