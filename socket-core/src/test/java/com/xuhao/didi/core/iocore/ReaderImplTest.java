package com.xuhao.didi.core.iocore;

import com.xuhao.didi.core.exceptions.ReadException;
import com.xuhao.didi.core.iocore.interfaces.IIOCoreOptions;
import com.xuhao.didi.core.iocore.interfaces.IStateSender;
import com.xuhao.didi.core.pojo.OriginalData;
import com.xuhao.didi.core.protocol.IReaderProtocol;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.nio.ByteOrder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;

public class ReaderImplTest {
    @Test
    public void readShouldNotWrapReadExceptionTwice() {
        ReaderImpl reader = new ReaderImpl();
        reader.setOption(new TestOptions(0));
        reader.initialize(new ByteArrayInputStream(new byte[]{0x01}), new NoOpStateSender());

        try {
            reader.read();
        } catch (ReadException e) {
            assertEquals("socket input stream reached end of file while reading 4 bytes", e.getMessage());
            assertNull(e.getCause());
            return;
        }

        throw new AssertionError("Expected ReadException");
    }

    @Test
    public void readShouldPopulateDiagnosticsMetadata() {
        byte[] bytes = new byte[]{0x00, 0x00, 0x00, 0x01, 0x7F};
        ReaderImpl reader = new ReaderImpl();
        CapturingStateSender sender = new CapturingStateSender();
        reader.setOption(new TestOptions(1));
        reader.initialize(new ByteArrayInputStream(bytes), sender);

        reader.read();

        OriginalData data = sender.lastOriginalData;
        assertEquals(1L, data.getReadSequence());
        assertEquals(1, data.getBodyBytes().length);
        assertEquals(1L, data.getReadStartedAtEpochMillis() > 0 ? 1L : 0L);
        assertEquals(1L, data.getReadCompletedAtEpochMillis() >= data.getReadStartedAtEpochMillis() ? 1L : 0L);
        assertEquals(1L, data.getReadHeaderCompleteNanoTime() >= data.getReadStartNanoTime() ? 1L : 0L);
        assertEquals(1L, data.getReadHeaderFirstByteNanoTime() >= data.getReadStartNanoTime() ? 1L : 0L);
        assertEquals(1L, data.getReadBodyCompleteNanoTime() >= data.getReadHeaderCompleteNanoTime() ? 1L : 0L);
        assertEquals(1L, data.getReadBodyFirstByteNanoTime() >= data.getReadHeaderCompleteNanoTime() ? 1L : 0L);
        assertEquals(1L, data.getReadCompleteNanoTime() >= data.getReadBodyCompleteNanoTime() ? 1L : 0L);
        assertEquals(1, data.getReadHeaderChunkCount());
        assertEquals(1, data.getReadBodyChunkCount());
        assertTrue(data.getReadDelayHint().startsWith("waiting_for_"));
        assertTrue(data.getReadDiagnosticsSummary().contains("seq=1"));
        assertTrue(data.getReadDiagnosticsSummary().contains("headerLength=4"));
        assertTrue(data.getReadDiagnosticsSummary().contains("bodyLength=1"));
        assertTrue(data.getReadDiagnosticsSummary().contains("hint=" + data.getReadDelayHint()));
    }

    private static final class TestOptions implements IIOCoreOptions {
        private final int bodyLength;
        private final IReaderProtocol protocol = new IReaderProtocol() {
            @Override
            public int getHeaderLength() {
                return 4;
            }

            @Override
            public int getBodyLength(byte[] header, ByteOrder byteOrder) {
                return bodyLength;
            }
        };

        private TestOptions(int bodyLength) {
            this.bodyLength = bodyLength;
        }

        @Override
        public ByteOrder getReadByteOrder() {
            return ByteOrder.BIG_ENDIAN;
        }

        @Override
        public int getMaxReadDataMB() {
            return 1;
        }

        @Override
        public IReaderProtocol getReaderProtocol() {
            return protocol;
        }

        @Override
        public ByteOrder getWriteByteOrder() {
            return ByteOrder.BIG_ENDIAN;
        }

        @Override
        public int getReadPackageBytes() {
            return 1;
        }

        @Override
        public int getWritePackageBytes() {
            return 1;
        }

        @Override
        public int getWritePackageQueueCapacity() {
            return 1;
        }

        @Override
        public boolean isDebug() {
            return false;
        }
    }

    private static final class NoOpStateSender implements IStateSender {
        @Override
        public void sendBroadcast(String action, Serializable serializable) {
        }

        @Override
        public void sendBroadcast(String action) {
        }
    }

    private static final class CapturingStateSender implements IStateSender {
        private OriginalData lastOriginalData;

        @Override
        public void sendBroadcast(String action, Serializable serializable) {
            lastOriginalData = (OriginalData) serializable;
        }

        @Override
        public void sendBroadcast(String action) {
        }
    }
}
