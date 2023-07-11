/*
 * Copyright (c) 2003-2023 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package org.genepattern.annotation;

import java.awt.*;
import java.util.*;
import java.util.List;

public class SparseClassVector {
    Map row2AssignmentMap = new HashMap();

    Map classNumber2ColorMap = new HashMap();

    Map classNumber2NameMap = new HashMap();

    Map<List, String> group2GroupName = new HashMap<>();

    List<SparseClassVectorListener> listeners = new ArrayList<>();

    List<List> groups = new ArrayList<>();

    public void slice(int[] order) {
        Map<Integer, List> temp = new HashMap<>();
        for (Iterator keys = row2AssignmentMap.keySet().iterator(); keys
                .hasNext();) {
            Integer row = (Integer) keys.next();
            List classNumbers = (List) row2AssignmentMap.get(row);
            temp.put(order[row.intValue()], classNumbers);
        }
        row2AssignmentMap = temp;
    }

    public void addListener(SparseClassVectorListener l) {
        listeners.add(l);
    }

    public void notifyListeners() {
        for (int i = 0; i < listeners.size(); i++) {
            SparseClassVectorListener listener = listeners.get(i);
            listener.classChanged();
        }
    }

    public List getMembers(Integer classNumber) {
        List members = new ArrayList();
        for (Iterator keys = row2AssignmentMap.keySet().iterator(); keys
                .hasNext();) {
            Integer row = (Integer) keys.next();
            List classNumbers = (List) row2AssignmentMap.get(row);
            if (classNumbers.contains(classNumber)) {
                members.add(row);
            }
        }
        return members;
    }

    public void setClass(Integer classNumber, String className, Color c) {
        classNumber2NameMap.put(classNumber, className);
        classNumber2ColorMap.put(classNumber, c);
        notifyListeners();
    }

    public Iterator getClassNumbers() {
        return classNumber2NameMap.keySet().iterator();
    }

    public List getClassNumbers(int index) {
        return (List) row2AssignmentMap.get(index);
    }

    public Color getColor(Integer i) {
        return (Color) classNumber2ColorMap.get(i);
    }

    public String getClassName(Integer i) {
        return (String) classNumber2NameMap.get(i);
    }

    public List<List> getClassGroups() {
        return groups;
    }

    public String getClassGroupName(List group) {
        return group2GroupName.get(group);
    }

    /**
     * adds a list of grouped class numbers
     *
     * @param group
     * @param groupName
     */
    public void addClassGroup(List group, String groupName) {
        List immutableGroup = Collections.unmodifiableList(group);
        groups.add(immutableGroup);
        group2GroupName.put(immutableGroup, groupName);
    }

    public void removeClassGroup(List group) {
        groups.remove(group);
        group2GroupName.remove(group);
    }

    public void addClass(int index, Integer classNumber) {
        List assignments = (List) row2AssignmentMap.get(index);
        if (assignments == null) {
            assignments = new ArrayList();
            row2AssignmentMap.put(index, assignments);
        }
        assignments.add(classNumber);
    }

    public void removeClass(int index, Integer classNumber) {
        List assignments = (List) row2AssignmentMap.get(index);
        assignments.remove(classNumber);
    }

    public void removeClass(Integer classNumber) {
        for (Iterator keys = row2AssignmentMap.keySet().iterator(); keys
                .hasNext();) {
            Integer row = (Integer) keys.next();
            removeClass(row.intValue(), classNumber);
        }
        classNumber2ColorMap.remove(classNumber);
        classNumber2NameMap.remove(classNumber);
        notifyListeners();
    }
}
