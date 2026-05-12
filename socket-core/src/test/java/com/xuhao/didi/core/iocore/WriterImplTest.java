package com.xuhao.didi.core.iocore;

import com.xuhao.didi.core.exceptions.WriteException;
import com.xuhao.didi.core.iocore.interfaces.IIOCoreOptions;
import com.xuhao.didi.core.iocore.interfaces.IOAction;
import com.xuhao.didi.core.iocore.interfaces.ISendable;
import com.xuhao.didi.core.iocore.interfaces.IStateSender;
import com.xuhao.didi.core.protocol.IReaderProtocol;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.nio.ByteOrder;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class WriterImplTest {
    @Test
    public void offerShouldRejectWhenQueueCapacityExceeded() {
        WriterImpl writer = new WriterImpl();
        writer.setOption(new TestOptions(4, 1));

        writer.offer(new TestSendable(new byte[]{0x01}));

        try {
            writer.offer(new TestSendable(new byte[]{0x02}));
        } catch (WriteException e) {
            assertTrue(e.getMessage().contains("write queue is full"));
            return;
        }

        throw new AssertionError("Expected WriteException");
    }

    @Test
    public void writeShouldDrainQueueAndAllowNextOffer() {
        WriterImpl writer = new WriterImpl();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CapturingStateSender stateSender = new CapturingStateSender();
        TestSendable first = new TestSendable(new byte[]{0x01, 0x02, 0x03});
        TestSendable second = new TestSendable(new byte[]{0x04});

        writer.setOption(new TestOptions(2, 1));
        writer.initialize(outputStream, stateSender);
        writer.offer(first);

        assertTrue(writer.write());
        assertArrayEquals(new byte[]{0x01, 0x02, 0x03}, outputStream.toByteArray());
        assertEquals(IOAction.ACTION_WRITE_COMPLETE, stateSender.lastAction);
        assertSame(first, stateSender.lastPayload);

        writer.offer(second);
        assertTrue(writer.write());
        assertArrayEquals(new byte[]{0x01, 0x02, 0x03, 0x04}, outputStream.toByteArray());
    }

    @Test
    public void concurrentOffersShouldRespectConfiguredCapacity() throws InterruptedException {
        final int threadCount = 8;
        WriterImpl writer = new WriterImpl();
        writer.setOption(new TestOptions(4, 1));

        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failureCount = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            Thread thread = new Thread(() -> {
                ready.countDown();
                try {
                    start.await();
                    writer.offer(new TestSendable(new byte[]{(byte) index}));
                    successCount.incrementAndGet();
                } catch (WriteException e) {
                    failureCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(e);
                } finally {
                    done.countDown();
                }
            });
            thread.start();
        }

        ready.await();
        start.countDown();
        done.await();

        assertEquals(1, successCount.get());
        assertEquals(threadCount - 1, failureCount.get());
    }

    private static final class TestOptions implements IIOCoreOptions {
        private final int writePackageBytes;
        private final int writePackageQueueCapacity;

        private TestOptions(int writePackageBytes, int writePackageQueueCapacity) {
            this.writePackageBytes = writePackageBytes;
            this.writePackageQueueCapacity = writePackageQueueCapacity;
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
            return null;
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
            return writePackageBytes;
        }

        @Override
        public int getWritePackageQueueCapacity() {
            return writePackageQueueCapacity;
        }

        @Override
        public boolean isDebug() {
            return false;
        }
    }

    private static final class TestSendable implements ISendable {
        private final byte[] bytes;

        private TestSendable(byte[] bytes) {
            this.bytes = bytes;
        }

        @Override
        public byte[] parse() {
            return bytes;
        }
    }

    private static final class CapturingStateSender implements IStateSender {
        private String lastAction;
        private Serializable lastPayload;

        @Override
        public void sendBroadcast(String action, Serializable serializable) {
            lastAction = action;
            lastPayload = serializable;
        }

        @Override
        public void sendBroadcast(String action) {
            lastAction = action;
            lastPayload = null;
        }
    }
}
