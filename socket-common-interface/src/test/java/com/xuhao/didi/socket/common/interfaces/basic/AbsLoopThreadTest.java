package com.xuhao.didi.socket.common.interfaces.basic;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class AbsLoopThreadTest {

    @Test
    public void shutdownShouldKeepThreadReferenceUntilLoopFinishes() throws Exception {
        TestLoopThread loopThread = new TestLoopThread();

        loopThread.start();
        assertTrue(loopThread.awaitEntered());

        Thread runningThread = loopThread.thread;
        loopThread.shutdown();

        assertSame(runningThread, loopThread.thread);
        assertFalse(loopThread.isShutdown());

        loopThread.release();
        assertTrue(loopThread.awaitFinished());
        assertNull(loopThread.thread);
        assertTrue(loopThread.isShutdown());
    }

    @Test
    public void startShouldNotRestartUntilPreviousRunFinishes() throws Exception {
        TestLoopThread loopThread = new TestLoopThread();

        loopThread.start();
        assertTrue(loopThread.awaitEntered());
        Thread firstThread = loopThread.thread;

        loopThread.shutdown();
        loopThread.start();

        assertSame(firstThread, loopThread.thread);
        assertEquals(1, loopThread.getBeforeLoopCount());

        loopThread.release();
        assertTrue(loopThread.awaitFinished());

        loopThread.prepareNextRun();
        loopThread.start();
        assertTrue(loopThread.awaitEntered());

        Thread secondThread = loopThread.thread;
        assertNotSame(firstThread, secondThread);
        assertEquals(2, loopThread.getBeforeLoopCount());

        loopThread.release();
        assertTrue(loopThread.awaitFinished());
    }

    private static final class TestLoopThread extends AbsLoopThread {
        private final AtomicInteger beforeLoopCount = new AtomicInteger();
        private volatile CountDownLatch enteredLatch = new CountDownLatch(1);
        private volatile CountDownLatch releaseLatch = new CountDownLatch(1);
        private volatile CountDownLatch finishedLatch = new CountDownLatch(1);

        @Override
        protected void beforeLoop() {
            beforeLoopCount.incrementAndGet();
        }

        @Override
        protected void runInLoopThread() {
            enteredLatch.countDown();
            while (releaseLatch.getCount() > 0) {
                try {
                    Thread.sleep(10L);
                } catch (InterruptedException ignored) {
                }
            }
            shutdown();
        }

        @Override
        protected void loopFinish(Exception e) {
            finishedLatch.countDown();
        }

        boolean awaitEntered() throws InterruptedException {
            return enteredLatch.await(5, TimeUnit.SECONDS);
        }

        boolean awaitFinished() throws InterruptedException {
            return finishedLatch.await(5, TimeUnit.SECONDS);
        }

        void release() {
            releaseLatch.countDown();
        }

        void prepareNextRun() {
            enteredLatch = new CountDownLatch(1);
            releaseLatch = new CountDownLatch(1);
            finishedLatch = new CountDownLatch(1);
        }

        int getBeforeLoopCount() {
            return beforeLoopCount.get();
        }
    }
}
