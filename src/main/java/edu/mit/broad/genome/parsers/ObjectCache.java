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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * In-memory cache of parsed PersistentObjects keyed by source path + class.
 * Tool threads write this cache while the FX thread iterates it, so every map
 * access is under {@link #lock} and listeners run after the lock is released.
 */
public class ObjectCache {
    private final Logger log = LoggerFactory.getLogger(ObjectCache.class);
    private final Object lock = new Object();

    private final Map<PathClass, Object> fPathClassObjectMap = new HashMap<>();
    private final Map<Object, String> fObjectPathStringMap = new HashMap<>();
    private final Map<PersistentObject, File> fInvisiblePobFileMap = new HashMap<>();
    private final Map fIdCachesExtra = new HashMap();
    /** First-seen representation class order (object-tree category order). */
    private final LinkedHashSet<Class<?>> fRepresentationClassOrder = new LinkedHashSet<>();

    private final PropertyChangeSupport fPathChanges = new PropertyChangeSupport(this);
    private final PropertyChangeSupport fReportChanges = new PropertyChangeSupport(this);

    /** Gene sets extracted from loaded GMX/GMT matrices (Subsets chooser tab). */
    private final List<GeneSet> auxGeneSets = new ArrayList<>();

    public static final String PROP_PATH_ADDED = "path_added";
    public static final String PROP_REPORT_ADDED = "pob_added";

    public boolean isCached(final String path, final Class cl) {
        synchronized (lock) {
            return fPathClassObjectMap.containsKey(new PathClass(path, cl));
        }
    }

    public boolean isCached(final File file, final Class cl) {
        return isCached(file.getPath(), cl);
    }

    public boolean isCached(PersistentObject pob) {
        synchronized (lock) {
            return fObjectPathStringMap.containsKey(pob) || fInvisiblePobFileMap.containsKey(pob);
        }
    }

    public Object get(final String path, final Class cl) {
        synchronized (lock) {
            return fPathClassObjectMap.get(new PathClass(path, cl));
        }
    }

    public Object get(final File file, final Class cl) {
        return get(file.getPath(), cl);
    }

    public String getSourcePath(final Object pob) {
        if (pob == null) {
            throw new IllegalArgumentException("Parameter obj cannot be null");
        }
        synchronized (lock) {
            Object fo = fObjectPathStringMap.get(pob);
            if (fo == null && fInvisiblePobFileMap.containsKey(pob)) {
                fo = fInvisiblePobFileMap.get(pob).getPath();
            }
            if (fo != null) {
                return fo.toString();
            }
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
        List<PropertyChangeEvent> pathEvents = new ArrayList<>();
        List<PropertyChangeEvent> reportEvents = new ArrayList<>();
        List<ExtraCache> extras = new ArrayList<>();
        synchronized (lock) {
            remember(path, pob, cl, fireAction, pathEvents, reportEvents);
            for (Object key : fIdCachesExtra.keySet()) {
                ExtraCache ex = (ExtraCache) fIdCachesExtra.get(key);
                if (ex.isMember(path)) {
                    extras.add(ex);
                }
            }
        }
        dispatch(pathEvents, reportEvents);
        for (ExtraCache ex : extras) {
            ex.fExCache.add(path, pob, cl, fireAction);
        }
    }

    private void remember(String path, PersistentObject pob, Class cl, boolean fireAction,
            List<PropertyChangeEvent> pathEvents, List<PropertyChangeEvent> reportEvents) {
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
            pathEvents.add(new PropertyChangeEvent(this, PROP_PATH_ADDED, null, path));
            if (pob instanceof Report) {
                reportEvents.add(new PropertyChangeEvent(this, PROP_REPORT_ADDED, null, pob));
            }
        }
    }

    private void dispatch(List<PropertyChangeEvent> pathEvents, List<PropertyChangeEvent> reportEvents) {
        for (PropertyChangeEvent evt : pathEvents) {
            fPathChanges.firePropertyChange(evt);
        }
        for (PropertyChangeEvent evt : reportEvents) {
            fReportChanges.firePropertyChange(evt);
        }
    }

    protected void hackAddAuxSets(GeneSetMatrix gm) {
        if (gm == null) {
            return;
        }
        synchronized (lock) {
            for (int i = 0; i < gm.getNumGeneSets(); i++) {
                auxGeneSets.add(gm.getGeneSet(i));
            }
            Collections.sort(auxGeneSets, ComparatorFactory.PERSISTENT_OBJECT_BY_NAME);
        }
    }

    /**
     * Gene sets derived from loaded gene-set matrices, sorted by name.
     */
    public List<GeneSet> getAuxGeneSets() {
        synchronized (lock) {
            return new ArrayList<>(auxGeneSets);
        }
    }

    /** Cached objects of {@code pobClass}, sorted by name. */
    @SuppressWarnings("unchecked")
    public List getCachedObjectsL(Class pobClass) {
        List<PersistentObject> pobs = new ArrayList<>();
        synchronized (lock) {
            for (PathClass pc : fPathClassObjectMap.keySet()) {
                if (pc.cl.equals(pobClass)) {
                    pobs.add((PersistentObject) fPathClassObjectMap.get(pc));
                }
            }
        }
        Collections.sort(pobs, ComparatorFactory.PERSISTENT_OBJECT_BY_NAME);
        return pobs;
    }

    /** Distinct representation classes currently present in the cache. */
    public java.util.Set<Class<?>> getCachedRepresentationClasses() {
        synchronized (lock) {
            LinkedHashSet<Class<?>> classes = new LinkedHashSet<>(fRepresentationClassOrder);
            for (PathClass pc : fPathClassObjectMap.keySet()) {
                if (pc.cl != null) {
                    classes.add(pc.cl);
                }
            }
            return classes;
        }
    }

    public void addPathAdditionsListener(PropertyChangeListener p) {
        fPathChanges.addPropertyChangeListener(p);
    }

    public void addReportAdditionsListener(PropertyChangeListener p) {
        fReportChanges.addPropertyChangeListener(p);
    }

    public void makeVisible(PersistentObject pob, Class cl) {
        makeVisible(new PersistentObject[]{pob}, cl);
    }

    public void makeVisible(PersistentObject[] pobs, Class cl) {
        List<Object[]> pending = new ArrayList<>();
        synchronized (lock) {
            for (int i = 0; i < pobs.length; i++) {
                File file = fInvisiblePobFileMap.remove(pobs[i]);
                if (file != null) {
                    pending.add(new Object[]{file, pobs[i]});
                }
            }
        }
        for (Object[] pair : pending) {
            add((File) pair[0], (PersistentObject) pair[1], cl, false);
        }
    }

    protected void addInvisibly(File file, PersistentObject pob) {
        if (file == null) {
            throw new IllegalArgumentException("Parameter file cannot be null");
        }
        if (pob == null) {
            throw new IllegalArgumentException("Parameter pob cannot be null");
        }
        synchronized (lock) {
            if (fInvisiblePobFileMap.containsKey(pob)) {
                log.warn("Already registered: {} overwriting", pob.getName());
            }
            fInvisiblePobFileMap.put(pob, file);
        }
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
