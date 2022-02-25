/*
 * Copyright (c) 2003-2022 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.objects;

import edu.mit.broad.genome.Printf;
import edu.mit.broad.genome.utils.ColorUtils;
import edu.mit.broad.genome.utils.ImmutedException;
import gnu.trove.TIntObjectHashMap;

import java.awt.*;
import java.util.*;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * IMP IMP: algorithms etc should not rely on the aux business - they should simpley work on the
 * specified template
 * <p/>
 * A Template Object.
 * <p/>
 * A Template is made up of a collection of TemplateItem's.
 * A TemplateItem can belong to one or more Template.Class,
 * but must belong to at least one Template.Class.
 * <p/>
 * <bold> Implementation Notes </bold><br>
 * Different from genecluster Template - The class labels in this template can
 * be accessed through <code>getClass(i).getName()</code> rather than directly
 * through this class.
 *
 * @author Michael Angelo
 * @author Aravind Subramanian (extensively re-engineered)
 */
public class TemplateImpl extends AbstractTemplate {

    /**
     * IMP IMP IMP IMP
     * <p/>
     * If adding any class vars make sure to check clone/factory methods here and in
     * TemplateFactory
     * <p/>
     * Elements are Template.Class objects - the classes that TemplateItems belong to
     * <p/>
     * <p/>
     * Elements are Template.Class objects - the classes that TemplateItems belong to
     */

    /**
     * Elements are Template.Class objects - the classes that TemplateItems belong to
     */
    private ArrayList fClasses;

    /**
     * Elements are Template.Item objects -> List of TemplateItems
     */
    private ArrayList fItems;

    /**
     * Flag for whether this Template is continuous or not
     */
    private boolean fContinuous;

    /**
     * Flag for whether this Template can be changed or not
     */
    private boolean fImmutable = false;

    /* By Default the Class of interest is the first one */
    private int fCoi = 0;


    private boolean fAux;

    /**
     * Class Constructor.
     * Creates a new Template.
     */
    protected TemplateImpl(final String name) {
        super(name);
        fClasses = new ArrayList();
        fItems = new ArrayList();
    }

    /**
     * Dummy constructor. Subclasses must take care to fill as needed
     */
    protected TemplateImpl() {
    }

    protected Template.Item _getItemAsIs(final int itemIndex) {
        return (Template.Item) fItems.get(itemIndex);
    }

    protected Template.Item[] _getItems() {
        return (Item[]) fItems.toArray(new Item[fItems.size()]);
    }

    /**
     * Label of the class at specified location
     */
    public String getClassName(final int i) {
        return ((Template.Class) fClasses.get(i)).getName();
    }

    /**
     * @return
     * @maint Might need updating if class vars are added
     * deep clones this Template
     */
    public Template cloneDeep(final String newName) {

        final TemplateImpl newt = new TemplateImpl(newName);
        newt.fImmutable = false; // IMP notice - we do this as often the cloned template is used in a diff way
        newt.fContinuous = this.fContinuous;

        for (int i = 0; i < this.getNumClasses(); i++) {
            Class cl = this.getClass(i);
            ClassImpl newcl = new ClassImpl(cl.getName());
            newt.add(newcl);
            for (int j = 0; j < cl.getSize(); j++) {
                Item newitem = cl.getItem(j).cloneDeep();
                newcl.add(newitem);
                newt.add(newitem);
            }
        }

        newt.fCoi = this.fCoi;
        return newt;
    }

    /**
     * coi -> the class we are interested in finding markers for
     *
     * @return
     */
    public int getClassOfInterestIndex() {
        return fCoi;
    }

    public boolean isContinuous() {
        return fContinuous;
    }

    public boolean isCategorical() {
        return !fContinuous;
    }

    public Template.Class getClass(final int classIndex) {
        return (Template.Class) fClasses.get(classIndex);
    }

    public boolean isMemberClass(final Template.Class cl) {
        int index = fClasses.indexOf(cl);
        return index != -1;
    }

    public int getClassIndex(final Template.Class cl) {
        return fClasses.indexOf(cl);
    }

    public int getNumItems() {
        return fItems.size();
    }

    public int getNumClasses() {
        return fClasses.size();
    }

    protected void setAux(final boolean aux) {

        // sanity checks
        if (aux) {
            if (!StringUtils.contains(getName(), "#")) {
                throw new IllegalStateException("Cannot make aux as the name has no # in it. Name: " + getName());
            }
        } else {
            if (StringUtils.contains(getName(), "#")) {
                throw new IllegalStateException("Non-aux template cannot have # in its name. Name: " + getName());
            }
        }

        this.fAux = aux;
    }

    public boolean isAux() {

        if (getName().indexOf('#') != -1) {
            return true;
        }

        return fAux;
    }

    // imp that it is protected (but cant as in interface - @todo improve)
    public void setClassOfInterestIndex(final int coi) {
        checkImmutable();
        this.fCoi = coi;
    }

    public String getClassOfInterestName() {
        return getClass(fCoi).getName();
    }

    /**
     * Add specified Class to this Template.
     * Class names must be unique
     */
    protected void add(final Class cl) {
        if (cl == null) {
            throw new IllegalArgumentException("Param cl cannot be null");
        }

        checkImmutable();
        // unique class name check
        for (int i = 0; i < getNumClasses(); i++) {
            if (getClass(i).getName().equals(cl.getName())) {
                throw new IllegalArgumentException("Class with this name already exists: " + cl.getName() +
                        " . The Classes in a Template must be unique");
            }

        }
        fClasses.add(cl);
    }

    /**
     * Add specified Template.Item to this Template
     */
    protected void add(final Template.Item aItem) {

        if (aItem == null) {
            throw new IllegalArgumentException("Param aItem cannot be null");
        }

        checkImmutable();

        if (isContinuous()) {
            try {
                Float.parseFloat(aItem.getId());
            } catch (NumberFormatException e) {
                throw new NumberFormatException("Template is numeric but Template.Item asked to be added was not Float-parsable "
                        + e.toString());
            }
        }

        fItems.add(aItem);
    }

    /**
     * Only valid if there is a collection of Template.Class's associated with this Template.
     * <p/>
     * The method cycles (in order of addition) through all its member TemplateItems.
     * In the first progress run, a list of unique TemplateItem Id's.
     * is generated. The size of this list must correspond to the
     * size of the Template.Class collection.
     * <p/>
     * Each Id is then assigned to one Template.Class (again, in order).
     * <p/>
     * The member TemplateItems are then again cycled through, and assigned to
     * a Template.Class (based on the Id - Template.Class map produced in the previous run).
     * <p/>
     * Thus, the result, is that each Template.Class is now associated with a collection of TemplateItem's.
     * <p/>
     * The Template.Class does NOT have to be the same as the TemplateItems id.
     * <p/>
     * IMP:
     * DONT call this method if you dont want automatic id-class assignments
     * Instead do a custom impl of addition of items to classes
     * Example: Template with items: 1 1 0 0 1 0 gets the first class with items with Id 1
     * and Template with items: 0 1 1 0 1 gets the first class with items with Id 0
     * Might want to do a custom assignment to avoid this.
     */
    static boolean warned = false;

    private boolean checked;

    protected void runChecks() {
        if (!checked) {
            runChecksInit();
            runChecksPost();
            checked = true;
        }
    }

    protected ArrayList runChecksInit() {

        if (this.fClasses == null) {
            throw new RuntimeException("Cannot call method as Template has no associated Template.Class's");
        }

        if (this.fItems == null) {
            throw new RuntimeException("Cannot call method as Template has no associated Template.Items's");
        }

        // Check 1: check to make sure profile pos in template hasnt been reused (i.e each item needs a uniq profile pos)
        final List ual = new ArrayList(getNumItems());

        for (int i = 0; i < getNumItems(); i++) {
            Template.Item item = (Template.Item) fItems.get(i);
            Integer primPos = new Integer(item.getProfilePosition());
            //log.debug("item = " + item);
            if (ual.contains(primPos)) {
                throw new RuntimeException("ProfilePosition has been reused! Position = " + primPos
                        + " by Item: " + Printf.outs(item));
            } else {
                ual.add(primPos);
            }
        }

        // Check 2: unique Item id's
        final ArrayList unique_item_ids_ordered_by_profile_pos = new ArrayList();
        final Template.Item[] items_ordered = getItemsOrderedByProfilePos();
        for (int i = 0; i < items_ordered.length; i++) {
            if (!unique_item_ids_ordered_by_profile_pos.contains(items_ordered[i].getId())) {
                unique_item_ids_ordered_by_profile_pos.add(items_ordered[i].getId());
            }
        }

        // could barf for continuous templates
        if (isContinuous() == false && unique_item_ids_ordered_by_profile_pos.size() != fClasses.size()) {
            StringBuffer err = new StringBuffer("Mismatched numbers between unique item id's: ").append(unique_item_ids_ordered_by_profile_pos.size());
            err.append(' ').append(unique_item_ids_ordered_by_profile_pos).append(" and number of Template.Class's: ").append(fClasses.size()).append('\n');
            for (int c = 0; c < fClasses.size(); c++) {
                err.append(((Class) fClasses.get(c)).getName()).append(' ');
            }
            throw new IllegalArgumentException(err.toString());
        }

        return unique_item_ids_ordered_by_profile_pos;
    }


    protected void runChecksPost() {

        Set classNames = new HashSet();
        // check: final (after adding items): Obvious error such as 2 classes and items swapped names
        for (int c = 0; c < fClasses.size(); c++) {
            Class cl = (Class) fClasses.get(c);

            if (classNames.contains(cl.getName())) {
                throw new IllegalArgumentException("Duplicate class names: " + cl.getName() + "\n" + classNames);
            } else {
                classNames.add(cl.getName());
            }

            if (cl.getSize() == 0) {
                throw new IllegalStateException("Empty class: " + cl.getName() + " " + cl.getSize() + " total # items: " + getNumItems());
            }
            Item item = cl.getItem(0); // some item from this class
            for (int r = 0; r < fClasses.size(); r++) {
                Class clr = (Class) fClasses.get(r);
                if (clr.getName().equals(item.getId())) {
                    if (r != c) {
                        throw new IllegalStateException("Something obviously wrong items and classes mismatched: \n" + Printf.outs(this));
                    }
                }
            }
        }

    }

    protected void assignItems2ClassInOrder() {

        // Check3: ensure that class-item assignment has not already been done
        for (int i = 0; i < fClasses.size(); i++) {
            if (getClass(i).getSize() != 0) {
                throw new RuntimeException("Items already seem to be assigned to class: "
                        + getClass(i).getName());
            }
        }

        final ArrayList unique_item_ids_ordered_by_profile_pos = runChecksInit();

        // Check 4: Make sure the profile order of items is the same as the order of classes

        // ok, all looks good, associate ids and classes
        // @note Here is where the first id is automatically (and blindly) associated with the first class
        // This may not be what you want always!!
        final Hashtable idClassHash = new Hashtable();

        for (int i = 0; i < unique_item_ids_ordered_by_profile_pos.size(); i++) {
            idClassHash.put(unique_item_ids_ordered_by_profile_pos.get(i), fClasses.get(i));
        }

        // finally (whew), the actual assignment
        for (int i = 0; i < getNumItems(); i++) {
            final Template.Item item = (Template.Item) fItems.get(i);
            final ClassImpl cl = (ClassImpl) idClassHash.get(item.getId());
            cl.add(item);
        }

        runChecksPost();

    }

    /**
     * Set this Template as continuous.
     * Continuous templates are for instance a gene's expression profile.
     * There should be as many classes as items in continuous templates.
     */
    protected void setContinuous(final boolean cont) {

        checkImmutable();
        this.fContinuous = cont;

        if (!cont) {
        } else {
            // check
            if (getNumItems() != getNumClasses()) {
                throw new IllegalStateException("Cannot make template continuous. # items: "
                        + getNumItems() + " is not equal to # classes: "
                        + getNumClasses());
            }

            // check to ensure that all items are numeric
            Item it = null;

            try { // try block for mor einformative of error messages
                for (int i = 0; i < getNumItems(); i++) {
                    it = _getItemAsIs(i);
                    it.floatValue(); // a check
                }
            } catch (NumberFormatException e) {
                throw new IllegalStateException("Item: " + it.getId() + " at pos: " + it.getProfilePosition()
                        + " is not numeric. The Template cannot be set as continuous");
            }
        }
    }

    private boolean fSampleNamesFromDataset;

    protected void setSampleNamesFromDataset(final boolean set) {
        this.fSampleNamesFromDataset = set;
    }

    /**
     * all public mut methods in this class mustt check
     * as must all factory methods
     */
    public void makeImmutable() {
        fImmutable = true;
        runChecks();
    }

    private void checkImmutable() {

        if (fImmutable) {
            throw new ImmutedException();
        }
    }

    /**
     * A Template.Class is the replacement for ma's ClusterData.
     * <p/>
     * Accessor methods are public
     * Modifier methods are private/protected
     */
    public static class ClassImpl implements Template.Class {

        /**
         * The name of this Template class
         */
        protected String fName;

        /**
         * list of Template.Items
         */
        protected ArrayList fItems;

        /**
         * Classs constructor
         */
        public ClassImpl(final String className) {
            this.fName = className;
            fItems = new ArrayList();
        }

        /**
         * The name of this Template.Class
         */
        public String getName() {
            return fName;
        }

        public Template.Item getItem(int i) {
            return (Template.Item) fItems.get(i);
        }

        public Template.Item[] getItemsOrderedByProfilePos() {
            final Item[] items = (Item[]) fItems.toArray(new Item[fItems.size()]);
            final TIntObjectHashMap map = hashProfilePosItemMap(items);
            final int[] pp = getProfilePositionsSorted(map);

            final Item[] ret = new Item[pp.length];
            for (int i = 0; i < pp.length; i++) {
                ret[i] = (Item) map.get(pp[i]);
            }

            return ret;
        }

        /**
         * Number of TemplateItems that are members
         */
        public int getSize() {
            return fItems.size();
        }

        public int hashCode() {
            return fName.hashCode();
        }

        public String getMembershipInfo() {

            StringBuffer buf = new StringBuffer("Class: ").append(this.getName()).append('\n');
            buf.append("Members: \t");

            for (int i = 0; i < this.getSize(); i++) {

                int profile_pos = this.getItem(i).getProfilePosition();
                if (profile_pos != -1) {
                    buf.append(this.getItem(i).getId()).append('[').append(i).append(',').append(profile_pos).append(']');

                } else {
                    buf.append(this.getItem(i).getId()).append('[').append(this.getItem(i).
                            getProfilePosition()).append(']');
                }

                if (i != this.getSize() - 1) {
                    buf.append(',');
                }
            }

            return buf.toString();
        }

        // IMP: Intentionally protected
        protected void add(final Template.Item item) {
            if (item == null) {
                throw new IllegalArgumentException("item cannot be null");
            }
            this.fItems.add(item);
        }

    }    // End Template.Class

    /**
     * Class Item
     *
     * @author Aravind Subramanian
     * @version %I%, %G%
     */
    public static class ItemImpl implements Template.Item {

        /**
         * Identifier for this Item
         */
        private String fId;

        /**
         * Position of this item in the Template vector
         * As a debugging aid, magic marker of -999 means its not been initialized
         */

        protected int fProfilePos = -999;


        /**
         * Class Constructor.
         */
        private ItemImpl(final String id, final int profilePos) {
            init(id, profilePos);
        }

        public static ItemImpl createItem(final String id, final int profilePos) {
            return new ItemImpl(id, profilePos);
        }

        private void init(final String id, final int profilePos) {
            this.fId = id;
            this.fProfilePos = profilePos;
        }

        // aka clone deep
        public Item cloneDeep() {
            return new ItemImpl(this.fId, this.fProfilePos);
        }

        public String getId() {
            return fId;
        }

        public int getProfilePosition() {
            return fProfilePos;
        }

        /**
         * Obviously works only if numeric
         *
         * @return Float value of this Item
         * @throws NumberFormatException
         */
        public float floatValue() throws NumberFormatException {
            return Float.parseFloat(fId);
        }

        public boolean equals(final Object obj) {
            if (obj instanceof Item) {
                final Item io = (Item) obj;
                if (io.getId().equals(fId) && io.getProfilePosition() == fProfilePos) {
                    return true;
                }
            }

            return false;
        }

    }    // End Item

    private static final Color DEFAULT_CLASS0_COLOR = Color.LIGHT_GRAY;
    private static final Color DEFAULT_CLASS1_COLOR = Color.ORANGE;

    private static final Color[] DEFAULT_COLORS = new Color[]{DEFAULT_CLASS0_COLOR,
            DEFAULT_CLASS1_COLOR,
            Color.YELLOW,
            Color.MAGENTA,
            Color.GREEN,
            Color.CYAN,
            Color.PINK};

    private TIntObjectHashMap fItemProfilePosColorScheme;

    public Color getItemColor(final int itemProfilePos) {

        if (fItemProfilePosColorScheme == null) {
            makeAutoColors();
        }

        Object obj = fItemProfilePosColorScheme.get(itemProfilePos);
        if (obj == null) {
            log.warn("No color for item at profile pos: {} existing pos-color scheme size: {}", itemProfilePos, Printf.outs(fItemProfilePosColorScheme));
            return Color.WHITE;
        }

        return (Color) obj;
    }

    private void makeAutoColors() {
        this.fItemProfilePosColorScheme = new TIntObjectHashMap();

        if (getNumClasses() == 1 || isContinuous()) { // for 1 class templates and continuous templates, dont color
            for (int i = 0; i < getNumItems(); i++) {
                fItemProfilePosColorScheme.put(_getItemAsIs(i).getProfilePosition(), Color.WHITE);
            }
        } else if (getNumClasses() == 2) {
            Class c0 = getClass(0);
            for (int i = 0; i < c0.getSize(); i++) {
                fItemProfilePosColorScheme.put(c0.getItem(i).getProfilePosition(), DEFAULT_CLASS0_COLOR);
            }

            Class c1 = getClass(1);
            for (int i = 0; i < c1.getSize(); i++) {
                fItemProfilePosColorScheme.put(c1.getItem(i).getProfilePosition(), DEFAULT_CLASS1_COLOR);
            }

            //log.debug("## doing 2 class color fill: " + c0.getSize() + " " + c1.getSize());

        } else {
            //log.debug("## doing multi class color fill: " + getNumClasses());
            // first try using standard colors, before generating random ones
            Color[] colors = ColorUtils.pickRandomColors(getNumClasses(), DEFAULT_COLORS);
            for (int c = 0; c < getNumClasses(); c++) {
                Class cl = getClass(c);
                //log.debug("class: " + cl.getName() + " " + cl.getSize());
                for (int i = 0; i < cl.getSize(); i++) {
                    fItemProfilePosColorScheme.put(cl.getItem(i).getProfilePosition(), colors[c]);
                }
            }
        }
    }
}
