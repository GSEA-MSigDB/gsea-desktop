/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.objects.strucs;

/**
 * Enum construct
 */
public class TemplateRandomizerType {

    private String name;

    public static final TemplateRandomizerType NO_BALANCE = new TemplateRandomizerType("no_balance");
    public static final TemplateRandomizerType BALANCED_CLASS0 = new TemplateRandomizerType("balance_class0");
    public static final TemplateRandomizerType BALANCED_CLASS1 = new TemplateRandomizerType("balance_class1");
    public static final TemplateRandomizerType EQUALIZE_AND_BALANCE = new TemplateRandomizerType("equalize_and_balance");

    /**
     * Privatized class constructor
     *
     * @param name
     */
    private TemplateRandomizerType(String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }

    public static TemplateRandomizerType lookupRandomizerType(Object obj) {
        if (obj == null) {
            throw new IllegalArgumentException("Param rt cannot be null");
        }

        if (obj instanceof TemplateRandomizerType) {
            return (TemplateRandomizerType) obj;
        }

        String rt = obj.toString();

        if (rt.equalsIgnoreCase(NO_BALANCE.name)) {
            return NO_BALANCE;
        } else if (rt.equalsIgnoreCase(BALANCED_CLASS0.name)) {
            return BALANCED_CLASS0;
        } else if (rt.equalsIgnoreCase(BALANCED_CLASS1.name)) {
            return BALANCED_CLASS1;
        } else if (rt.equalsIgnoreCase(EQUALIZE_AND_BALANCE.name)) {
            return EQUALIZE_AND_BALANCE;
        } else {
            throw new IllegalArgumentException("Unknown RandomizationType: " + rt);
        }
    }

} // End class RandomizerType
