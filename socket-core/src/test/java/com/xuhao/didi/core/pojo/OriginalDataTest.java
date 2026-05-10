package com.xuhao.didi.core.pojo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OriginalDataTest {
    @Test
    public void readDelayHintShouldPointToHeaderWhenHeaderDominates() {
        OriginalData data = new OriginalData();
        data.setHeadBytes(new byte[4]);
        data.setBodyBytes(new byte[0]);
        data.setReadSequence(18L);
        data.setReadStartedAtEpochMillis(1778433014414L);
        data.setReadCompletedAtEpochMillis(1778433043956L);
        data.setReadStartNanoTime(0L);
        data.setReadHeaderFirstByteNanoTime(29_540_000_000L);
        data.setReadHeaderCompleteNanoTime(29_540_000_000L);
        data.setReadHeaderChunkCount(1);
        data.setReadBodyFirstByteNanoTime(29_540_000_000L);
        data.setReadBodyCompleteNanoTime(29_540_000_000L);
        data.setReadBodyChunkCount(0);
        data.setReadCompleteNanoTime(29_540_000_000L);

        assertEquals("waiting_for_header_bytes", data.getReadDelayHint());
        assertTrue(data.getReadDiagnosticsSummary().contains("seq=18"));
        assertTrue(data.getReadDiagnosticsSummary().contains("headerMs=29540"));
        assertTrue(data.getReadDiagnosticsSummary().contains("hint=waiting_for_header_bytes"));
    }

    @Test
    public void readDelayHintShouldPointToBodyFragmentationWhenBodyDominates() {
        OriginalData data = new OriginalData();
        data.setHeadBytes(new byte[4]);
        data.setBodyBytes(new byte[32]);
        data.setReadStartNanoTime(0L);
        data.setReadHeaderFirstByteNanoTime(1_000_000L);
        data.setReadHeaderCompleteNanoTime(1_000_000L);
        data.setReadHeaderChunkCount(1);
        data.setReadBodyFirstByteNanoTime(2_000_000L);
        data.setReadBodyCompleteNanoTime(25_000_000L);
        data.setReadBodyChunkCount(4);
        data.setReadCompleteNanoTime(25_000_000L);

        assertEquals("body_fragmentation_or_partial_delivery", data.getReadDelayHint());
        assertTrue(data.getReadDiagnosticsSummary().contains("bodyChunks=4"));
        assertTrue(data.getReadDiagnosticsSummary().contains("hint=body_fragmentation_or_partial_delivery"));
    }
}
