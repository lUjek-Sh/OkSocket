package com.xuhao.didi.socket.client.impl.client;

import com.xuhao.didi.core.iocore.interfaces.IPulseSendable;
import com.xuhao.didi.core.iocore.interfaces.ISendable;
import com.xuhao.didi.socket.client.impl.exceptions.DogDeadException;
import com.xuhao.didi.socket.client.sdk.client.ConnectionInfo;
import com.xuhao.didi.socket.client.sdk.client.OkSocketOptions;
import com.xuhao.didi.socket.client.sdk.client.action.ISocketActionListener;
import com.xuhao.didi.socket.client.sdk.client.connection.AbsReconnectionManager;
import com.xuhao.didi.socket.client.sdk.client.connection.IConnectionManager;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PulseManagerTest {
    @Test
    public void pulseShouldDisconnectWhenFeedThresholdIsExceeded() throws Exception {
        FakeConnectionManager manager = new FakeConnectionManager();
        PulseManager pulseManager = new PulseManager(manager, new OkSocketOptions.Builder()
                .setPulseFrequency(1000L)
                .setPulseFeedLoseTimes(1)
                .build());
        pulseManager.setPulseSendable(new TestSendable());

        try {
            pulseManager.pulse();

            assertTrue(manager.awaitSendCount(1, 1000L));
            assertTrue(manager.awaitDisconnect(2500L));
            assertTrue(manager.disconnectException.get() instanceof DogDeadException);
            assertEquals(1, manager.sendCount.get());
        } finally {
            pulseManager.dead();
        }
    }

    @Test
    public void setOkOptionsShouldStopScheduledPulseInSimplexMode() throws Exception {
        FakeConnectionManager manager = new FakeConnectionManager();
        OkSocketOptions duplexOptions = new OkSocketOptions.Builder()
                .setPulseFrequency(1000L)
                .setPulseFeedLoseTimes(-1)
                .build();
        PulseManager pulseManager = new PulseManager(manager, duplexOptions);
        pulseManager.setPulseSendable(new TestSendable());

        try {
            pulseManager.pulse();

            assertTrue(manager.awaitSendCount(1, 1000L));
            pulseManager.setOkOptions(new OkSocketOptions.Builder(duplexOptions)
                    .setIOThreadMode(OkSocketOptions.IOThreadMode.SIMPLEX)
                    .build());

            Thread.sleep(1300L);
            assertEquals(1, manager.sendCount.get());
        } finally {
            pulseManager.dead();
        }
    }

    @Test
    public void setOkOptionsShouldReschedulePulseImmediately() throws Exception {
        FakeConnectionManager manager = new FakeConnectionManager();
        OkSocketOptions initialOptions = new OkSocketOptions.Builder()
                .setPulseFrequency(3000L)
                .setPulseFeedLoseTimes(-1)
                .build();
        PulseManager pulseManager = new PulseManager(manager, initialOptions);
        pulseManager.setPulseSendable(new TestSendable());

        try {
            pulseManager.pulse();

            assertTrue(manager.awaitSendCount(1, 1000L));
            long rescheduleAt = System.nanoTime();
            pulseManager.setOkOptions(new OkSocketOptions.Builder(initialOptions)
                    .setPulseFrequency(1000L)
                    .build());

            assertTrue(manager.awaitSendCount(2, 2200L));
            long secondSendAt = manager.secondSendAtNanos.get();
            long delayMillis = TimeUnit.NANOSECONDS.toMillis(secondSendAt - rescheduleAt);
            assertTrue("Expected rescheduled pulse in under 2000ms but was " + delayMillis + "ms", delayMillis < 2000L);
        } finally {
            pulseManager.dead();
        }
    }

    private static final class TestSendable implements IPulseSendable {
        @Override
        public byte[] parse() {
            return new byte[0];
        }
    }

    private static final class FakeConnectionManager implements IConnectionManager {
        private final ConnectionInfo connectionInfo = new ConnectionInfo("127.0.0.1", 12000);
        private final AtomicInteger sendCount = new AtomicInteger();
        private final AtomicLong secondSendAtNanos = new AtomicLong(-1L);
        private final CountDownLatch firstSendLatch = new CountDownLatch(1);
        private final CountDownLatch secondSendLatch = new CountDownLatch(1);
        private final CountDownLatch disconnectLatch = new CountDownLatch(1);
        private final AtomicReference<Exception> disconnectException = new AtomicReference<>();

        boolean awaitSendCount(int expectedCount, long timeoutMs) throws InterruptedException {
            if (expectedCount <= 1) {
                return firstSendLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
            }
            return secondSendLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
        }

        boolean awaitDisconnect(long timeoutMs) throws InterruptedException {
            return disconnectLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
        }

        @Override
        public void connect() {
        }

        @Override
        public IConnectionManager option(OkSocketOptions okOptions) {
            return this;
        }

        @Override
        public OkSocketOptions getOption() {
            return OkSocketOptions.getDefault();
        }

        @Override
        public void disconnect(Exception e) {
            disconnectException.set(e);
            disconnectLatch.countDown();
        }

        @Override
        public void disconnect() {
            disconnect(null);
        }

        @Override
        public IConnectionManager send(ISendable sendable) {
            int sends = sendCount.incrementAndGet();
            if (sends == 1) {
                firstSendLatch.countDown();
            } else if (sends == 2) {
                secondSendAtNanos.set(System.nanoTime());
                secondSendLatch.countDown();
            }
            return this;
        }

        @Override
        public IConnectionManager registerReceiver(ISocketActionListener socketActionListener) {
            return this;
        }

        @Override
        public IConnectionManager unRegisterReceiver(ISocketActionListener socketActionListener) {
            return this;
        }

        @Override
        public boolean isConnect() {
            return true;
        }

        @Override
        public boolean isDisconnecting() {
            return false;
        }

        @Override
        public PulseManager getPulseManager() {
            return null;
        }

        @Override
        public void setIsConnectionHolder(boolean isHold) {
        }

        @Override
        public ConnectionInfo getRemoteConnectionInfo() {
            return connectionInfo.clone();
        }

        @Override
        public ConnectionInfo getLocalConnectionInfo() {
            return null;
        }

        @Override
        public void setLocalConnectionInfo(ConnectionInfo localConnectionInfo) {
        }

        @Override
        public void switchConnectionInfo(ConnectionInfo info) {
        }

        @Override
        public AbsReconnectionManager getReconnectionManager() {
            return null;
        }
    }
}
