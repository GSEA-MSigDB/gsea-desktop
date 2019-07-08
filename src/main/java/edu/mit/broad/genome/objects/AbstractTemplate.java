/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.objects;

import edu.mit.broad.genome.Printf;
import edu.mit.broad.genome.math.Vector;
import edu.mit.broad.genome.math.XMath;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TObjectIntHashMap;

import java.util.*;

/**
 * @author Aravind Subramanian
 */
public abstract class AbstractTemplate extends AbstractObject implements Template {

    AbstractTemplate(String name) {
        super(name);
    }

    /**
     * Dummy constructor. Subclasses must take care to fill as needed
     */
    protected AbstractTemplate() {
    }

    public String getQuickInfo() {
        StringBuffer buf = new StringBuffer();
        if (isContinuous()) {
            buf.append(getNumItems()).append("=>numeric");
        } else if (getNumClasses() == 2) {
            buf.append(getNumItems()).append(" samples").append('(').append(getClass(0).getSize()).append(',').append(getClass(1).getSize()).append(')').append("=>").append(getNumClasses()).append(" classes");
        } else if (getNumClasses() == 1) {
            buf.append(getNumItems()).append(" samples").append("=>").append(getNumClasses()).append(" class");
        } else {
            buf.append(getNumItems()).append(" samples").append("=>").append(getNumClasses()).append(" classes");
        }

        return buf.toString();
    }

    protected abstract Item _getItemAsIs(int index);

    protected abstract Item[] _getItems();

    private String[] _getClassNamesOrderedByProfilePos_CAREFUL() {
        final List classNames = new ArrayList();

        // @note for profile pos blindly as profile posn need not start from 0 to n-1
        // they can be anything

        final int[] ordered_profile_pos = getProfilePositionsOrdered();

        for (int i = 0; i < ordered_profile_pos.length; i++) {
            final String cn = getItemByProfilePos(ordered_profile_pos[i]).getId(); // @note by pos and not by index
            if (classNames.contains(cn) == false) {
                classNames.add(cn);
            }
        }

        if (classNames.size() != getNumClasses()) {
            throw new IllegalStateException("Odd: className.length: " + classNames.size() + " getNumClasses: " + getNumClasses() + " " + classNames);
        }

        return (String[]) classNames.toArray(new String[classNames.size()]);
    }

    public Class getClass(final Item item) {

        for (int i = 0; i < getNumClasses(); i++) {
            Class cl = getClass(i);
            for (int c = 0; c < cl.getSize(); c++) {
                if (item.equals(cl.getItem(c))) {
                    return cl;
                }
            }
        }

        throw new IllegalArgumentException("No membership for item in any of the classes: " + item.getId() + " " + item.getProfilePosition());
    }

    private Vector fTemplateAsVector;

    public Vector toVector() {

        if (fTemplateAsVector == null) {

            Vector v = new Vector(this.getNumItems());

            if (isContinuous()) {
                for (int i = 0; i < getNumItems(); i++) {
                    v.setElement(i, _getItemAsIs(i).floatValue());
                }
            } else {
                // for max safety lets enforce that even categorical templates need their elements to be
                // "numbers" before this method can be used. See below for an alternate impl that i felt wasnt as safe.

                for (int i = 0; i < getNumItems(); i++) {
                    v.setElement(i, _getItemAsIs(i).floatValue());
                }
            }

            this.fTemplateAsVector = v;
            this.fTemplateAsVector.setImmutable();
        }

        return fTemplateAsVector;
    }

    /**
     * Splits a Vector by the specified template. Vector gets split into as many sub-vectors
     * as there are classes in the Template.
     * Template ordering is respected (i.e classes need not be one after the other for this
     * method to work)
     * For example: Vector <- (4, 6, 8, 2, 4)
     * when split by Template 1 1 0 1 0
     * becomes: v0 <- 8, 4
     * and      v1 <- 4, 6, 2
     * <p/>
     * Not all elements of the vector might be used. e.g  only those profile
     * positions defined in the template are used. And hence No need for the profile
     * length to equal the number of items in the Template
     */
    public Vector[] splitByTemplateClass(final Vector profile) {

        final int numClasses = getNumClasses();
        final Vector[] vectors = new Vector[numClasses];

        // @note IMP: within a class the items need not be ordered by asc profile pos
        for (int i = 0; i < numClasses; i++) {
            Template.Class cl = getClass(i); // @note IMP simply get the class NOT get teh class by first profile pos
            final int classSize = cl.getSize();
            Vector v = new Vector(classSize);

            for (int p = 0; p < classSize; p++) {
                int pos = cl.getItem(p).getProfilePosition();
                v.setElement(p, profile.getElement(pos));
            }

            vectors[i] = v;
        }

        return vectors;
    }

    /**
     * Munge this Template object into a String. Just to keep things familiar
     * use the cls format.
     * Example:<br<
     * <pre>
     * 53 3 1
     * # Breast Bladder Renal
     * 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1 1 1 1 1 1 1 1 1 1 2 2 2 2 2 2 2 2 2 2 2
     * </pre>
     */
    public String getAsString(final boolean gcFormat) {
        if (this.isContinuous()) {
            return _getAsString_cont();
        } else {
            return _getAsString_cat(gcFormat);
        }
    }

    private String _getAsString_cont() {

        StringBuffer s = new StringBuffer();
        s.append(NUMERIC).append('\n');
        s.append('\n');
        s.append('#').append(this.getName()).append('\n');

        Template.Item[] items = getItemsOrderedByProfilePos();

        for (int i = 0; i < items.length; i++) {
            s.append(items[i].floatValue());

            if (i != this.getNumItems() - 1) {
                s.append(" ");
            }
        }

        s.append('\n');
        return s.toString();
    }

    private String _getAsString_cat(final boolean gcFormat) {

        // For categorical templates
        StringBuffer s = new StringBuffer();
        s.append(this.getNumItems());
        s.append(" ");
        s.append(this.getNumClasses());
        s.append(" ");
        s.append("1");    // dont know what this is?? but never seem anything other than 1
        s.append("\n");
        s.append("# ");

        // item order determines the class order
        final String[] classNames = _getClassNamesOrderedByProfilePos_CAREFUL(); // @note
        for (int i = 0; i < classNames.length; i++) {
            s.append(classNames[i]);

            if (i != classNames.length - 1) {
                s.append(" ");
            }
        }

        s.append("\n");

        final Template.Item[] items_ordered = getItemsOrderedByProfilePos();

        // do 0 1 2 .. thing
        if (gcFormat) {
            TObjectIntHashMap idCntMap = new TObjectIntHashMap();
            int classCnt = 0;
            for (int i = 0; i < items_ordered.length; i++) {
                Item item = items_ordered[i];
                if (!idCntMap.containsKey(item.getId())) {
                    idCntMap.put(item.getId(), classCnt);
                    classCnt++;
                }
            }

            for (int i = 0; i < items_ordered.length; i++) {
                int cnt = idCntMap.get(items_ordered[i].getId());
                s.append(cnt);
                if (i != items_ordered.length - 1) {
                    s.append(" ");
                }
            }

        } else {
            for (int i = 0; i < items_ordered.length; i++) {
                s.append(items_ordered[i].getId());
                if (i != items_ordered.length - 1) {
                    s.append(" ");
                }
            }
        }

        s.append("\n");
        return s.toString();
    }

    public boolean isAscendingProfilePositions() {

        int[] profilePositions_as_is = new int[getNumItems()];
        for (int i = 0; i < getNumItems(); i++) {
            Item item = _getItemAsIs(i);
            profilePositions_as_is[i] = item.getProfilePosition();
        }

        return XMath.isAscending(profilePositions_as_is);
    }

    // makes the profile elements and the template items in order
    public Vector synchProfile(final Vector profile) {
        final Vector synched = new Vector(getNumItems());
        for (int i = 0; i < getNumItems(); i++) {
            int pos = _getItemAsIs(i).getProfilePosition();
            synched.setElement(i, profile.getElement(pos));
        }

        return synched;
    }

    // cant do this for profile pos blindly as profile positions need not start from 0 to n-1
    // they can be anything
    public Template.Item[] getItemsOrderedByProfilePos() {
        int[] sorted_profile_positions = getProfilePositionsOrdered();
        final List list = new ArrayList();
        for (int i = 0; i < sorted_profile_positions.length; i++) {
            list.add(getItemByProfilePos(sorted_profile_positions[i]));
        }

        return (Item[]) list.toArray(new Item[list.size()]);
    }

    public Template.Item[] getItemsOrderedByClassFirstAndThenProfilePos() {

        List list = new ArrayList();

        for (int c = 0; c < getNumClasses(); c++) {
            Template.Class cl = getClass(c); // @note that the class order is set on construction and NOT by profile
            Template.Item[] items = cl.getItemsOrderedByProfilePos();
            for (int i = 0; i < items.length; i++) {
                list.add(items[i]);
            }
        }

        return (Item[]) list.toArray(new Item[list.size()]);
    }

    public Template.Item getItemByProfilePos(final int profilePos) {

        _initProfilePosItemMap();

        Object obj = fProfilePosItemMap.get(profilePos);
        if (obj == null) {
            Printf.out(fProfilePosItemMap.keys());
            throw new IllegalArgumentException("No such profile pos: " + profilePos + " # points: " + getNumItems());
        }

        return (Template.Item) obj;
    }

    public int[] getProfilePositionsOrdered() {
        _initProfilePosItemMap();
        return getProfilePositionsSorted(fProfilePosItemMap);
    }

    protected static TIntObjectHashMap hashProfilePosItemMap(final Template.Item[] items) {
        final TIntObjectHashMap profilePosItemMap = new TIntObjectHashMap();
        for (int i = 0; i < items.length; i++) {
            if (profilePosItemMap.containsKey(items[i].getProfilePosition())) {
                throw new IllegalStateException("Duplicate profile positions: " + items[i].getProfilePosition());
            }

            if (profilePosItemMap.get(items[i].getProfilePosition()) != null) {
                throw new IllegalStateException("Multiple items at same profile position in template: " + items[i].getProfilePosition());
            } else {
                profilePosItemMap.put(items[i].getProfilePosition(), items[i]);
            }
        }

        return profilePosItemMap;

    }

    protected static Item[] getProfilePositionsSorted(final Item[] items) {
        final TIntObjectHashMap map = hashProfilePosItemMap(items);
        final Item[] sorted_items = new Item[items.length];
        final int[] sorted_profile_positons = getProfilePositionsSorted(map);

        for (int i = 0; i < sorted_profile_positons.length; i++) {
            sorted_items[i] = (Item) map.get(sorted_profile_positons[i]);
        }

        return sorted_items;
    }

    protected static int[] getProfilePositionsSorted(final TIntObjectHashMap profilePosItemMap) {
        int[] sorted_profile_positions = profilePosItemMap.keys();
        Arrays.sort(sorted_profile_positions);
        return sorted_profile_positions;
    }

    // inited lazilly
    private TIntObjectHashMap fProfilePosItemMap;

    private void _initProfilePosItemMap() {
        if (fProfilePosItemMap == null) {
            fProfilePosItemMap = hashProfilePosItemMap(_getItems());
        }
    }

} // End class AbstractTemplate
