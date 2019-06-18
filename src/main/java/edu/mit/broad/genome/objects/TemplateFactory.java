/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.objects;

import edu.mit.broad.genome.Constants;
import edu.mit.broad.genome.NamingConventions;
import edu.mit.broad.genome.Printf;
import edu.mit.broad.genome.math.Matrix;
import edu.mit.broad.genome.math.Vector;
import edu.mit.broad.genome.objects.strucs.DatasetTemplate;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * A Template object that is mutable i.e has add() methods.
 * Generally used only by the ClsParser and methods(in here)that make new Templates.
 * <p/>
 * Also, factor methods for temnplates kept here so that we can control generation
 * (and access to non public fields needed for generation)
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
public class TemplateFactory extends TemplateImpl {

    private static final Logger klog = Logger.getLogger(TemplateFactory.class);

    private static final String VS = "_versus_";

    /**
     * Class Constructor.
     * Creates a new TemplateFactory.
     */
    private TemplateFactory() {
    }

    /**
     * The dataset returned has its columns in the same order as the item's
     * primary positions in the template. So, the dataset returned is now synched to the
     * template. (BUT remember that the input template's profile positions are NO longer synched
     * with the return dataset (if it was extracted)).
     *
     * @param fullDs
     * @param origT
     * @return
     * @note ALSO contiguize the classes (and ofcourse dataset)
     * @note DatasetTemplate needed as the extracted ds and incoming t may not be synched anymore
     * They WILL have the same number of columns/items but the profile positions of the items
     * will refer to profile positions in the old (unextracted) dataset. If all you care about is
     * the extracted dataset, ignore the modified template.
     */
    public static synchronized DatasetTemplate extract(final Dataset fullDs,
                                                       final Template origT,
                                                       final boolean verbose) {

        if (fullDs == null) {
            throw new IllegalArgumentException("Parameter fullDs cannot be null");
        }

        if (origT == null) {
            throw new IllegalArgumentException("Parameter template cannot be null");
        }

        for (int i = 0; i < origT.getNumClasses(); i++) {
            if (origT.getClass(i).getSize() == 0) {
                throw new IllegalArgumentException("In template: " + origT.getName() + " 0 members in class: " + origT.getClass(i).getName());
            }
        }

        // no synching needed if:
        // 1) the dataset and template are of identical length
        // 2) the primary positions of the template are in ascending order
        // as an opt check for this and return unchanged ds if true
        if (fullDs.getNumCol() == origT.getNumItems() && origT.isAscendingProfilePositions()) {
            if (verbose) {
                klog.info("Already a synched dataset-template -- NO extracting done");
            }
            return new DatasetTemplate(fullDs, origT); // no extraction needed
        }

        if (verbose) {
            klog.info("Extracting ds: " + fullDs.getName() + " by template: " + origT.getName());
        }

        final Matrix matrix = new Matrix(fullDs.getNumRow(), origT.getNumItems());
        final List colNames = new ArrayList(origT.getNumItems());
        final Template.Item[] newItems = new Template.Item[origT.getNumItems()];

        final Template.Item[] orig_items = origT.getItemsOrderedByClassFirstAndThenProfilePos();
        for (int c = 0; c < orig_items.length; c++) {
            // first the dataset
            final int col2use = orig_items[c].getProfilePosition(); // @note extracting using the profile position
            //log.debug("col2use: " + col2use);
            matrix.setColumn(c, fullDs.getColumn(col2use));
            colNames.add(fullDs.getColumnName(col2use));
            // then the new template
            newItems[c] = TemplateImpl.ItemImpl.createItem(orig_items[c].getId(), c); // @note profile pos is changed
        }

        final String name = NamingConventions.generateName(fullDs, origT, true);
        //log.debug("$$$ gen name: " + name + " " + maybeAuxTemplate.getName());
        DefaultDataset newds = new DefaultDataset(name, matrix, fullDs.getRowNames(), colNames, true, false, true, fullDs.getAnnot());
        newds.setProperty("extracted", "true"); // huh

        //klog.debug(">>>>>>>>>>>>>> origT: " + origT.getName() + " newds: " + newds.getName());

        final Template newt = createTemplate(origT.getName() + "_repos", newItems, origT.isContinuous());

        //klog.debug("orig class a: " + origT.getClassName(0) + " made: " + newt.getClassName(0));

        return new DatasetTemplate(newds, newt);
    }

    public static synchronized Template createTemplate(final String name,
                                                       final Item[] items,
                                                       final Template.Class[] classes,
                                                       final boolean continuous) {

        return createTemplate_ordered_assign(name, items, classes, continuous);
    }

    private static int _getColumnIndex(final List colNames, final String colName) {
        for (int c = 0; c < colNames.size(); c++) {
            final String cn = colNames.get(c).toString();
            if (colName.equalsIgnoreCase(cn)) {
                return c;
            }
        }

        return -1;
    }

    public static synchronized Template createCategoricalTemplate(final String name,
                                                                  final Dataset ds,
                                                                  final String classAName,
                                                                  final String[] classASampleNames,
                                                                  final String classBName,
                                                                  final String[] classBSampleNames) {
        return createCategoricalTemplate(name, ds.getColumnNames(), classAName, classASampleNames, classBName, classBSampleNames);
    }

    public static synchronized Template createCategoricalTemplate(final String name,
                                                                  final List all_col_names_ordered,
                                                                  final String classAName,
                                                                  final String[] classASampleNames,
                                                                  final String classBName,
                                                                  final String[] classBSampleNames) {

        if (classASampleNames.length == 0) {
            throw new IllegalArgumentException("Empty classASampleNames");
        }

        if (classBSampleNames.length == 0) {
            throw new IllegalArgumentException("Empty classBSampleNames");
        }


        final GeneSet a = new GeneSet(classAName, classASampleNames);
        final GeneSet b = new GeneSet(classBName, classBSampleNames);

        return createCategoricalTemplate(name, all_col_names_ordered, new GeneSet[]{a, b});
    }

    public static synchronized Template createCategoricalTemplate(final String name,
                                                                  final List all_col_names_ordered,
                                                                  final GeneSet[] gsets_sample_names) {
        TemplateImpl template = new TemplateImpl(name);

        for (int i = 0; i < gsets_sample_names.length; i++) {
            ClassImpl cl = new ClassImpl(gsets_sample_names[i].getName(true));
            template.add(cl);
            for (int r = 0; r < gsets_sample_names[i].getNumMembers(); r++) {
                // TODO: Is this uppercase hack still needed?
                String sampleName = gsets_sample_names[i].getMember(r).toUpperCase(); // @ugggh hack for things like dmap
                int pos = _getColumnIndex(all_col_names_ordered, sampleName);
                if (pos == -1) {
                    throw new IllegalArgumentException("No such sample in ds: " + sampleName + " \n" + Printf.outs(all_col_names_ordered));
                }
                Template.Item item = TemplateImpl.ItemImpl.createItem(gsets_sample_names[i].getName(true), pos);
                template.add(item);
                cl.add(item);
            }
        }

        template.setContinuous(false);
        template.setAux(false);
        template.makeImmutable();
        template.setSampleNamesFromDataset(true); // @note
        return template;
    }

    /**
     * @param name
     * @param items
     * @param classes
     * @param continuous
     * @return
     */
    // THIS IS THE CORE ASSIGNMENT METHOD
    private static synchronized Template createTemplate_ordered_assign(final String name,
                                                                       final Item[] items,
                                                                       final Template.Class[] classes,
                                                                       final boolean continuous) {

        //klog.debug(">>>>> name: " + name);

        if (items == null) {
            throw new IllegalArgumentException("Null items not allowed parameter");
        }

        if (classes == null) {
            throw new IllegalArgumentException("Null classes not allowed parameter");
        }

        TemplateImpl template = new TemplateImpl(name);

        for (int i = 0; i < items.length; i++) {
            template.add(items[i]);
        }

        // @note class order is determined by the spec here
        for (int i = 0; i < classes.length; i++) {
            template.add(classes[i]);
        }

        template.assignItems2ClassInOrder();
        template.setContinuous(continuous);
        template.makeImmutable(); // @note

        return template;
    }

    public static synchronized Template createTemplate(final String name,
                                                       final Item[] items,
                                                       final boolean continuous) {

        if (items == null) {
            throw new IllegalArgumentException("Null items not allowed parameter");
        }

        final List classNames = new ArrayList();

        // IMP need to sort the items by profile pos
        final Item[] items_sorted = getProfilePositionsSorted(items);
        for (int i = 0; i < items_sorted.length; i++) {
            String className = items_sorted[i].getId(); // @note that the original class names are lost here -> we use the class names according to their items name
            if (!classNames.contains(className)) {
                classNames.add(className);
            }
        }

        final Class[] classes = new Class[classNames.size()];
        for (int i = 0; i < classNames.size(); i++) {
            classes[i] = new TemplateImpl.ClassImpl(classNames.get(i).toString());
        }

        return createTemplate_ordered_assign(name, items, classes, continuous); // @note which items are sent doesnt matter (i think)
    }

    /**
     * Make a Template from specified vector (rep a genes expression profile for instance)
     * Template is continuous.
     */
    public static synchronized Template createContinuousTemplate(final String probeName,
                                                                 final Vector profile) {

        // what if 2 elements have the same value??
        // so num of classes may not be eq to num of items
        // is that ok in the context of numeric templates??
        // Yes - it is correct to make 1 item per class
        TemplateImpl template = new TemplateImpl(probeName);

        for (int i = 0; i < profile.getSize(); i++) {
            TemplateImpl.ClassImpl sept = new TemplateImpl.ClassImpl("class." + i);

            template.add(sept);
            TemplateImpl.ItemImpl item = TemplateImpl.ItemImpl.createItem(Float.toString(profile.getElement(i)), i);

            template.add(item);
            sept.add(item);
        }

        template.setContinuous(true);

        template.makeImmutable();
        return template;
    }

    /**
     * Create a template based on the expression pattern of probe_name in the
     * specified Dataset
     *
     * @param probe_name
     * @param ds
     * @return
     */
    public static synchronized Template createContinuousTemplate(final String probe_name,
                                                                 final Dataset ds) {
        int rindex = ds.getRowIndex(probe_name);
        if (rindex == -1) {
            throw new IllegalArgumentException("No data in Dataset: " + ds.getName() + " for probe: " + probe_name);
        }

        Vector vector = ds.getRow(rindex);
        return createContinuousTemplate(probe_name, vector);
    }

    /**
     * @param origT
     * @param includeOrigT
     * @return
     * @throws Exception
     */
    public static Template[] extractAllPossibleTemplates(final Template origT,
                                                         final boolean includeOrigT) throws Exception {

        if (origT == null) {
            throw new IllegalArgumentException("Parameter origT cannot be null");
        }

        List options = new ArrayList();
        if (includeOrigT) {
            options.add(origT);
        }

        if (origT instanceof PreTemplate) {
            // do nothing
        } else if (origT instanceof TemplateImplFromSampleNames) {
            // because of the UI, always 2 class templates only
            Template straight = extractForwardBiphasicTemplate(origT);
            options.add(straight);
            Template rev = TemplateFactory.createReversedBiphasicTemplate(origT);
            options.add(rev);
            //System.out.println(">> " + options);
        } else if (!origT.isContinuous()) {

            // bludgeon our way through -- Template making is inexpensive
            if (origT.getNumClasses() > 2) {
                Template[] all = TemplateFactory.extractAll2ClassTemplates(origT, false); // all so both forward and rev
                for (int i = 0; i < all.length; i++) {
                    options.add(all[i]);
                }
            }

            if (origT.getNumClasses() == 2) {
                Template straight = extractForwardBiphasicTemplate(origT);
                options.add(straight);
                Template rev = TemplateFactory.createReversedBiphasicTemplate(origT);
                options.add(rev);
            }

            // always add 1 for each class, if not an aux
            if (!origT.isAux() && origT.getNumClasses() > 1) {
                Template[] unis = extractAllUniphasicTemplates(origT);
                for (int i = 0; i < unis.length; i++) {
                    options.add(unis[i]);
                }
            }
        }

        return (Template[]) options.toArray(new Template[options.size()]);
    }

    /**
     * INcluding the specified Template and its reverse if the specified template is itself 2-class
     *
     * @param parentMultiClassTemplate
     * @param doOva
     * @param doAllPairs
     * @return
     */
    public static Template[] extractAll2ClassTemplates(final Template parentMultiClassTemplate,
                                                       final boolean onlyForwardOvas) {
        List templates = new ArrayList();

        if (parentMultiClassTemplate.getNumClasses() == 2) {
            templates.add(parentMultiClassTemplate);
            templates.add(TemplateFactory.createReversedBiphasicTemplate(parentMultiClassTemplate));
            return (Template[]) templates.toArray(new Template[templates.size()]);
        }

        Template[] ovas = TemplateFactory.extractAllOvaTemplates(parentMultiClassTemplate, onlyForwardOvas);
        for (int i = 0; i < ovas.length; i++) {
            templates.add(ovas[i]);
        }


        Template[] allpairs = TemplateFactory.extractAllPairsTemplates(parentMultiClassTemplate);
        for (int i = 0; i < allpairs.length; i++) {
            templates.add(allpairs[i]);
        }

        //klog.debug("Generated # aux: " + templates.size() + " from orig: " + parentMultiClassTemplate.getName() + " # classes: " + parentMultiClassTemplate.getNumClasses());
        return (Template[]) templates.toArray(new Template[templates.size()]);
    }

    /**
     * @param parentMultiClassTemplate
     * @return
     */
    public static synchronized Template[] extractAllPairsTemplates(final Template parentMultiClassTemplate) {

        if (parentMultiClassTemplate.getNumClasses() < 2) {
            throw new IllegalArgumentException("At least 3 Template classes needed to do ALL_PAIRS");
        }

        if (parentMultiClassTemplate.isContinuous()) {
            throw new IllegalArgumentException("Cannot work on continuous Template: " + parentMultiClassTemplate.getName());
        }

        // lets do all pairs including reverse ones
        List templatePairs = new ArrayList();
        //Map idTemplateMap = new HashMap();
        for (int i = 0; i < parentMultiClassTemplate.getNumClasses(); i++) {
            Template.Class a = parentMultiClassTemplate.getClass(i);
            for (int j = 0; j < parentMultiClassTemplate.getNumClasses(); j++) {
                if (i == j) {
                    continue;
                }
                Template.Class b = parentMultiClassTemplate.getClass(j);
                //klog.debug("Making aux pair: " + a)
                templatePairs.add(new Template.Class[]{a, b});
            }
        }

        List templates = new ArrayList(templatePairs.size());

        for (int t = 0; t < templatePairs.size(); t++) {
            final Template.Class[] pair = (Template.Class[]) templatePairs.get(t);
            // @maint naming convention
            final String newtName = parentMultiClassTemplate.getName() + "#" + pair[0].getName() + VS + pair[1].getName();
            final TemplateImpl newT = new TemplateImpl(newtName);
            final TemplateImpl.ClassImpl newClassA = new TemplateImpl.ClassImpl(pair[0].getName());
            final TemplateImpl.ClassImpl newClassB = new TemplateImpl.ClassImpl(pair[1].getName());
            newT.add(newClassA);
            newT.add(newClassB);

            for (int i = 0; i < pair[0].getSize(); i++) {
                Item orig = pair[0].getItem(i);
                Item newItem = orig.cloneDeep();// no change to profile pos
                newClassA.add(newItem);
                newT.add(newItem);
            }

            for (int i = 0; i < pair[1].getSize(); i++) {
                Item orig = pair[1].getItem(i);
                Item newItem = orig.cloneDeep();// no change to profile pos
                newClassB.add(newItem);
                newT.add(newItem);
            }

            newT.setContinuous(false);

            newT.makeImmutable();
            templates.add(newT);
        }

        return (Template[]) templates.toArray(new Template[templates.size()]);
    }

    /**
     * @param multiClassTemplate
     * @param onlyForward
     * @return
     */
    public static synchronized Template[] extractAllOvaTemplates(final Template multiClassTemplate,
                                                                 final boolean onlyForward) {
        if (multiClassTemplate.getNumClasses() <= 2) {
            throw new IllegalArgumentException("At least 3 Template classes needed to make a set of ova templates but found: " + multiClassTemplate.getNumClasses() + "\n" + multiClassTemplate.getAsString(false));
        }

        List ovas = new ArrayList();

        for (int c = 0; c < multiClassTemplate.getNumClasses(); c++) {
            Template.Class one = multiClassTemplate.getClass(c);
            Template ova = extractOvaTemplate(multiClassTemplate, one);
            ovas.add(ova);
            if (!onlyForward) {
                String rname = multiClassTemplate.getName() + "#REST" + VS + one.getName();
                Template rova = _createReversedBiphasicTemplate(rname, ova);
                ovas.add(rova);
            }
        }

        // if (!sortByFreq)
        //klog.debug("From master template with # classes: " + multiClassTemplate.getNumClasses() + " made ovas #: " + ovas.size());
        return (Template[]) ovas.toArray(new Template[ovas.size()]);
    }

    /**
     * @param parentMultiClassTemplate
     * @param theOneClass
     * @return
     */
    public static synchronized Template extractOvaTemplate(final Template parentMultiClassTemplate,
                                                           final Template.Class theOneClass) {

        if (!parentMultiClassTemplate.isMemberClass(theOneClass)) {
            throw new IllegalArgumentException("Specified theOneClass: " + theOneClass.getName() + " is not a member of parentMultiClassTemplate: " + parentMultiClassTemplate.getName());
        }

        if (parentMultiClassTemplate.getNumClasses() <= 2) {
            throw new IllegalArgumentException("Need 3 or more classes to do a ova. Found: " + parentMultiClassTemplate.getNumClasses());
        }

        if (parentMultiClassTemplate.isContinuous()) {
            throw new IllegalArgumentException("Cannot work on continuous Template: " + parentMultiClassTemplate.getName());
        }

        // create Template
        //klog.debug("The one name: " + theOneClass.getName() + " restname: " + Constants.REST);
        // @maint naming convention
        String newtName = parentMultiClassTemplate.getName() + "#" + theOneClass.getName() + VS + Constants.REST;
        TemplateImpl newT = new TemplateImpl(newtName);
        TemplateImpl.ClassImpl clonedTheOneClass = new TemplateImpl.ClassImpl(theOneClass.getName());
        newT.add(clonedTheOneClass);

        final int index = parentMultiClassTemplate.getClassIndex(theOneClass);
        final TemplateImpl.ClassImpl restClass = new TemplateImpl.ClassImpl(Constants.REST);
        newT.add(restClass);

        // always put theOneClass first in the new template made
        for (int c = 0; c < theOneClass.getSize(); c++) {
            Item item = TemplateImpl.ItemImpl.createItem(theOneClass.getName(),
                    theOneClass.getItem(c).getProfilePosition()); // note position IS maintained
            newT.add(item);
            clonedTheOneClass.add(item);
        }

        for (int i = 0; i < parentMultiClassTemplate.getNumClasses(); i++) {
            Template.Class cl = parentMultiClassTemplate.getClass(i);
            if (i == index) {
                // already done
            } else {
                for (int c = 0; c < cl.getSize(); c++) {
                    Item item = TemplateImpl.ItemImpl.createItem(Constants.REST, cl.getItem(c).getProfilePosition()); // note position
                    //allItems.add(item);
                    newT.add(item);
                    restClass.add(item);
                }
            }
        }

        newT.setContinuous(false);
        newT.makeImmutable();
        return newT;
    }

    /**
     * @param origTemplate
     * @return
     */
    public static synchronized Template extractForwardBiphasicTemplate(final Template origTemplate) {
        if (origTemplate.getNumClasses() != 2) {
            throw new IllegalArgumentException("Cannot straight template as its not biphasic. # classes: " + origTemplate.getNumClasses() + " " + origTemplate.getName());
        }

        final String newName = origTemplate.getName() + "#" + origTemplate.getClassName(0) + VS + origTemplate.getClassName(1);
        final Template newTemplate = origTemplate.cloneDeep(newName);
        newTemplate.makeImmutable();
        return newTemplate;
    }

    public static synchronized Template createReversedBiphasicTemplate(final Template origTemplate) {
        return _createReversedBiphasicTemplate(origTemplate.getName() + "#" + origTemplate.getClassName(1)
                + VS + origTemplate.getClassName(0), origTemplate);
    }

    // @note IMP the profile positions are NOT changed
    // a a a b b
    // becomes:
    // b b a a a but b & b still have pp of  3 & 4
    // so ONLY changing the class order and items are NOT touched
    private static synchronized Template _createReversedBiphasicTemplate(final String name,
                                                                         final Template origTemplate) {

        if (origTemplate.getNumClasses() != 2) {
            throw new IllegalArgumentException("Cannot reverse template as its not biphasic. # classes: " + origTemplate.getNumClasses() + " " + origTemplate.getName());
        }

        if (origTemplate instanceof TemplateImplFromSampleNames) { // @note
            //TraceUtils.showTrace();
            return ((TemplateImplFromSampleNames) origTemplate).cloneDeepReversed(name);
        }

        final TemplateImpl newT = new TemplateImpl(name);

        // This is the magix (not the items)
        final TemplateImpl.ClassImpl newClassA = new TemplateImpl.ClassImpl(origTemplate.getClass(1).getName());
        for (int i = 0; i < origTemplate.getClass(1).getSize(); i++) {
            Item newItem = origTemplate.getClass(1).getItem(i);
            newClassA.add(newItem.cloneDeep()); // no change to profile pos
            newT.add(newItem);
        }

        final TemplateImpl.ClassImpl newClassB = new TemplateImpl.ClassImpl(origTemplate.getClassName(0));
        for (int i = 0; i < origTemplate.getClass(0).getSize(); i++) {
            Item newItem = origTemplate.getClass(0).getItem(i);
            newClassB.add(newItem.cloneDeep()); // no change to profile pos
            newT.add(newItem);
        }

        newT.add(newClassA);
        newT.add(newClassB);

        newT.setContinuous(false);

        newT.makeImmutable();
        return newT;
    }

    /**
     * for biphasic templates
     */
    public static synchronized Template extractUniphasicTemplate(final Template fullTemplate,
                                                                 final Template.Class cl) {
        if (fullTemplate.isAux()) {
            throw new IllegalArgumentException("Cannot make uniphasic from aux templates: " + fullTemplate.getName());
        }

        TemplateImpl template = new TemplateImpl(fullTemplate.getName() + "#" + cl.getName());
        TemplateImpl.ClassImpl onlyClass = new TemplateImpl.ClassImpl(cl.getName());
        template.add(onlyClass);

        for (int i = 0; i < cl.getSize(); i++) {
            Item origItem = cl.getItem(i);
            Item item = origItem.cloneDeep();
            template.add(item);
            onlyClass.add(item);
        }

        template.setContinuous(false);
        template.setAux(true);
        template.makeImmutable();
        return template;
    }

    public static synchronized Template[] extractAllUniphasicTemplates(final Template fullTemplate) {

        Template[] unis = new Template[fullTemplate.getNumClasses()];
        for (int i = 0; i < fullTemplate.getNumClasses(); i++) {
            unis[i] = extractUniphasicTemplate(fullTemplate, fullTemplate.getClass(i));
        }

        return unis;
    }


}    // End TemplateFactory

/*--- Formatted in Sun Java Convention Style on Fri, Sep 27, '02 ---*/