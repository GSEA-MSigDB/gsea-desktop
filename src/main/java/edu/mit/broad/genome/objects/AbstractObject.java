/*******************************************************************************
 * Copyright (c) 2003-2016 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 *******************************************************************************/
package edu.mit.broad.genome.objects;

import edu.mit.broad.genome.XLogger;

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
        this.log = XLogger.getLogger(this.getClass());
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

        /* dont
        if (nameEnglish == null) {
            throw new NullPointerException("Parameter nameEnglish cannot be null");
        }
        */

        this.log = XLogger.getLogger(this.getClass());
        this.fId = id;
        this.fName = removeExtension(name);
        if (nameEnglish != null) {
            this.fNameEnglish = removeExtension(nameEnglish);
        }

        //enforceNamingConvention();
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

        /*
        if (fNameEnglish == null) {
            return fName; // @note
        } else {
            return fNameEnglish;
        }
        */
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
     * @see FSet
     */
    protected void setName(String newname) {
        if (newname == null) {
            log.warn("Ignoring rename request as newname is null");
        } else {
            this.fName = newname;
        }
    }

    /**
     * @return
     * @todo see if this causes trouble
     * <p/>
     * not for internal use -- see ObjectBinder
     * <p/>
     * not for internal use -- see ObjectBinder
     * <p/>
     * not for internal use -- see ObjectBinder
     * <p/>
     * not for internal use -- see ObjectBinder
     * <p/>
     * not for internal use -- see ObjectBinder
     * <p/>
     * not for internal use -- see ObjectBinder
     */
    /*
    private void enforceNamingConvention() {

        String newn = enforceNamingConvention(fName, this.getClass());
        if (newn.equals(fName)) {
            ; // we're ok
        } else {
            log.warn("Incorrect name for pob -- missing extension - changing from: " + fName + " to: " + newn);
            this.fName = newn;
        }

    }
    */

    private Properties prp; // lazilly inited

    public void setProperty(String key, String value) {
        if (prp == null) {
            prp = new Properties();
        }
        prp.put(key, value);
    }

}    // End AbstractObject
