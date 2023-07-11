/*
 * Copyright (c) 2003-2023 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package edu.mit.broad.genome.parsers;

import edu.mit.broad.genome.objects.GeneSetMatrix;
import edu.mit.broad.genome.objects.PersistentObject;
import edu.mit.broad.genome.objects.Template;
import edu.mit.broad.genome.reports.api.Report;
import edu.mit.broad.genome.swing.ProxyComboBoxModel;
import edu.mit.broad.genome.swing.ProxyTreeModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.*;

/**
 * @author Aravind Subramanian
 */
public class ObjectCache {
    private final Logger log = LoggerFactory.getLogger(ObjectCache.class);

    /**
     * key-> class names, value -> DefaultMutableTreeNode
     */
    private final Map fClassNameNodeMap = new Hashtable();

    /**
     * key -> class name, value -> PobModel
     */
    private final Map fClassNameBoxModelMap = new Hashtable();
    private final DefaultTreeModel fTreeModel;
    private final DefaultMutableTreeNode fRootNode;

    private Map fIdCachesExtra = new HashMap(); // stores as elements al models (resricted to just the id)

    /**
     * Key -> PathClass, Value ->the object
     */
    private final Map fPathClassObjectMap = new HashMap();

    // key -> template, value -> source file
    // needed as continuous templates are clobberred by the other map
    //private final Map fContinuous_template_file_map_hack = new HashMap();

    /**
     * Value -> the object, Value ->the path string to where the object is from (eg file path)
     */
    private final Map fObjectPathStringMap = new HashMap();

    /**
     * pobs bot visible in the usual cache containers,. but still accessible (as the last check)
     * via get source file
     * Usually used for auxe's
     */
    private final Map fInvisiblePobFileMap = new HashMap();

    /**
     * Utility fields for the event firing mechanism
     */
    private final PropertyChangeSupport fPathChanges = new PropertyChangeSupport(this);

    private final PropertyChangeSupport fReportChanges = new PropertyChangeSupport(this);

    /**
     * Name of the event that is fired when a new path is added to the cache
     */
    public static final String PROP_PATH_ADDED = "path_added";

    /**
     * Name of the event that is fired when a new reportis added to the cache
     * Initially made it generic to all objects but event firings are slow and hence
     * beter to restrict to specific ones
     */
    public static final String PROP_REPORT_ADDED = "pob_added";

    /**
     * Privatized Class Constructor.
     * Its a singleton, so use getInstance(0;
     */
    public ObjectCache() {
        this("<html><body><font type=bold>Objects in memory</font><font color=gray size=-2> " +
                "[shift-click to expand all]</font></body></html>");
    }

    private ObjectCache(final String rootNodeLabel) {
        fRootNode = new DefaultMutableTreeNode(rootNodeLabel);
        fTreeModel = new DefaultTreeModel(fRootNode, true);
    }

    public boolean isCached(final String path, final Class cl) {
        return fPathClassObjectMap.containsKey(new PathClass(path, cl));
    }

    public boolean isCached(final File file, final Class cl) {
        return isCached(file.getPath(), cl);
    }

    public boolean isCached(PersistentObject pob) {

        if (fObjectPathStringMap.containsKey(pob)) {
            return true;
        } else if (fInvisiblePobFileMap.containsKey(pob)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * @param file
     * @return Null if file not cached
     */
    public Object get(final String path, final Class cl) {
        return fPathClassObjectMap.get(new PathClass(path, cl));
    }

    public Object get(final File file, final Class cl) {
        return fPathClassObjectMap.get(new PathClass(file.getPath(), cl));
    }

    /**
     * @param pob
     * @return file that the object was parsed from, null if not known
     */
    public String getSourcePath(final Object pob) {

        if (pob == null) {
            throw new IllegalArgumentException("Parameter obj cannot be null");
        }

        Object fo = fObjectPathStringMap.get(pob);

        if (fo == null && fInvisiblePobFileMap.containsKey(pob)) {
            fo = ((File) fInvisiblePobFileMap.get(pob)).getPath();
        }

        if (fo != null) {
            return fo.toString();
        } else {
            throw new IllegalArgumentException("Unknown cache object -- use isCached() to check first if appropriate: " + pob + " class: " + pob.getClass());
            //return null;
        }

    }

    public File getSourceFile(final Object pob) {
        String path = getSourcePath(pob);
        if (path == null) {
            return null;
        } else {
            return new File(path);
        }
    }

    /**
     * add an object to the cache
     *
     * @param object
     */
    //IMP that this is protected -- we want only ParserFactory to make additions
    public void add(final File file, final PersistentObject pob, final Class cl) {
        add(file.getPath(), pob, cl);
    }

    protected void add(final File file, final PersistentObject pob, final Class cl, final boolean fireAction) {
        add(file.getPath(), pob, cl, fireAction);
    }

    protected void add(String path, PersistentObject pob, Class cl) {
        add(path, pob, cl, true);
    }

    /**
     * @param path
     * @param pob
     * @param cl
     */
    protected void add(String path, PersistentObject pob, Class cl, boolean fireAction) {

        PathClass fc = new PathClass(path, cl);

        if (fObjectPathStringMap.containsKey(pob) && !(pob instanceof Template)) {
            if (log.isDebugEnabled()) { log.debug("Already cached object: {} in: {}", pob, fObjectPathStringMap.get(pob)); }
        }

        fPathClassObjectMap.put(fc, pob);
        fObjectPathStringMap.put(pob, path);

        String cn = cl.getName();

        // first update the tree model
        // Make a class node if not already extant
        if (fClassNameNodeMap.get(cn) == null) {
            DefaultMutableTreeNode mtr = new DefaultMutableTreeNode(cn);

            fClassNameNodeMap.put(cn, mtr);
            fTreeModel.insertNodeInto(mtr, fRootNode, fRootNode.getChildCount());
        }

        // Then always add a ;eaf
        DefaultMutableTreeNode objclassNode = (DefaultMutableTreeNode) fClassNameNodeMap.get(cn);
        DefaultMutableTreeNode objNode = new DefaultMutableTreeNode(pob, false);

        cleanupNode(pob, path, objclassNode);
        objclassNode.add(objNode);

        try { // happens if the object hasnt been registered with the object tree
            if (fireAction) {
                fTreeModel.reload(objclassNode);
            }
        } catch (Throwable t) {
            log.error(t.getMessage(), t);
        }

        // then add to box model
        if (fClassNameBoxModelMap.get(cn) == null) {
            PobBoxModel model = new PobBoxModel();
            fClassNameBoxModelMap.put(cn, model);
        }

        PobBoxModel model = (PobBoxModel) fClassNameBoxModelMap.get(cn);

        if (model.getIndexOf(pob) == -1) {
            if (fireAction) {
                cleanupModel(pob, path, model); // not sure if this should be done all the time?
                // trouble is that the template action fires all the time then
            }

            model.addElement(pob);

            if (fireAction) {
                firePathAdded(new PropertyChangeEvent(this, PROP_PATH_ADDED, null, path));
                if (pob instanceof Report) {
                    fireReportAdded(new PropertyChangeEvent(this, PROP_REPORT_ADDED, null, pob));
                }
            }
        }

        // Finally add to any sub-caches if neccessary
        for (Iterator iterator = fIdCachesExtra.keySet().iterator(); iterator.hasNext();) {
            Object key = iterator.next();
            ExtraCache ex = (ExtraCache) fIdCachesExtra.get(key);
            if (ex.isMember(path)) {
                ex.fExCache.add(path, pob, cl, fireAction);
            }
        }

    }

    // remove existing entities similar to the one we are abut to add
    // similar -> same name AND same file
    private void cleanupNode(PersistentObject newpob, String path, DefaultMutableTreeNode node) {

        List toremove = new ArrayList();
        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode onode = (DefaultMutableTreeNode) node.getChildAt(i);
            PersistentObject pob = (PersistentObject) onode.getUserObject();
            String extant = getSourcePath(pob);
            if ((extant != null) && (extant.equals(path)) && (pob.getName().equals(newpob.getName()))) { // replace the old object with this one
                toremove.add(i);
            }
        }

        for (int i = 0; i < toremove.size(); i++) {
            node.remove(((Integer) toremove.get(i)).intValue());
        }

    }

    private void cleanupModel(PersistentObject newpob, String path, PobBoxModel model) {
        List toremove = new ArrayList();
        for (int i = 0; i < model.getSize(); i++) {
            PersistentObject pob = (PersistentObject) model.getElementAt(i);
            String extant = getSourcePath(pob);
            if ((extant != null) && (extant.equals(path)) && (pob.getName().equals(newpob.getName()))) { // replace the old object with this one
                toremove.add(pob);
            }
        }

        for (int i = 0; i < toremove.size(); i++) {
            model.removeElement(toremove.get(i));
        }
    }

    protected void sortModel(Class cn) {
        Object obj = fClassNameBoxModelMap.get(cn.getName());
        if (obj == null) {
            log.debug("no object model yet for class: {}", cn);
            return;
        }

        PobBoxModel model = (PobBoxModel) obj;
        model.sort();
    }

    // Not sure if this avoids the multi-instance selection probelm (see createBoxModel) below
    public TreeModel createTreeModel() {
        return new ProxyTreeModel(fTreeModel);
    }

    // just returning cbm strangly cause classcastexception -- see objbindery
    // fixed -> problem was junit -- excluded proxy from junit and all was well
    public ComboBoxModel createBoxModel(Class cl) {
        return createBoxModel(cl, false);
    }

    public ComboBoxModel createBoxModel(Class cl, boolean addNotSpecifiedObject) {
        PobBoxModel real = _createBoxModel(cl);
        if (addNotSpecifiedObject) {
            // @todo
            return new ProxyComboBoxModel(real);
        } else {
            return new ProxyComboBoxModel(real);
        }
    }

    public PobBoxModel _createBoxModel(Class cl) {
        Object emodel = fClassNameBoxModelMap.get(cl.getName());
        PobBoxModel real;

        if (emodel != null) {
            real = (PobBoxModel) emodel;
        } else {
            real = new PobBoxModel();
            fClassNameBoxModelMap.put(cl.getName(), real);    // so that if it gets filled after request, we will see it
        }

        return real;
    }

    // @todo kludge for the GeneSetMatrix UI window thing - gte rid of me
    private PobBoxModel auxsetsmodel;

    protected void hackAddAuxSets(GeneSetMatrix gm) {
        if (auxsetsmodel == null) {
            auxsetsmodel = new PobBoxModel();
        }
        for (int i = 0; i < gm.getNumGeneSets(); i++) {
            auxsetsmodel.addElement(gm.getGeneSet(i));
        }
        auxsetsmodel.sort();
    }

    public PobBoxModel hackCreateAuxGeneSetsBoxModel() {
        if (auxsetsmodel == null) {
            auxsetsmodel = new PobBoxModel();
        }

        return auxsetsmodel;
    }

    public ComboBoxModel createBoxModel(Class[] classes) {

        PobBoxModel[] models = new PobBoxModel[classes.length];

        for (int i = 0; i < classes.length; i++) {
            models[i] = _createBoxModel(classes[i]);
        }

        return new PobBoxModels(models);
    }

    // NOT an immutbale list!! --can be sorted etc by caller
    // @todo this mechanism doesnt work with multiple objects of same class and same file path
    public List getCachedObjectsL(Class pobClass) {
        List pobs = new ArrayList();

        Iterator it = fPathClassObjectMap.keySet().iterator();
        while (it.hasNext()) {
            PathClass pc = (PathClass) it.next();
            if (pc.cl.equals(pobClass)) {
                pobs.add(fPathClassObjectMap.get(pc));
            }
        }

        return pobs;
    }

    public void addPathAdditionsListener(PropertyChangeListener p) {
        fPathChanges.addPropertyChangeListener(p);
    }

    private void firePathAdded(PropertyChangeEvent evt) {
        fPathChanges.firePropertyChange(evt);
    }

    public void addReportAdditionsListener(PropertyChangeListener p) {
        fReportChanges.addPropertyChangeListener(p);
    }

    private void fireReportAdded(PropertyChangeEvent evt) {
        fReportChanges.firePropertyChange(evt);
    }

    /**
     * To enable parsing > 1 object (i.e class) from the same path
     *
     * @author Aravind Subramanian
     * @version %I%, %G%
     */
    class PathClass {

        private String path;
        private Class cl;

        private PathClass(String path, Class cl) {
            if (path == null) {
                throw new IllegalArgumentException("Param path cannot be null");
            }

            if (cl == null) {
                throw new IllegalArgumentException("Param cl cannot be null");
            }

            this.path = path;
            this.cl = cl;
        }

        public boolean equals(Object obj) {

            if (obj instanceof PathClass) {
                return equals((PathClass) obj);
            }

            return false;
        }

        public boolean equals(PathClass fc) {

            if (fc.path.equals(this.path)) {
                // more checks to do below
            } else {
                return false;
            }

            if (fc.cl.getName().equals(this.cl.getName())) {
            } else {
                return false;
            }

            return true;
        }

        public int hashCode() {
            return path.hashCode() + cl.getName().hashCode();
        }
    }

    public void makeVisible(PersistentObject pob, Class cl) {
        makeVisible(new PersistentObject[]{pob}, cl);
    }

    public void makeVisible(PersistentObject[] pobs, Class cl) {
        for (int i = 0; i < pobs.length; i++) {
            Object file = fInvisiblePobFileMap.get(pobs[i]);
            if (file != null) {
                fInvisiblePobFileMap.remove(pobs[i]);
                add((File) file, pobs[i], cl, false);
            }
        }

        this.sortModel(cl);
    }

    protected void addInvisibly(File file, PersistentObject pob) {
        if (file == null) {
            throw new IllegalArgumentException("Parameter file cannot be null");
        }

        if (pob == null) {
            throw new IllegalArgumentException("Parameter pob cannot be null");
        }

        if (fInvisiblePobFileMap.containsKey(pob)) { log.warn("Already registered: {} overwriting", pob.getName()); }

        fInvisiblePobFileMap.put(pob, file);
    }

    protected void addInvisibly(String source, PersistentObject pob) {
        if (source == null) {
            throw new IllegalArgumentException("Parameter source cannot be null");
        }

        addInvisibly(new File(source), pob);
    }


    static class ExtraCache {

        private ObjectCache fExCache;

        private String uniqId;

        boolean isMember(final String path) {
            int index = path.indexOf(uniqId); // i,e is path a a sub dir of uniqid
            return index != -1;
        }

        public String toString() {
            return uniqId;
        }

        public int hashCode() {
            return uniqId.hashCode();
        }
    }
}
