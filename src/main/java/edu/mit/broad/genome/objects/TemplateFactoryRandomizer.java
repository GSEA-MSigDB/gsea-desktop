/*
 * Copyright (c) 2003-2022 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.objects;

import edu.mit.broad.genome.math.RandomSeedGenerator;
import edu.mit.broad.genome.math.Vector;
import edu.mit.broad.genome.math.XMath;
import edu.mit.broad.genome.objects.strucs.TemplateRandomizerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Aravind Subramanian
 */
public class TemplateFactoryRandomizer {
    private static final Logger klog = LoggerFactory.getLogger(TemplateFactoryRandomizer.class);

    public static Template[] createRandomTemplates(final int num,
                                                   final Template origTemplate,
                                                   final TemplateRandomizerType rt,
                                                   final RandomSeedGenerator rst) {

        return createRandomTemplates(num, origTemplate, rt, rst, false);
    }

    // @todo delete me later or fixme SEE below for orig
    public static Template[] createRandomTemplates(final int num,
                                                   final Template origTemplate,
                                                   final TemplateRandomizerType rt,
                                                   final RandomSeedGenerator rst,
                                                   final boolean silent) {

        Template[] tss;

        if (!silent) { klog.debug("TemplateRandomizerType: {}", rt.toString()); }

        if (rt == TemplateRandomizerType.NO_BALANCE) {
            tss = createRandomTemplates(num, origTemplate, rst);
        } else if (rt == TemplateRandomizerType.BALANCED_CLASS0) {
            tss = createRandomAuxBalancedTemplates(num, origTemplate, true, rst);
        } else if (rt == TemplateRandomizerType.BALANCED_CLASS1) {
            tss = createRandomAuxBalancedTemplates(num, origTemplate, false, rst);
        } else if (rt == TemplateRandomizerType.EQUALIZE_AND_BALANCE) {
            tss = createRandomAuxEqualizedAndBalancedTemplates(num, origTemplate, rst);
        } else {
            throw new IllegalArgumentException("Unknown RandomizerType: " + rt);
        }

        return tss;
    }

    public static Template[] createRandomTemplates(final int num,
                                                   final Template origTemplate,
                                                   final RandomSeedGenerator rsgen) {

        Template[] templates = new Template[num];

        for (int i = 0; i < num; i++) {
            templates[i] = createRandomTemplate(origTemplate, rsgen);
        }

        return templates;
    }

    /**
     * To randomizes a template:
     *
     * @note IMP to make new items and classes and NOT add orig templates items and classes
     * (i.e clone dont use refs)
     * @note we are just randomizing the indices so the number of items in each
     * class IS preserved.
     */
    public static Template createRandomTemplate(final Template origTemplate,
                                                final RandomSeedGenerator rsgen) {

        if (origTemplate.isContinuous()) { // better to call that as the assign items on cont templates can barf (on dupl items)
            Vector rnd = createRandomTemplateContinuousV(origTemplate, rsgen);
            return TemplateFactory.createContinuousTemplate(origTemplate.getName(), rnd);
        } else {

            // @note for now only allow rnd of 2 class templates
            if (origTemplate.getNumClasses() != 2) {
                throw new IllegalArgumentException("Only 2 class templates rnd allowed: " + origTemplate.getNumClasses());
            }

            /**
             * Need to pick from existing template randomly into a new one
             * WITHOUT repetition
             */
            final int[] inds = XMath.randomizeWithoutReplacement(origTemplate.getNumItems(), rsgen);

            final Template.Item[] items = origTemplate.getItemsOrderedByProfilePos();
            final Template.Item[] newItems = new Template.Item[items.length];
            final TemplateImpl newT = new TemplateImpl(origTemplate.getName());

            for (int i = 0; i < items.length; i++) {
                final Template.Item real_item = items[i];
                final Template.Item real_rnd_item = items[inds[i]];
                //String rndLabel = real_rnd_item.getId();
                final Template.Class cl = origTemplate.getClass(real_rnd_item);
                // cant use rnd label as item label (e.g t) need not be the same as class label (e.g tumor)
                //newItems[i] = TemplateImpl.ItemImpl.createItem(rndLabel, real_item.getProfilePosition());//rnd label (class), same position(data);
                newItems[i] = TemplateImpl.ItemImpl.createItem(cl.getName(), real_item.getProfilePosition());//rnd label (class), same position(data);
                newT.add(newItems[i]);
            }

            final TemplateImpl.ClassImpl newClassA = new TemplateImpl.ClassImpl(origTemplate.getClass(0).getName());
            final TemplateImpl.ClassImpl newClassB = new TemplateImpl.ClassImpl(origTemplate.getClass(1).getName());

            // @note make our custom assignments here
            for (int i = 0; i < newItems.length; i++) {
                if (newItems[i].getId().equals(newClassA.getName())) {
                    newClassA.add(newItems[i]);
                } else if (newItems[i].getId().equals(newClassB.getName())) {
                    newClassB.add(newItems[i]);
                } else {
                    throw new IllegalStateException("Unknown item class: " + newItems[i].getId());
                }
            }

            newT.add(newClassA);
            newT.add(newClassB);
            //newt.assignItems();  // DO NOT call - custom addition already done above

            newT.setClassOfInterestIndex(origTemplate.getClassOfInterestIndex());
            newT.setAux(origTemplate.isAux());
            newT.setContinuous(false);
            newT.makeImmutable();
            return newT;
        }
    }

    // ultra fast compared to the other method (i hope)
    public static Vector createRandomTemplateContinuousV(final Template origContinuousTemplate,
                                                         final RandomSeedGenerator rsgen) {

        if (origContinuousTemplate.isContinuous() == false) {
            throw new IllegalArgumentException("Only valid to call for continuous templates");
        }

        final Vector shufv = new Vector(origContinuousTemplate.getNumItems());
        final int[] inds = XMath.randomizeWithoutReplacement(origContinuousTemplate.getNumItems(), rsgen);
        final int[] profilePos = origContinuousTemplate.getProfilePositionsOrdered();

        for (int i = 0; i < profilePos.length; i++) {
            final int rndProfilePos = profilePos[inds[i]];
            final Template.Item item = origContinuousTemplate.getItemByProfilePos(rndProfilePos);
            shufv.setElement(i, Float.parseFloat(item.getId()));
        }

        return shufv;
    }

    /**
     * balanced -> basically constrained with 50-50 for the specified class
     * <p/>
     * If:
     * class0 -> 14
     * class1 -> 14
     * <p/>
     * Then: rnd class 0 -> always 7 c0 and 7 c1 and ditto for class 1
     * <p/>
     * If:
     * class0 -> 8
     * class1 -> 20
     * <p/>
     * Then balancing wrt class 0 gives:
     * <p/>
     * rnd class0 -> 4 from class 0 and 4 from class 1
     * and hence class 1 -> 4 from class 0 and 16 from class 1
     * <p/>
     * and balancing wrt class 1 gives -> not possible as too few!
     *
     * @param orig
     * @param c0
     * @param num0
     * @param c1
     * @param num1
     * @return
     */
    public static Template createRandomAuxBalancedTemplate(final Template orig,
                                                           final boolean balanceFirstClass,
                                                           final RandomSeedGenerator rsgen) {

        if (orig.getNumClasses() != 2) {
            throw new IllegalArgumentException("Balanced rnd only possible for 2 class templates");
        }

        if (balanceFirstClass) {
            Template.Class one = orig.getClass(0);
            int oneNum = one.getSize() / 2; // @note
            int twoNum = one.getSize() - oneNum; // the rest
            Template.Class two = orig.getClass(1);
            return createRandomConstrainedTemplate(orig, one, oneNum, two, twoNum, rsgen);
        } else {
            Template.Class one = orig.getClass(0);
            Template.Class two = orig.getClass(1);
            int twoNum = two.getSize() / 2; // @note
            int oneNum = one.getSize() - twoNum;
            return createRandomConstrainedTemplate(orig, one, oneNum, two, twoNum, rsgen);
        }

    }

    public static Template[] createRandomAuxBalancedTemplates(final int numrnd,
                                                              final Template orig,
                                                              final boolean balanceFirstClass,
                                                              final RandomSeedGenerator rsgen) {

        if (orig.getNumClasses() != 2) {
            throw new IllegalArgumentException("Only 2 class templates possible");
        }

        final Template[] rndtss = new Template[numrnd];

        for (int i = 0; i < numrnd; i++) {
            rndtss[i] = createRandomAuxBalancedTemplate(orig, balanceFirstClass, rsgen);
        }

        return rndtss;
    }

    public static Template[] createRandomAuxEqualizedAndBalancedTemplates(final int numrnd,
                                                                          final Template orig,
                                                                          final RandomSeedGenerator rsgen) {

        if (orig.getNumClasses() != 2) {
            throw new IllegalArgumentException("Only 2 class templates possible");
        }

        final Template[] rndtss = new Template[numrnd];

        for (int i = 0; i < numrnd; i++) {
            // first equalize (randomly) Must eq within this loop else all rnds will share the same equalization
            //int num2use = XMath.min(new int[]{orig.getClass(0).getSize(), orig.getClass(1).getSize()});
            //Template eqt = createAuxConstrainedRealTemplate(orig, num2use, num2use, rsgen);
            //klog.debug("Equalizing to: " + num2use);
            // then balance the rnd
            // @todo restore me perhaps
            //rndtss[i] = TemplateFactory.createAuxBalancedRndTemplate(eqt, true, rsgen);// doesnt matter which class is balanced

            final Template.Class one = orig.getClass(0);
            final int twoNum = one.getSize() / 2; // @note
            final int oneNum = one.getSize() - twoNum; // the rest (bigger if odd number)
            final Template.Class two = orig.getClass(1);
            rndtss[i] = createRandomConstrainedTemplate(orig, one, oneNum, two, twoNum, rsgen);
        }

        return rndtss;
    }

    /**
     * works for only 2 class templates
     * <p/>
     * creates a new rnd template with num0 of class0 in class0 and rest in class 1
     * num1 of class1 in class0 and rest in class1
     *
     * @param c0
     * @param numFromC0inC0
     * @param c1
     * @param numFromC1inC0
     */
    public static Template createRandomConstrainedTemplate(final Template orig,
                                                           final Template.Class c0,
                                                           final int numFromC0inC0,
                                                           final Template.Class c1,
                                                           final int numFromC1inC0,
                                                           final RandomSeedGenerator rsgen) {

        if (numFromC0inC0 > c0.getSize()) {
            throw new IllegalArgumentException("numFromC0inC0:  " + " cannot be larger than size of templateclass 0: " + c0.getSize());
        }

        if (numFromC1inC0 > c1.getSize()) {
            throw new IllegalArgumentException("numFromC1inC0: " + numFromC1inC0 + " cannot be larger than size of templateclass 1: " + c1.getSize());
        }

        if ((numFromC0inC0 + numFromC1inC0) != c0.getSize()) {
            throw new IllegalArgumentException("numFromC0inC0 + numFromC1inC0: " + (numFromC0inC0 + numFromC1inC0) + " not equal to size of class0: " + c0.getSize());
        }

        // randomize all c0 indices, but keep only numFromC0inC0 of them
        int[] inds0 = XMath.randomizeWithoutReplacement(c0.getSize(), rsgen);

        TemplateImpl newt = new TemplateImpl(orig.getName());
        TemplateImpl.ClassImpl newc0 = new TemplateImpl.ClassImpl(c0.getName());
        TemplateImpl.ClassImpl newc1 = new TemplateImpl.ClassImpl(c1.getName());

        String id0 = c0.getItem(0).getId(); // some item
        String id1 = c1.getItem(0).getId(); // some item

        // place the first numFromC0inC0 of these rnd class0 items in the new C0
        int x0 = 0;
        for (; x0 < numFromC0inC0; x0++) { // constrain to numFromC0inC0 of them only
            Template.Item item = c0.getItem(inds0[x0]);
            Template.Item newitem = TemplateImpl.ItemImpl.createItem(id0, item.getProfilePosition());
            newc0.add(newitem);
            newt.add(newitem);
        }

        // rest go to the other template class
        for (; x0 < inds0.length; x0++) { // constrain to num of them only
            Template.Item item = c0.getItem(inds0[x0]);
            Template.Item newitem = TemplateImpl.ItemImpl.createItem(id1, item.getProfilePosition());
            newc1.add(newitem);
            newt.add(newitem);
        }

        // randomize all c1 indices but keep only numFromC1inC0
        int[] inds1 = XMath.randomizeWithoutReplacement(c1.getSize(), rsgen);

        int x1 = 0;
        for (; x1 < numFromC1inC0; x1++) { // constrain to num1 of them only
            Template.Item item = c1.getItem(inds1[x1]);
            Template.Item newitem = TemplateImpl.ItemImpl.createItem(id0, item.getProfilePosition());
            newc0.add(newitem);
            newt.add(newitem);
        }

        // rest go to the other template class
        for (; x1 < inds1.length; x1++) { // constrain to num of them only
            Template.Item item = c1.getItem(inds1[x1]);
            Template.Item newitem = TemplateImpl.ItemImpl.createItem(id1, item.getProfilePosition());
            newc1.add(newitem);
            newt.add(newitem);
        }

        newt.add(newc0);
        newt.add(newc1);

        //newt.assignItems();  // DO NOT call - custom addition already done above

        newt.setClassOfInterestIndex(orig.getClassOfInterestIndex());

        if (orig.isAux()) {
            newt.setAux(true);
        }

        if (orig.isContinuous()) {
            newt.setContinuous(true);
        }

        newt.makeImmutable();

        return newt;
    }
}
