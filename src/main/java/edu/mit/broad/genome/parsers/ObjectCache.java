/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.genome.parsers;

import edu.mit.broad.genome.alg.ComparatorFactory;
import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.objects.GeneSetMatrix;
import edu.mit.broad.genome.objects.PersistentObject;
import edu.mit.broad.genome.objects.Template;
import edu.mit.broad.genome.reports.api.Report;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * In-memory cache of parsed PersistentObjects keyed by source path + class.
 */
public class ObjectCache {
    private final Logger log = LoggerFactory.getLogger(ObjectCache.class);

    private final Map<PathClass, Object> fPathClassObjectMap = new HashMap<>();
    private final Map<Object, String> fObjectPathStringMap = new HashMap<>();
    private final Map<PersistentObject, File> fInvisiblePobFileMap = new HashMap<>();
    private final Map fIdCachesExtra = new HashMap();
    /** First-seen representation class order (Swing ObjectCache tree category order). */
    private final java.util.LinkedHashSet<Class<?>> fRepresentationClassOrder = new java.util.LinkedHashSet<>();

    private final PropertyChangeSupport fPathChanges = new PropertyChangeSupport(this);
    private final PropertyChangeSupport fReportChanges = new PropertyChangeSupport(this);

    /** Gene sets extracted from loaded GMX/GMT matrices (Subsets chooser tab). */
    private final List<GeneSet> auxGeneSets = new ArrayList<>();

    public static final String PROP_PATH_ADDED = "path_added";
    public static final String PROP_REPORT_ADDED = "pob_added";

    public boolean isCached(final String path, final Class cl) {
        return fPathClassObjectMap.containsKey(new PathClass(path, cl));
    }

    public boolean isCached(final File file, final Class cl) {
        return isCached(file.getPath(), cl);
    }

    public boolean isCached(PersistentObject pob) {
        return fObjectPathStringMap.containsKey(pob) || fInvisiblePobFileMap.containsKey(pob);
    }

    public Object get(final String path, final Class cl) {
        return fPathClassObjectMap.get(new PathClass(path, cl));
    }

    public Object get(final File file, final Class cl) {
        return fPathClassObjectMap.get(new PathClass(file.getPath(), cl));
    }

    public String getSourcePath(final Object pob) {
        if (pob == null) {
            throw new IllegalArgumentException("Parameter obj cannot be null");
        }
        Object fo = fObjectPathStringMap.get(pob);
        if (fo == null && fInvisiblePobFileMap.containsKey(pob)) {
            fo = fInvisiblePobFileMap.get(pob).getPath();
        }
        if (fo != null) {
            return fo.toString();
        }
        throw new IllegalArgumentException("Unknown cache object -- use isCached() to check first if appropriate: "
                + pob + " class: " + pob.getClass());
    }

    public File getSourceFile(final Object pob) {
        String path = getSourcePath(pob);
        return path == null ? null : new File(path);
    }

    public void add(final File file, final PersistentObject pob, final Class cl) {
        add(file.getPath(), pob, cl);
    }

    protected void add(final File file, final PersistentObject pob, final Class cl, final boolean fireAction) {
        add(file.getPath(), pob, cl, fireAction);
    }

    protected void add(String path, PersistentObject pob, Class cl) {
        add(path, pob, cl, true);
    }

    protected void add(String path, PersistentObject pob, Class cl, boolean fireAction) {
        PathClass fc = new PathClass(path, cl);
        if (fObjectPathStringMap.containsKey(pob) && !(pob instanceof Template)) {
            if (log.isDebugEnabled()) {
                log.debug("Already cached object: {} in: {}", pob, fObjectPathStringMap.get(pob));
            }
        }
        fPathClassObjectMap.put(fc, pob);
        fObjectPathStringMap.put(pob, path);
        if (cl != null) {
            fRepresentationClassOrder.add(cl);
        }

        if (fireAction) {
            firePathAdded(new PropertyChangeEvent(this, PROP_PATH_ADDED, null, path));
            if (pob instanceof Report) {
                fireReportAdded(new PropertyChangeEvent(this, PROP_REPORT_ADDED, null, pob));
            }
        }

        for (Iterator iterator = fIdCachesExtra.keySet().iterator(); iterator.hasNext();) {
            Object key = iterator.next();
            ExtraCache ex = (ExtraCache) fIdCachesExtra.get(key);
            if (ex.isMember(path)) {
                ex.fExCache.add(path, pob, cl, fireAction);
            }
        }
    }

    protected void hackAddAuxSets(GeneSetMatrix gm) {
        if (gm == null) {
            return;
        }
        for (int i = 0; i < gm.getNumGeneSets(); i++) {
            auxGeneSets.add(gm.getGeneSet(i));
        }
        Collections.sort(auxGeneSets, ComparatorFactory.PERSISTENT_OBJECT_BY_NAME);
    }

    /**
     * Gene sets derived from loaded gene-set matrices, sorted by name.
     */
    public List<GeneSet> getAuxGeneSets() {
        return new ArrayList<>(auxGeneSets);
    }

    /** Cached objects of {@code pobClass}, sorted by name. */
    @SuppressWarnings("unchecked")
    public List getCachedObjectsL(Class pobClass) {
        List<PersistentObject> pobs = new ArrayList<>();
        for (PathClass pc : fPathClassObjectMap.keySet()) {
            if (pc.cl.equals(pobClass)) {
                pobs.add((PersistentObject) fPathClassObjectMap.get(pc));
            }
        }
        Collections.sort(pobs, ComparatorFactory.PERSISTENT_OBJECT_BY_NAME);
        return pobs;
    }

    /** Distinct representation classes currently present in the cache (Swing ObjectTree categories). */
    public java.util.Set<Class<?>> getCachedRepresentationClasses() {
        java.util.LinkedHashSet<Class<?>> classes = new java.util.LinkedHashSet<>(fRepresentationClassOrder);
        for (PathClass pc : fPathClassObjectMap.keySet()) {
            if (pc.cl != null) {
                classes.add(pc.cl);
            }
        }
        return classes;
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
    }

    protected void addInvisibly(File file, PersistentObject pob) {
        if (file == null) {
            throw new IllegalArgumentException("Parameter file cannot be null");
        }
        if (pob == null) {
            throw new IllegalArgumentException("Parameter pob cannot be null");
        }
        if (fInvisiblePobFileMap.containsKey(pob)) {
            log.warn("Already registered: {} overwriting", pob.getName());
        }
        fInvisiblePobFileMap.put(pob, file);
    }

    protected void addInvisibly(String source, PersistentObject pob) {
        if (source == null) {
            throw new IllegalArgumentException("Parameter source cannot be null");
        }
        addInvisibly(new File(source), pob);
    }

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
            return obj instanceof PathClass && equals((PathClass) obj);
        }

        public boolean equals(PathClass fc) {
            return fc.path.equals(this.path) && fc.cl.getName().equals(this.cl.getName());
        }

        public int hashCode() {
            return path.hashCode() + cl.getName().hashCode();
        }
    }

    static class ExtraCache {
        private ObjectCache fExCache;
        private String uniqId;

        boolean isMember(final String path) {
            return path.indexOf(uniqId) != -1;
        }

        public String toString() {
            return uniqId;
        }

        public int hashCode() {
            return uniqId.hashCode();
        }
    }
}
