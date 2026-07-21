/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.jobs;

import java.io.File;
import java.net.URI;
import java.util.Properties;

import edu.mit.broad.xbench.tui.JobState;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import xtools.api.Tool;

/**
 * One tool invocation tracked by {@link JobRuntime} and the Jobs UI.
 */
public final class JobRecord {

    private final String runId;
    private final StringProperty name = new SimpleStringProperty();
    private final ObjectProperty<JobState> state = new SimpleObjectProperty<>(JobState.WAITING);
    private final StringProperty statusLabel = new SimpleStringProperty(JobState.WAITING.defaultLabel());
    private final JobLogBuffer log = new JobLogBuffer();
    private final long startedAtMillis = System.currentTimeMillis();
    private volatile Long finishedAtMillis;
    private volatile File reportDir;
    private volatile URI reportIndex;
    private volatile Tool tool;
    private volatile Properties paramSnapshot;
    private volatile Throwable lastError;

    public JobRecord(String runId, String name, Tool tool, Properties paramSnapshot) {
        this.runId = runId;
        this.tool = tool;
        this.paramSnapshot = paramSnapshot;
        this.name.set(JobDisplay.listTitle(name, paramSnapshot));
    }

    public String getRunId() {
        return runId;
    }

    public StringProperty nameProperty() {
        return name;
    }

    public String getName() {
        return name.get();
    }

    public void setName(String name) {
        this.name.set(JobDisplay.listTitle(name, paramSnapshot));
    }

    /** Hover details for the Jobs list (dataset / phenotype / gene sets when present). */
    public String getHoverText() {
        return JobDisplay.hoverText(paramSnapshot);
    }

    public ObjectProperty<JobState> stateProperty() {
        return state;
    }

    public JobState getState() {
        return state.get();
    }

    public StringProperty statusLabelProperty() {
        return statusLabel;
    }

    public String getStatusLabel() {
        return statusLabel.get();
    }

    public JobLogBuffer getLog() {
        return log;
    }

    public long getStartedAtMillis() {
        return startedAtMillis;
    }

    public File getReportDir() {
        return reportDir;
    }

    public void setReportDir(File reportDir) {
        if (reportDir != null) {
            this.reportDir = reportDir;
        }
    }

    public URI getReportIndex() {
        return reportIndex;
    }

    public void setReportIndex(URI reportIndex) {
        if (reportIndex != null) {
            this.reportIndex = reportIndex;
        }
    }

    public Tool getTool() {
        return tool;
    }

    public Properties getParamSnapshot() {
        return paramSnapshot;
    }

    public Throwable getLastError() {
        return lastError;
    }

    public void setLastError(Throwable lastError) {
        this.lastError = lastError;
    }

    public boolean isParamError() {
        return getState() == JobState.INVALID_PARAM;
    }

    void applyStatus(JobState newState, String label, File reportDir, URI reportIndex) {
        if (newState != null) {
            state.set(newState);
            if (newState.isTerminal() && finishedAtMillis == null) {
                finishedAtMillis = System.currentTimeMillis();
            }
        }
        if (label != null && !label.isBlank()) {
            statusLabel.set(label);
        } else if (newState != null) {
            statusLabel.set(newState.defaultLabel());
        }
        setReportDir(reportDir);
        setReportIndex(reportIndex);
    }

    /** Elapsed time text for active jobs, or total duration for finished ones. */
    public String formatElapsed() {
        long end = finishedAtMillis != null ? finishedAtMillis : System.currentTimeMillis();
        long secs = Math.max(0, (end - startedAtMillis) / 1000);
        long m = secs / 60;
        long s = secs % 60;
        if (m > 0) {
            return m + "m " + s + "s";
        }
        return s + "s";
    }
}
