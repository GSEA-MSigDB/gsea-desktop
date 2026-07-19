/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package edu.mit.broad.xbench.tui;

import java.io.File;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.broad.genome.reports.api.Report;
import edu.mit.broad.genome.reports.api.ToolReport;
import xtools.api.CanceledException;
import xtools.api.Tool;
import xtools.api.param.ParamSet;

/**
 * Runs tools on background threads and notifies listeners of status changes.
 * Each invocation has a unique {@code runId} so concurrent runs of the same tool
 * stay distinct (Swing {@code List<ToolRunnable>} identity parity).
 */
public class TaskManager {
    private static final Logger klog = LoggerFactory.getLogger(TaskManager.class);

    /**
     * Listener for tool status updates. {@code reportDir} is non-null when a report
     * directory is available. {@code runId} uniquely identifies the invocation;
     * {@code name} is the display name ({@link Tool#getName()}).
     */
    @FunctionalInterface
    public interface StatusListener {
        void onStatus(String runId, String name, String status, File reportDir);
    }

    private static TaskManager kInstance;
    private final List<ToolRunnable> fToolRunnables = new ArrayList<>();
    private final List<StatusListener> statusListeners = new CopyOnWriteArrayList<>();
    private final Map<String, File> reportDirsByRunId = new ConcurrentHashMap<>();
    private final Map<String, URI> reportIndexByRunId = new ConcurrentHashMap<>();
    private final Map<String, ToolRunnable> runningByRunId = new ConcurrentHashMap<>();
    private final Map<String, Throwable> lastErrorByRunId = new ConcurrentHashMap<>();
    private final java.util.Set<String> paramErrorRunIds = ConcurrentHashMap.newKeySet();

    public static TaskManager getInstance() {
        if (kInstance == null) {
            synchronized (TaskManager.class) {
                if (kInstance == null) {
                    kInstance = new TaskManager();
                }
            }
        }
        return kInstance;
    }

    private TaskManager() {
    }

    public void addStatusListener(StatusListener listener) {
        if (listener != null) {
            statusListeners.add(listener);
        }
    }

    public void removeStatusListener(StatusListener listener) {
        if (listener != null) {
            statusListeners.remove(listener);
        }
    }

    /** Report directory for a specific run, if any. */
    public File getReportDir(String runId) {
        return runId != null ? reportDirsByRunId.get(runId) : null;
    }

    /** Swing {@code report.getReportIndex()} for a specific run, if any. */
    public URI getReportIndex(String runId) {
        return runId != null ? reportIndexByRunId.get(runId) : null;
    }

    /** Most recent error for a specific run, if any. */
    public Throwable getLastError(String runId) {
        return runId != null ? lastErrorByRunId.get(runId) : null;
    }

    /**
     * Request cancellation of a running tool by {@code runId}.
     *
     * @return true if a running tool with that runId was found
     */
    public boolean cancel(String runId) {
        if (runId == null) {
            return false;
        }
        ToolRunnable runnable = runningByRunId.get(runId);
        if (runnable == null) {
            return false;
        }
        runnable.requestCancel();
        // Surface cancellation intent immediately for long-running/non-interruptible work.
        notifyStatus(runId, runnable.tool.getName(), "Canceled (requested)", null);
        return true;
    }

    /** Whether a tool thread is currently executing for this {@code runId}. */
    public boolean isRunning(String runId) {
        return runId != null && runningByRunId.containsKey(runId);
    }

    /**
     * True when {@link #run} failed during tool construction (Swing {@code PARAM_ERROR} /
     * {@code createParamErrorToolState}) — a table row exists but no worker was started.
     */
    public boolean isParamConstructionError(String runId) {
        return runId != null && paramErrorRunIds.contains(runId);
    }

    private void notifyStatus(String runId, String name, String status, File reportDir) {
        if (reportDir != null) {
            reportDirsByRunId.put(runId, reportDir);
        }
        for (StatusListener listener : statusListeners) {
            try {
                listener.onStatus(runId, name, status, reportDir);
            } catch (Exception e) {
                klog.debug("Status listener failed", e);
            }
        }
    }

    /**
     * @return unique run id for this invocation (for UI cancel / status correlation)
     */
    public String run(Tool tool, ParamSet pset, int priority) throws Exception {
        if (tool == null) {
            throw new IllegalArgumentException("Param tool cannot be null");
        }
        if (pset == null) {
            throw new IllegalArgumentException("Param pset cannot be null");
        }

        String runId = UUID.randomUUID().toString();
        Tool clonedTool;
        try {
            clonedTool = createTool(tool, pset);
        } catch (Exception t) {
            // Swing createParamErrorToolState: keep a table row with tool+pset for Name-click relaunch.
            // Return runId (do not throw) so callers can attachSnapshot before showing the error.
            lastErrorByRunId.put(runId, t);
            paramErrorRunIds.add(runId);
            // Swing ExecState.PARAM_ERROR.name == "Invalid Param(s)" (message is on throwable only).
            notifyStatus(runId, tool.getName(), "Invalid Param(s)", null);
            return runId;
        }

        ToolRunnable trunnable = new ToolRunnable(runId, clonedTool);
        synchronized (fToolRunnables) {
            fToolRunnables.add(trunnable);
        }
        runningByRunId.put(runId, trunnable);
        // Swing ToolRunnable starts as ExecState.WAITING before the worker thread sets RUNNING.
        notifyStatus(runId, clonedTool.getName(), "Waiting", null);

        Thread t = new Thread(trunnable, "gsea-tool-" + clonedTool.getName() + "-" + runId.substring(0, 8));
        trunnable.ownerThread = t;
        t.setPriority(priority);
        t.start();
        klog.debug("Started executing Tool: {} runId={} priority: {}",
                clonedTool.getClass().getName(), runId, priority);
        return runId;
    }

    public static Tool createTool(final Tool tool, final ParamSet pset) throws Exception {
        Class<?> toolClass = Class.forName(tool.getClass().getName());
        Constructor<?> ctor = toolClass.getConstructor(Properties.class);
        Properties prp = pset.toProperties();
        prp.remove("help");
        return (Tool) ctor.newInstance(prp);
    }

    public static Tool createTool(final String toolName) throws Exception {
        Class<?> toolClass = Class.forName(toolName);
        return (Tool) toolClass.getConstructor().newInstance();
    }

    private class ToolRunnable implements Runnable {
        private final String runId;
        private final Tool tool;
        private volatile Thread ownerThread;
        private volatile boolean cancelRequested;

        private ToolRunnable(String runId, Tool tool) {
            this.runId = runId;
            this.tool = tool;
        }

        void requestCancel() {
            cancelRequested = true;
            Thread t = ownerThread;
            if (t != null) {
                t.interrupt();
            }
        }

        @Override
        public void run() {
            if (cancelRequested || Thread.currentThread().isInterrupted()) {
                notifyStatus(runId, tool.getName(), "Canceled", null);
                return;
            }
            notifyStatus(runId, tool.getName(), "Running", null);
            try {
                if (cancelRequested || Thread.currentThread().isInterrupted()) {
                    throw new CanceledException("Canceled before start");
                }
                tool.execute();
                if (cancelRequested || Thread.currentThread().isInterrupted()) {
                    throw new CanceledException("Canceled");
                }
                File reportDir = null;
                Report report = tool.getReport();
                String doneStatus;
                if (report == null) {
                    // Swing ToolRunnable: null report → EXEC_ERROR; status text is ExecState name "Error!".
                    // throwable is left null (click still opens error dialog with null throwable).
                    doneStatus = "Error!";
                } else {
                    reportDir = report.getReportDir();
                    URI index = report.getReportIndex();
                    if (index != null) {
                        reportIndexByRunId.put(runId, index);
                    }
                    // Swing: SUCCESS_WARN takes state precedence over zero-page SUCCESS text.
                    boolean hasWarnings = report instanceof ToolReport
                            && !((ToolReport) report).getToolWarnings().isEmpty();
                    if (hasWarnings) {
                        doneStatus = "Success (with warnings)";
                    } else if (report.getNumPagesMade() == 0) {
                        doneStatus = "0 Result Objects";
                    } else {
                        doneStatus = "Success";
                    }
                }
                notifyStatus(runId, tool.getName(), doneStatus, reportDir);
                // Swing TaskManager does not auto-open the report; user clicks Status.
            } catch (CanceledException ce) {
                notifyStatus(runId, tool.getName(), "Canceled", null);
            } catch (Throwable t) {
                if (isCancellationFailure(t)) {
                    notifyStatus(runId, tool.getName(), "Canceled", null);
                } else {
                    lastErrorByRunId.put(runId, t);
                    File reportDir = null;
                    try {
                        Report report = tool.getReport();
                        if (report != null) {
                            // Swing: rename report dir to error_* and drop from cache.
                            report.setErroredOut();
                            reportDir = report.getReportDir();
                        }
                    } catch (Throwable ignored) {
                        // Keep original error as the status failure.
                    }
                    // Swing status cell shows ExecState.EXEC_ERROR.name == "Error!" only.
                    notifyStatus(runId, tool.getName(), "Error!", reportDir);
                    klog.error("Tool failed: {} runId={}", tool.getName(), runId, t);
                }
            } finally {
                runningByRunId.remove(runId, this);
            }
        }

        private boolean isCancellationFailure(Throwable error) {
            if (cancelRequested || Thread.currentThread().isInterrupted()) {
                return true;
            }

            Throwable current = error;
            while (current != null) {
                if (current instanceof CanceledException
                        || current instanceof InterruptedException
                        || current instanceof java.util.concurrent.CancellationException) {
                    return true;
                }
                current = current.getCause();
            }
            return false;
        }
    }
}
