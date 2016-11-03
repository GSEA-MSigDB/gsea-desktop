/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package org.genepattern.data.matrix;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A one-dimensional matrix used to hold class assignment information.
 *
 * @author Joshua Gould
 */
public class ClassVector {
    Map classNumber2IndicesMap;

    int[] assignments;

    Map classNumber2LabelMap;

    int classCount;

    /**
     * Constructs a new class vector from the array of class assignments
     *
     * @param x       the class assignments
     * @param classes ordered array of class names. The 0th entry will be given the
     *                assignment 0, the 1st entry the assignment 1, etc.
     */
    public ClassVector(String[] x, String[] classes) {
        this.assignments = new int[x.length];
        this.classNumber2IndicesMap = new HashMap();
        this.classNumber2LabelMap = new HashMap();
        int maxClassNumber = classes.length;
        Map className2ClassNumberMap = new HashMap();
        for (int i = 0; i < classes.length; i++) {
            Integer classNumberInteger = new Integer(i);
            className2ClassNumberMap.put(classes[i], classNumberInteger);
            classNumber2IndicesMap.put(classNumberInteger, new ArrayList());
            classNumber2LabelMap.put(classNumberInteger, classes[i]);
        }
        for (int i = 0; i < x.length; i++) {
            Integer classNumberInteger = (Integer) className2ClassNumberMap
                    .get(x[i]);
            if (classNumberInteger == null) {
                classNumberInteger = new Integer(maxClassNumber++);
                className2ClassNumberMap.put(x[i], classNumberInteger);
                classNumber2IndicesMap.put(classNumberInteger, new ArrayList());
                classNumber2LabelMap.put(classNumberInteger, x[i]);
            }
            assignments[i] = classNumberInteger.intValue();
            List indices = (List) this.classNumber2IndicesMap
                    .get(classNumberInteger);
            indices.add(new Integer(i));
        }
        this.classCount = maxClassNumber;
    }

    /**
     * Gets the number of assignments in this class vector
     *
     * @return the number of assignments
     */
    public int size() {
        return assignments.length;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        for (int i = 0, length = assignments.length; i < length; i++) {
            if (i > 0) {
                sb.append(" ");
            }
            sb.append(getClassName(assignments[i]));
        }
        return sb.toString();
    }

    /**
     * Gets the number of different possible values taken by the class
     * assignments. Note that this can be greater than the actual number of
     * classes contained in this class vector.
     *
     * @return The number of classes.
     */
    public int getClassCount() {
        return classCount;
    }

    /**
     * Gets the class name for the specified class number
     *
     * @param classNumber The class number
     * @return The class name.
     */
    public String getClassName(int classNumber) {
        return (String) classNumber2LabelMap.get(new Integer(classNumber));
    }

    /**
     * Gets the class assignment
     *
     * @param index The index
     * @return The assignment
     */
    public int getAssignment(int index) {
        return assignments[index];
    }

}
