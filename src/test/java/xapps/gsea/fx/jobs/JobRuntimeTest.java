/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.jobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.mit.broad.genome.reports.api.Report;
import edu.mit.broad.xbench.tui.JobState;
import edu.mit.broad.xbench.tui.RunContext;
import xtools.api.Tool;
import xtools.api.ToolCategory;
import xtools.api.param.ParamSet;
import xtools.api.param.ToolParamSet;

public class JobRuntimeTest {

    private ApplicationLog appLog;
    private JobRuntime runtime;

    @BeforeEach
    public void setUp() {
        appLog = new ApplicationLog();
        runtime = new JobRuntime(appLog);
    }

    @AfterEach
    public void tearDown() {
        if (runtime != null) {
            runtime.dispose();
        }
        RunContext.unbind();
    }

    @Test
    public void jobStateSemantics() {
        assertTrue(JobState.RUNNING.isActive());
        assertTrue(JobState.CANCELING.isActive());
        assertTrue(JobState.CANCELED.isTerminal());
        assertTrue(JobState.SUCCESS.isSuccess());
        assertEquals("Canceling…", JobState.CANCELING.defaultLabel());
    }

    @Test
    public void cancelEmitsCancelingThenCanceled() throws Exception {
        List<JobState> states = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch running = new CountDownLatch(1);
        CountDownLatch canceled = new CountDownLatch(1);
        runtime.addListener(job -> {
            states.add(job.getState());
            if (job.getState() == JobState.RUNNING) {
                running.countDown();
            }
            if (job.getState() == JobState.CANCELED) {
                canceled.countDown();
            }
        });

        String runId = runtime.start(new SlowFakeTool(), new ToolParamSet(), Thread.NORM_PRIORITY);
        assertTrue(running.await(5, TimeUnit.SECONDS));
        assertTrue(runtime.cancel(runId));
        assertTrue(states.contains(JobState.CANCELING));
        assertTrue(canceled.await(5, TimeUnit.SECONDS));
        assertEquals(JobState.CANCELED, runtime.find(runId).getState());
    }

    @Test
    public void snapshotPresentOnWaitingJob() throws Exception {
        CountDownLatch waiting = new CountDownLatch(1);
        AtomicReference<Properties> snap = new AtomicReference<>();
        runtime.addListener(job -> {
            if (job.getState() == JobState.WAITING) {
                snap.set(job.getParamSnapshot());
                waiting.countDown();
            }
        });

        ToolParamSet pset = new ToolParamSet();
        // Snapshot comes from pset.toProperties(); empty set is still non-null.
        String runId = runtime.start(new QuickFakeTool(), pset, Thread.NORM_PRIORITY);
        assertTrue(waiting.await(5, TimeUnit.SECONDS));
        assertNotNull(snap.get());
        assertNotNull(runtime.find(runId).getParamSnapshot());
    }

    @Test
    public void concurrentRunsRouteLogsToCorrectBuffers() throws Exception {
        CountDownLatch gate = new CountDownLatch(2);
        CountDownLatch bothDone = new CountDownLatch(2);
        runtime.addListener(job -> {
            if (job.getState() != null && job.getState().isTerminal()) {
                bothDone.countDown();
            }
        });

        String runA = runtime.start(new TagATool(gate), new ToolParamSet(), Thread.NORM_PRIORITY);
        String runB = runtime.start(new TagBTool(gate), new ToolParamSet(), Thread.NORM_PRIORITY);
        assertTrue(bothDone.await(10, TimeUnit.SECONDS));

        JobRecord jobA = runtime.find(runA);
        JobRecord jobB = runtime.find(runB);
        assertTrue(jobA.getLog().getText().contains("tag-A"), jobA.getLog().getText());
        assertTrue(jobB.getLog().getText().contains("tag-B"), jobB.getLog().getText());
        assertFalse(jobA.getLog().getText().contains("tag-B"));
        assertFalse(jobB.getLog().getText().contains("tag-A"));
    }

    @Test
    public void unboundOutputGoesToApplicationLog() {
        appLog.clear();
        RunContext.unbind();
        runtime.appendLog(null, "global-only-line\n");
        assertTrue(appLog.getHistoryText().contains("global-only-line"));
    }

    @Test
    public void logBufferTracksLastLine() {
        JobLogBuffer buffer = new JobLogBuffer();
        buffer.append("line one\n");
        buffer.append("line two\n");
        assertEquals("line two", buffer.getLastLine());
    }

    @Test
    public void invalidParamKeepsSnapshotAndError() throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        runtime.addListener(job -> {
            if (job.getState() == JobState.INVALID_PARAM) {
                done.countDown();
            }
        });
        String runId = runtime.start(new NoPropertiesCtorTool(), new ToolParamSet(), Thread.NORM_PRIORITY);
        assertTrue(done.await(5, TimeUnit.SECONDS));
        JobRecord job = runtime.find(runId);
        assertEquals(JobState.INVALID_PARAM, job.getState());
        assertNotNull(job.getParamSnapshot());
        assertNotNull(job.getLastError());
        assertTrue(job.isParamError());
    }

    @Test
    public void cancelDoesNotRegressToRunning() throws Exception {
        CountDownLatch running = new CountDownLatch(1);
        CountDownLatch canceled = new CountDownLatch(1);
        List<JobState> states = Collections.synchronizedList(new ArrayList<>());
        runtime.addListener(job -> {
            states.add(job.getState());
            if (job.getState() == JobState.RUNNING) {
                running.countDown();
            }
            if (job.getState() == JobState.CANCELED) {
                canceled.countDown();
            }
        });
        String runId = runtime.start(new SlowFakeTool(), new ToolParamSet(), Thread.NORM_PRIORITY);
        assertTrue(running.await(5, TimeUnit.SECONDS));
        assertTrue(runtime.cancel(runId));
        assertTrue(canceled.await(5, TimeUnit.SECONDS));
        int lastRunning = states.lastIndexOf(JobState.RUNNING);
        int firstCanceling = states.indexOf(JobState.CANCELING);
        int lastCanceled = states.lastIndexOf(JobState.CANCELED);
        // After CANCELING appears, RUNNING must not reappear.
        if (firstCanceling >= 0) {
            for (int i = firstCanceling + 1; i < states.size(); i++) {
                assertFalse(states.get(i) == JobState.RUNNING,
                        "RUNNING after CANCELING: " + states);
            }
        }
        assertTrue(lastCanceled > lastRunning);
    }

    @Test
    public void disposeClearsCurrent() {
        assertEquals(runtime, JobRuntime.current());
        runtime.dispose();
        assertEquals(null, JobRuntime.current());
        runtime = null; // tearDown skip
    }

    @Test
    public void listTitleIncludesAnalysisName() {
        Properties params = new Properties();
        params.setProperty("rpt_label", "lung_vs_normal");
        params.setProperty("res", "C:/data/Expression.gct");
        params.setProperty("cls", "C:/data/phenotypes.cls#Lung_versus_Normal");
        params.setProperty("gmx", "C:/data/h.all.v2024.1.Hs.symbols.gmt");

        assertEquals("GSEA — lung_vs_normal", JobDisplay.listTitle("GSEA", params));

        String hover = JobDisplay.hoverText(params);
        assertNotNull(hover);
        assertTrue(hover.contains("Expression dataset: Expression.gct"), hover);
        assertTrue(hover.contains("Phenotype comparison: Lung_versus_Normal"), hover);
        assertTrue(hover.contains("Gene set database: h.all.v2024.1.Hs.symbols.gmt"), hover);
    }

    @Test
    public void prerankedHoverUsesRankedList() {
        Properties params = new Properties();
        params.setProperty("rpt_label", "prerank");
        params.setProperty("rnk", "C:/data/my_ranks.rnk");
        params.setProperty("gmx", "C:/data/c2.gmt");
        String hover = JobDisplay.hoverText(params);
        assertNotNull(hover);
        assertTrue(hover.contains("Ranked list: my_ranks.rnk"), hover);
        assertTrue(hover.contains("Gene set database: c2.gmt"), hover);
        assertFalse(hover.contains("Expression dataset"));
        assertFalse(hover.contains("Phenotype"));
    }

    @Test
    public void collapseHoverShowsDatasetChipAndMode() {
        Properties params = new Properties();
        params.setProperty("rpt_label", "collapse1");
        params.setProperty("res", "C:/data/raw.gct");
        params.setProperty("chip", "C:/chips/Human_Gene.chip");
        params.setProperty("mode", "Max_probe");
        String hover = JobDisplay.hoverText(params);
        assertNotNull(hover);
        assertTrue(hover.contains("Expression dataset: raw.gct"), hover);
        assertTrue(hover.contains("Chip platform: Human_Gene.chip"), hover);
        assertTrue(hover.contains("Collapsing mode: Max_probe"), hover);
    }

    @Test
    public void chip2ChipHoverShowsGeneSetsAndTarget() {
        Properties params = new Properties();
        params.setProperty("gmx", "C:/data/symbols.gmt");
        params.setProperty("chip_target", "Mouse_Gene.chip");
        String hover = JobDisplay.hoverText(params);
        assertNotNull(hover);
        assertTrue(hover.contains("Gene set database: symbols.gmt"), hover);
        assertTrue(hover.contains("Target chip: Mouse_Gene.chip"), hover);
    }

    @Test
    public void leadingEdgeHoverShowsResultFolderAndGeneSets() {
        Properties params = new Properties();
        params.setProperty("dir", "C:/reports/my_GSEA.Gsea.123");
        params.setProperty("gsets", "SET_A,SET_B,SET_C");
        String hover = JobDisplay.hoverText(params);
        assertNotNull(hover);
        assertTrue(hover.contains("GSEA result folder: my_GSEA.Gsea.123"), hover);
        assertTrue(hover.contains("Gene sets: SET_A,SET_B,SET_C"), hover);
    }

    @Test
    public void jobRecordNameUsesAnalysisLabel() {
        Properties params = new Properties();
        params.setProperty("rpt_label", "my_run");
        JobRecord job = new JobRecord("id", "GSEA", null, params);
        assertEquals("GSEA — my_run", job.getName());
        job.setName("GseaPreranked");
        assertEquals("GseaPreranked — my_run", job.getName());
    }

    @Test
    public void listenerCanRebindAfterDispose() throws Exception {
        CountDownLatch first = new CountDownLatch(1);
        JobRuntime.JobListener listener = job -> {
            if (job.getState() == JobState.WAITING) {
                first.countDown();
            }
        };
        runtime.addListener(listener);
        runtime.addListener(listener); // idempotent
        runtime.start(new QuickFakeTool(), new ToolParamSet(), Thread.NORM_PRIORITY);
        assertTrue(first.await(5, TimeUnit.SECONDS));

        runtime.dispose();
        // Same instance reused after dispose must accept listeners again.
        CountDownLatch second = new CountDownLatch(1);
        runtime.addListener(job -> {
            if (job.getState() == JobState.WAITING) {
                second.countDown();
            }
        });
        runtime.start(new QuickFakeTool(), new ToolParamSet(), Thread.NORM_PRIORITY);
        assertTrue(second.await(5, TimeUnit.SECONDS));
        runtime.dispose();
        runtime = null;
    }

    /** Tool without Properties constructor → ToolFactory fails → INVALID_PARAM. */
    public static final class NoPropertiesCtorTool implements Tool {
        public NoPropertiesCtorTool() {
        }

        @Override
        public void execute() {
        }

        @Override
        public String getHelpURL() {
            return "";
        }

        @Override
        public String getName() {
            return "NoPropertiesCtorTool";
        }

        @Override
        public String getTitle() {
            return getName();
        }

        @Override
        public String getDesc() {
            return "";
        }

        @Override
        public ToolCategory getCategory() {
            return null;
        }

        @Override
        public ParamSet getParamSet() {
            return new ToolParamSet();
        }

        @Override
        public void declareParams() {
        }

        @Override
        public Report getReport() {
            return null;
        }
    }

    public static final class SlowFakeTool implements Tool {
        public SlowFakeTool() {
        }

        public SlowFakeTool(Properties ignored) {
        }

        @Override
        public void execute() throws Exception {
            long end = System.currentTimeMillis() + 30_000;
            while (System.currentTimeMillis() < end) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new xtools.api.CanceledException("Canceled");
                }
                Thread.sleep(50);
            }
        }

        @Override
        public String getHelpURL() {
            return "";
        }

        @Override
        public String getName() {
            return "SlowFakeTool";
        }

        @Override
        public String getTitle() {
            return getName();
        }

        @Override
        public String getDesc() {
            return "";
        }

        @Override
        public ToolCategory getCategory() {
            return null;
        }

        @Override
        public ParamSet getParamSet() {
            return new ToolParamSet();
        }

        @Override
        public void declareParams() {
        }

        @Override
        public Report getReport() {
            return null;
        }
    }

    public static final class QuickFakeTool implements Tool {
        public QuickFakeTool() {
        }

        public QuickFakeTool(Properties ignored) {
        }

        @Override
        public void execute() {
        }

        @Override
        public String getHelpURL() {
            return "";
        }

        @Override
        public String getName() {
            return "QuickFakeTool";
        }

        @Override
        public String getTitle() {
            return getName();
        }

        @Override
        public String getDesc() {
            return "";
        }

        @Override
        public ToolCategory getCategory() {
            return null;
        }

        @Override
        public ParamSet getParamSet() {
            return new ToolParamSet();
        }

        @Override
        public void declareParams() {
        }

        @Override
        public Report getReport() {
            return null;
        }
    }

    public static final class TagATool extends xtools.api.AbstractTool {
        private static volatile CountDownLatch gate = new CountDownLatch(0);

        public TagATool(CountDownLatch sharedGate) {
            super("TagATool");
            gate = sharedGate;
        }

        public TagATool(Properties ignored) {
            super("TagATool");
        }

        @Override
        public void execute() throws Exception {
            gate.countDown();
            gate.await(5, TimeUnit.SECONDS);
            getOutputStream().println("tag-A");
        }

        @Override
        public String getName() {
            return "TagATool";
        }

        @Override
        public String getTitle() {
            return getName();
        }

        @Override
        public String getDesc() {
            return "";
        }

        @Override
        public ToolCategory getCategory() {
            return null;
        }

        @Override
        public void declareParams() {
        }
    }

    public static final class TagBTool extends xtools.api.AbstractTool {
        private static volatile CountDownLatch gate = new CountDownLatch(0);

        public TagBTool(CountDownLatch sharedGate) {
            super("TagBTool");
            gate = sharedGate;
        }

        public TagBTool(Properties ignored) {
            super("TagBTool");
        }

        @Override
        public void execute() throws Exception {
            gate.countDown();
            gate.await(5, TimeUnit.SECONDS);
            getOutputStream().println("tag-B");
        }

        @Override
        public String getName() {
            return "TagBTool";
        }

        @Override
        public String getTitle() {
            return getName();
        }

        @Override
        public String getDesc() {
            return "";
        }

        @Override
        public ToolCategory getCategory() {
            return null;
        }

        @Override
        public void declareParams() {
        }
    }
}
