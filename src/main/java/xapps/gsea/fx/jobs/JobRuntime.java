/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.jobs;

import java.io.File;
import java.io.PrintStream;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import edu.mit.broad.genome.Conf;
import edu.mit.broad.genome.reports.api.Report;
import edu.mit.broad.genome.reports.api.ToolReport;
import edu.mit.broad.xbench.tui.JobState;
import edu.mit.broad.xbench.tui.RunContext;
import edu.mit.broad.xbench.tui.RunLogOutputStream;
import edu.mit.broad.xbench.tui.ToolFactory;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import xapps.gsea.fx.FxThreads;
import xtools.api.AbstractTool;
import xtools.api.CanceledException;
import xtools.api.Tool;
import xtools.api.param.ParamSet;

/**
 * Shell-owned job system: starts tools, tracks typed state, routes per-job logs,
 * and holds the observable Jobs list. Single entry point for analysis runs.
 */
public final class JobRuntime {

    public static final String MDC_RUN_ID = "gsea.runId";

    private static final org.slf4j.Logger klog = LoggerFactory.getLogger(JobRuntime.class);

    /** Notified on the FX thread after a job status change. */
    @FunctionalInterface
    public interface JobListener {
        void onJobUpdated(JobRecord job);
    }

    private final ApplicationLog applicationLog;
    private final ObservableList<JobRecord> jobs = FXCollections.observableArrayList();
    private final ConcurrentHashMap<String, JobRecord> byRunId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Worker> workers = new ConcurrentHashMap<>();
    private final ObjectProperty<JobRecord> selectedJob = new SimpleObjectProperty<>();
    private final List<JobListener> listeners = new CopyOnWriteArrayList<>();

    private volatile boolean captureInstalled;
    private PrintStream previousOut;
    private PrintStream previousErr;
    private Handler julHandler;

    private static volatile JobRuntime current;

    public JobRuntime(ApplicationLog applicationLog) {
        this.applicationLog = Objects.requireNonNull(applicationLog, "applicationLog");
        current = this;
    }

    /**
     * Runtime installed by the shell. Prefer constructor injection; this supports
     * relaunch / secondary panes created after the shell is up.
     */
    public static JobRuntime current() {
        return current;
    }

    public static JobRuntime require() {
        JobRuntime runtime = current;
        if (runtime == null) {
            throw new IllegalStateException("JobRuntime is not available");
        }
        return runtime;
    }

    public ApplicationLog getApplicationLog() {
        return applicationLog;
    }

    public ObservableList<JobRecord> getJobs() {
        return jobs;
    }

    public ObjectProperty<JobRecord> selectedJobProperty() {
        return selectedJob;
    }

    public JobRecord getSelectedJob() {
        return selectedJob.get();
    }

    public void setSelectedJob(JobRecord job) {
        selectedJob.set(job);
    }

    public JobRecord find(String runId) {
        return runId != null ? byRunId.get(runId) : null;
    }

    public void addListener(JobListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(JobListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    /** Install JUL + System.out/err routing once (call from the shell). */
    public synchronized void installCapture() {
        if (captureInstalled) {
            return;
        }
        previousOut = System.out;
        previousErr = System.err;
        PrintStream routing = RunLogOutputStream.printStream(null,
                (ignored, text) -> appendLog(currentRunId(), text));
        System.setOut(routing);
        System.setErr(routing);

        julHandler = new Handler() {
            private final SimpleFormatter formatter = new SimpleFormatter();

            @Override
            public void publish(LogRecord record) {
                if (!isLoggable(record)) {
                    return;
                }
                String line = formatter.format(record);
                String text = line.endsWith("\n") ? line : line + "\n";
                appendLog(currentRunId(), text);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        julHandler.setLevel(Conf.isDebugMode() ? Level.FINE : Level.INFO);
        Logger.getLogger("").addHandler(julHandler);
        captureInstalled = true;
    }

    /**
     * Start a tool run. Registers the job (with param snapshot) before the first status update.
     *
     * @return run id
     */
    public String start(Tool tool, ParamSet pset, int priority) throws Exception {
        if (tool == null) {
            throw new IllegalArgumentException("tool cannot be null");
        }
        if (pset == null) {
            throw new IllegalArgumentException("pset cannot be null");
        }

        String runId = UUID.randomUUID().toString();
        Properties snapshot = pset.toProperties();
        JobRecord record = new JobRecord(runId, tool.getName(), tool, snapshot);
        byRunId.put(runId, record);
        addJobToList(record);

        Tool clonedTool;
        try {
            clonedTool = ToolFactory.createTool(tool.getClass().getName(), snapshot);
        } catch (Exception t) {
            record.setLastError(t);
            updateStatus(record, JobState.INVALID_PARAM, null, null, null);
            return runId;
        }

        record.setName(clonedTool.getName());
        Worker worker = new Worker(record, clonedTool);
        workers.put(runId, worker);
        updateStatus(record, JobState.WAITING, null, null, null);

        Thread thread = new Thread(worker, "gsea-tool-" + clonedTool.getName() + "-" + runId.substring(0, 8));
        worker.ownerThread = thread;
        thread.setPriority(priority);
        thread.start();
        klog.debug("Started tool {} runId={} priority={}", clonedTool.getClass().getName(), runId, priority);
        return runId;
    }

    public boolean cancel(JobRecord job) {
        return job != null && cancel(job.getRunId());
    }

    public boolean cancel(String runId) {
        if (runId == null) {
            return false;
        }
        Worker worker = workers.get(runId);
        if (worker == null) {
            return false;
        }
        worker.requestCancel();
        updateStatus(worker.record, JobState.CANCELING, null, null, null);
        return true;
    }

    /** Auto-dialog for INVALID_PARAM only (start path). */
    public void showParamErrorIfNeeded(String runId) {
        JobErrors.showParamErrorIfNeeded(find(runId));
    }

    /** Route a log chunk to a job buffer or the application log. */
    public void appendLog(String runIdOrNull, String chunk) {
        if (chunk == null || chunk.isEmpty()) {
            return;
        }
        String runId = runIdOrNull != null ? runIdOrNull : currentRunId();
        JobRecord job = find(runId);
        if (job != null) {
            job.getLog().append(chunk);
            return;
        }
        applicationLog.append(chunk);
    }

    /** Cancel active workers, restore streams, and clear the installed singleton. */
    public synchronized void dispose() {
        listeners.clear();
        for (Worker w : workers.values()) {
            w.requestCancel();
        }
        workers.clear();
        if (captureInstalled) {
            if (previousOut != null) {
                System.setOut(previousOut);
            }
            if (previousErr != null) {
                System.setErr(previousErr);
            }
            if (julHandler != null) {
                Logger.getLogger("").removeHandler(julHandler);
                julHandler = null;
            }
            captureInstalled = false;
        }
        if (current == this) {
            current = null;
        }
    }

    private static String currentRunId() {
        String id = RunContext.currentRunId();
        if (id != null) {
            return id;
        }
        return MDC.get(MDC_RUN_ID);
    }

    private void addJobToList(JobRecord record) {
        FxThreads.runLater(() -> {
            if (!jobs.contains(record)) {
                jobs.add(0, record);
                if (selectedJob.get() == null) {
                    selectedJob.set(record);
                }
            }
        });
    }

    private void updateStatus(JobRecord record, JobState state, String label, File reportDir, URI reportIndex) {
        FxThreads.runLater(() -> {
            JobState currentState = record.getState();
            // Never regress CANCELING → RUNNING/WAITING, or any terminal → active.
            if (state != null && currentState != null) {
                if (currentState == JobState.CANCELING
                        && (state == JobState.RUNNING || state == JobState.WAITING)) {
                    return;
                }
                if (currentState.isTerminal() && !state.isTerminal()) {
                    return;
                }
            }
            record.applyStatus(state, label, reportDir, reportIndex);
            if (state != null && state.isTerminal()) {
                workers.remove(record.getRunId());
            }
            for (JobListener listener : listeners) {
                try {
                    listener.onJobUpdated(record);
                } catch (Exception e) {
                    klog.debug("Job listener failed", e);
                }
            }
        });
    }

    private final class Worker implements Runnable {
        private final JobRecord record;
        private final Tool tool;
        private volatile Thread ownerThread;
        private volatile boolean cancelRequested;

        private Worker(JobRecord record, Tool tool) {
            this.record = record;
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
            String runId = record.getRunId();
            MDC.put(MDC_RUN_ID, runId);
            RunContext.bind(runId);
            try {
                if (cancelRequested || Thread.currentThread().isInterrupted()) {
                    updateStatus(record, JobState.CANCELED, null, null, null);
                    return;
                }
                updateStatus(record, JobState.RUNNING, null, null, null);
                if (cancelRequested || Thread.currentThread().isInterrupted()) {
                    updateStatus(record, JobState.CANCELED, null, null, null);
                    return;
                }
                bindToolOutput();
                try {
                    tool.execute();
                    if (cancelRequested || Thread.currentThread().isInterrupted()) {
                        throw new CanceledException("Canceled");
                    }
                    finishSuccess();
                } catch (CanceledException ce) {
                    updateStatus(record, JobState.CANCELED, null, null, null);
                } catch (Throwable t) {
                    if (isCancellationFailure(t)) {
                        updateStatus(record, JobState.CANCELED, null, null, null);
                    } else {
                        finishError(t);
                    }
                }
            } finally {
                RunContext.unbind();
                MDC.remove(MDC_RUN_ID);
            }
        }

        private void bindToolOutput() {
            if (!(tool instanceof AbstractTool)) {
                return;
            }
            ((AbstractTool) tool).setOutputStream(
                    RunLogOutputStream.printStream(record.getRunId(), JobRuntime.this::appendLog));
        }

        private void finishSuccess() {
            File reportDir = null;
            URI index = null;
            Report report = tool.getReport();
            JobState doneState;
            String doneLabel;
            if (report == null) {
                doneState = JobState.ERROR;
                doneLabel = JobState.ERROR.defaultLabel();
            } else {
                reportDir = report.getReportDir();
                index = report.getReportIndex();
                boolean hasWarnings = report instanceof ToolReport
                        && !((ToolReport) report).getToolWarnings().isEmpty();
                if (hasWarnings) {
                    doneState = JobState.SUCCESS_WARN;
                    doneLabel = JobState.SUCCESS_WARN.defaultLabel();
                } else if (report.getNumPagesMade() == 0) {
                    doneState = JobState.SUCCESS;
                    doneLabel = "0 Result Objects";
                } else {
                    doneState = JobState.SUCCESS;
                    doneLabel = JobState.SUCCESS.defaultLabel();
                }
            }
            updateStatus(record, doneState, doneLabel, reportDir, index);
        }

        private void finishError(Throwable t) {
            record.setLastError(t);
            File reportDir = null;
            URI index = null;
            try {
                Report report = tool.getReport();
                if (report != null) {
                    report.setErroredOut();
                    reportDir = report.getReportDir();
                    index = report.getReportIndex();
                }
            } catch (Throwable ignored) {
            }
            updateStatus(record, JobState.ERROR, null, reportDir, index);
            klog.error("Tool failed: {} runId={}", tool.getName(), record.getRunId(), t);
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
