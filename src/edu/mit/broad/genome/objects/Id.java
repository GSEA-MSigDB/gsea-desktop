/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.objects;

/**
 * A reasonable unique id generator.
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */
//public class Id implements javax.xml.bind.IdentifiableElement { // @note comm out agu 2006 for the web stuff
public class Id {

    private String fId;

    /**
     * Factory method for creating a new Id.
     * Reasonably guaranteed to be unique
     *
     * @see UUID
     */
    public static Id createId() {
        return new Id("todo");
        // @todo
        // @note id'ing disabled as:
        // 1) not used
        // 2) unable to create ID on macs and if run under LSF
        //return new Id(new UUID().toString());
    }

    /**
     * privatized constructor -> use teh factroy metjod to create a new object instead
     * <p/>
     * NO - need it. Maybe change if pob is changed to id.
     */
    public Id(String id) {

        if (id.indexOf("\n") != -1) {
            throw new IllegalArgumentException("Ids must not have \n");
        }

        id = id.trim();
        this.fId = id;
    }

    // Yes! Needed by db40
    // @load check if needed -> exp for db40 for use with load
    public Id() {
    }

    public int hashCode() {
        return fId.hashCode();
    }

    public boolean equals(Object obj) {

        if (obj instanceof Id) {
            return fId.equals(((Id) obj).fId);
        }

        return false;
    }
}    // End Id
