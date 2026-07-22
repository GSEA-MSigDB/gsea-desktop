/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.jobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import edu.mit.broad.xbench.tui.JobState;

public class LogBufferTest {

    @Test
    public void clearNotifiesNullAndRestoresBanner() {
        LogBuffer buffer = new LogBuffer("<banner>\n");
        AtomicReference<String> last = new AtomicReference<>("unset");
        buffer.addListener(last::set);
        buffer.append("line\n");
        buffer.clear();
        assertNull(last.get());
        assertTrue(buffer.getText().startsWith("<banner>"));
        assertFalse(buffer.getText().contains("line"));
    }

    @Test
    public void truncationNotifiesNullForFullReload() {
        LogBuffer buffer = new LogBuffer();
        AtomicReference<String> last = new AtomicReference<>("unset");
        buffer.addListener(last::set);
        StringBuilder big = new StringBuilder();
        while (big.length() < 520_000) {
            big.append("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\n");
        }
        buffer.append(big.toString());
        assertNull(last.get());
        assertTrue(buffer.getText().startsWith("…[log truncated]\n"));
        assertTrue(buffer.getText().length() <= 512_000 + 40);
    }

    @Test
    public void applicationLogBannerSurvivesClear() {
        ApplicationLog log = new ApplicationLog();
        log.append("noise\n");
        log.clear();
        assertTrue(log.getHistoryText().contains("Application messages will appear below"));
        assertFalse(log.getHistoryText().contains("noise"));
    }

    @Test
    public void jobErrorsTitles() {
        JobRecord invalid = new JobRecord("a", "T", null, null);
        invalid.applyStatus(JobState.INVALID_PARAM, null, null, null);
        assertEquals(JobErrors.INVALID_PARAM_TITLE, JobErrors.titleFor(invalid));

        JobRecord error = new JobRecord("b", "T", null, null);
        error.applyStatus(JobState.ERROR, null, null, null);
        assertEquals(JobErrors.EXECUTION_ERROR_TITLE, JobErrors.titleFor(error));

        JobRecord ok = new JobRecord("c", "T", null, null);
        ok.applyStatus(JobState.SUCCESS, null, null, null);
        assertFalse(ok.getState() == JobState.ERROR || ok.getState() == JobState.INVALID_PARAM);
    }
}
