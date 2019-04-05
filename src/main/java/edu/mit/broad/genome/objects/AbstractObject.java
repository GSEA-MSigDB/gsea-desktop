/*
 * Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.objects;

import org.apache.log4j.Logger;

import java.util.Properties;

/**
 * Basic class for data structures.
 * <p/>
 * Object is auto assigned a unique programmatic name on constructiorn.
 * The alg used for this is edu.mit.broad.genome.util.UUID
 * and the object name is guaranteed to be unique in time and space.
 * <p/>
 * Both Id and name are immuatble (but only the Id is guaranteed to be unique)
 * <p/>
 * IMPORTANT see item 54 in bloch for the initialization stuff.
 *
 * @author Aravind Subramanian
 * @version 1.0
 */
// NOTE: the above javadoc is *incorrect*.  There is no use of UUIDs.
// Leaving it in place as a historical marker in case we ever get around
// to proving IDs are unnecessary.
public abstract class AbstractObject implements PersistentObject {

    /**
     * Name of this object
     */
    private String fName;

    private String fNameEnglish;

    /**
     * Programmatic id of this object - invariant
     */
    private Id fId;

    private StringBuffer fComment;

    /**
     * For logging support
     */
    protected transient Logger log;
    private boolean fInited;

    /**
     * Class Constructor.
     * Makes a new object with specified name.
     */
    protected AbstractObject(final String name) {
        initialize(Id.createId(), name);
    }

    /**
     * Class Constructor.
     * This constructor is provided for use with the load() method.
     * Subclasses that implement load must call initialize() after loading
     * the object. See note above about serialization.
     */
    protected AbstractObject() {
        this.log = Logger.getLogger(this.getClass());
    }

    /**
     * @param id
     * @param name
     */
    protected void initialize(final Id id, final String name) {
        this.initialize(id, name, null);  // @note default
    }

    /**
     * Initializes state of this abstract class.
     */
    protected void initialize(final Id id, final String name, final String nameEnglish) {

        if (fInited) {
            throw new IllegalStateException("Already initialized. disp name " + name);
        }

        if (id == null) {
            throw new NullPointerException("Parameter id cannot be null");
        }

        if (name == null) {
            throw new NullPointerException("Parameter name cannot be null");
        }

        this.log = Logger.getLogger(this.getClass());
        this.fId = id;
        this.fName = removeExtension(name);
        if (nameEnglish != null) {
            this.fNameEnglish = removeExtension(nameEnglish);
        }

        this.fInited = true;
    }

    protected void initialize(final String name) {
        initialize(Id.createId(), name);
    }

    protected void initialize(final String name, final String nameEnglish) {
        initialize(Id.createId(), name, nameEnglish);
    }

    protected boolean isInited() {
        return fInited;
    }

    /**
     * any file extensions are removed from the object name
     *
     * @param name
     * @return
     */
    private String removeExtension(final String name) {
        return name; // @todo
    }

    /**
     * @return Programmatic name of this object
     */
    public Id getId() {

        checkInit();

        return fId;
    }

    /**
     * Must be called by all public instance methods of this abstract class.
     *
     * @throws IllegalStateException if the class has not been initialized properly
     */
    private void checkInit() {

        if (!fInited) {
            throw new IllegalStateException("Uninitialized: " + fName);
        }
    }

    /**
     * @return Display name of this object
     */
    public String getName() {

        checkInit();

        return fName;
    }

    public String getNameEnglish() {

        checkInit();

        return fNameEnglish;
    }

    public String getComment() {
        if (fComment == null)
            return "";
        else
            return fComment.toString();
    }

    public void addComment(String comment) {
        if (fComment == null) fComment = new StringBuffer(comment);
    }

    /**
     * IMP -> not a public method.
     * Implemntors need to decide if its safe or not to allow this.
     * Preferab;ly impl as part of a cloneShallow strategy
     *
     * @see GeneSet
     */
    protected void setName(String newname) {
        if (newname == null) {
            log.warn("Ignoring rename request as newname is null");
        } else {
            this.fName = newname;
        }
    }

    private Properties prp; // lazilly inited

    public void setProperty(String key, String value) {
        if (prp == null) {
            prp = new Properties();
        }
        prp.put(key, value);
    }
}