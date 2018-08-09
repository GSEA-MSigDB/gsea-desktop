/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.objects;

import edu.mit.broad.genome.Errors;
import edu.mit.broad.genome.NamingConventions;
import edu.mit.broad.genome.math.Vector;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author Aravind Subramanian
 */
public class TemplateImplFromSampleNames extends AbstractObject implements Template {

    private Template fCreatedTemplate;

    private String fClassAName;
    private String[] fClassASampleNames;
    private String fClassBName;
    private String[] fClassBSampleNames;

    /**
     * Class constructor
     *
     * @param classAName
     * @param classASampleNames
     * @param classBName
     * @param classBSampleNames
     */
    public TemplateImplFromSampleNames(final String name,
                                       final String classAName, final String[] classASampleNames,
                                       final String classBName, final String[] classBSampleNames) {

        if (classAName == null || classAName.length() == 0) {
            throw new IllegalArgumentException("Param classAName cannot be null or zero length");
        }
        if (classBName == null || classBName.length() == 0) {
            throw new IllegalArgumentException("Param classBName cannot be null or zero length");
        }

        super.initialize(name);

        // sanity checks
        if (classAName.equalsIgnoreCase(classBName)) {
            throw new IllegalArgumentException("Class names cannot be the same classAName: " + classAName + " classBName: " + classBName);
        }

        java.util.List ca = Arrays.asList(classASampleNames);
        Errors errors = new Errors();
        for (int i = 0; i < classBSampleNames.length; i++) {
            if (ca.contains(classBSampleNames[i])) {
                errors.add("This sample is in both classes: " + classBSampleNames[i]);
            }
        }

        errors.barfIfNotEmptyRuntime();

        this.fClassAName = classAName;
        this.fClassASampleNames = _toUniques(classASampleNames);

        this.fClassBName = classBName;
        this.fClassBSampleNames = _toUniques(classBSampleNames);
    }

    public Template createTemplate(final Dataset ds) {
        this.fCreatedTemplate = TemplateFactory.createCategoricalTemplate(NamingConventions.removeExtension(getName()) + "_" + ds.getName(), ds,
                fClassAName, fClassASampleNames, fClassBName, fClassBSampleNames);
        return fCreatedTemplate;
    }

    public Template cloneDeep(final String newName) {
        if (_got_ct()) {
            return _ct().cloneDeep(newName);
        } else {
            return new TemplateImplFromSampleNames(newName, fClassAName, fClassASampleNames, fClassBName, fClassBSampleNames);
        }
    }

    public Template cloneDeepReversed(final String newName) {
        return new TemplateImplFromSampleNames(newName, fClassBName, fClassBSampleNames, fClassAName, fClassASampleNames);
    }

    public void makeImmutable() {
        if (_got_ct()) {
            _ct().makeImmutable();
        } else {
            // do nothing
        }
    }

    public String getQuickInfo() {
        if (_got_ct()) {
            return fCreatedTemplate.getQuickInfo();
        } else {
            return fClassAName + " (" + fClassASampleNames.length + ") " + fClassBName + " (" + fClassBSampleNames.length + ")";
        }
    }

    public boolean isAux() {
        return false;
    }

    public boolean isContinuous() {
        return false;
    }

    public boolean isCategorical() {
        return true;
    }

    public String getClassName(final int classIndex) {
        if (classIndex == 0) {
            return fClassAName;
        } else if (classIndex == 1) {
            return fClassBName;
        } else {
            throw new IllegalArgumentException("Too large: " + classIndex);
        }
    }

    public int getClassIndex(final String name) {
        if (name.equals(fClassAName)) {
            return 0;
        } else {
            return 1;
        }
    }

    public int getClassIndex(final Template.Class cl) {
        return this.getClassIndex(cl.getName());
    }

    public int getNumClasses() {
        return 2;
    }

    public boolean isMemberClass(final Template.Class cl) {
        return cl.getName().equals(fClassAName) || cl.getName().equals(fClassBName);
    }

    private Template _ct() {
        if (fCreatedTemplate == null) {
            throw new IllegalArgumentException("Dataset not set!!");
        }
        return fCreatedTemplate;
    }

    private boolean _got_ct() {
        return fCreatedTemplate != null;
    }

    public Color getItemColor(int itemProfilePos) {
        return _ct().getItemColor(itemProfilePos);
    }

    public boolean isAscendingProfilePositions() {
        return _ct().isAscendingProfilePositions();
    }

    public Class getClass(final Item item) {
        return _ct().getClass(item);
    }

    public Template.Class getClass(final int classIndex) {
        return _ct().getClass(classIndex);
    }

    public String getClassOfInterestName() {
        return _ct().getClassOfInterestName();
    }

    public int getClassOfInterestIndex() {
        return _ct().getClassOfInterestIndex();
    }

    public int[] getProfilePositionsOrdered() {
        return _ct().getProfilePositionsOrdered();
    }

    public Template.Item getItemByProfilePos(int profilePos) {
        return _ct().getItemByProfilePos(profilePos);
    }

    public Template.Item[] getItemsOrderedByProfilePos() {
        return _ct().getItemsOrderedByProfilePos();
    }

    public Template.Item[] getItemsOrderedByClassFirstAndThenProfilePos() {
        return _ct().getItemsOrderedByClassFirstAndThenProfilePos();
    }

    public int getNumItems() {
        return _ct().getNumItems();
    }

    public Vector toVector() {
        return _ct().toVector();
    }

    /**
     * Munge this Template object into a String. Just to keep things familiar
     * use the cls format.
     */
    public String getAsString(final boolean gcFormat) {
        return _ct().getAsString(gcFormat);
    }

    public Vector[] splitByTemplateClass(final Vector profile) {
        return _ct().splitByTemplateClass(profile);
    }

    public Vector synchProfile(final Vector profile) {
        return _ct().synchProfile(profile);
    }

    private static String[] _toUniques(final String[] ss) {

        java.util.List list = new ArrayList();
        for (int i = 0; i < ss.length; i++) {
            String s = ss[i].trim();
            if (s.length() != 0 && list.contains(s) == false) {
                list.add(s);
            }
        }

        return (String[]) list.toArray(new String[list.size()]);
    }

} // End interface TemplateImplFromSampleNames
