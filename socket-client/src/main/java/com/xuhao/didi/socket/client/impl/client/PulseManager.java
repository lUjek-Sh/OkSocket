package com.xuhao.didi.socket.client.impl.client;

import com.xuhao.didi.core.iocore.interfaces.IPulseSendable;
import com.xuhao.didi.socket.client.impl.exceptions.DogDeadException;
import com.xuhao.didi.socket.client.sdk.client.OkSocketOptions;
import com.xuhao.didi.socket.client.sdk.client.bean.IPulse;
import com.xuhao.didi.socket.client.sdk.client.connection.IConnectionManager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by xuhao on 2017/5/18.
 */
public class PulseManager implements IPulse {
    static final long MIN_PULSE_FREQUENCY_MILLIS = 1000L;

    private final Object mScheduleLock = new Object();
    private final AtomicInteger mLoseTimes = new AtomicInteger(-1);
    private final ScheduledExecutorService mPulseExecutor = Executors.newSingleThreadScheduledExecutor(new PulseThreadFactory());

    private volatile IConnectionManager mManager;
    private volatile OkSocketOptions mOkOptions;
    private volatile IPulseSendable mSendable;
    private volatile ScheduledFuture<?> mPulseFuture;
    private volatile long mCurrentFrequency;
    private volatile OkSocketOptions.IOThreadMode mCurrentThreadMode;
    private volatile boolean isDead = false;

    PulseManager(IConnectionManager manager, OkSocketOptions okOptions) {
        mManager = manager;
        mOkOptions = okOptions;
        mCurrentThreadMode = mOkOptions.getIOThreadMode();
        mCurrentFrequency = normalizePulseFrequency(mOkOptions.getPulseFrequency());
    }

    public synchronized IPulse setPulseSendable(IPulseSendable sendable) {
        if (sendable != null) {
            mSendable = sendable;
        }
        return this;
    }

    public IPulseSendable getPulseSendable() {
        return mSendable;
    }

    @Override
    public synchronized void pulse() {
        if (isDead) {
            return;
        }
        updateFrequency();
        if (mCurrentThreadMode == OkSocketOptions.IOThreadMode.SIMPLEX) {
            cancelPulseSchedule();
            return;
        }
        reschedulePulse();
    }

    @Override
    public synchronized void trigger() {
        if (isDead) {
            return;
        }
        if (mCurrentThreadMode != OkSocketOptions.IOThreadMode.SIMPLEX && mManager != null && mSendable != null) {
            mManager.send(mSendable);
        }
    }

    public synchronized void dead() {
        if (isDead) {
            return;
        }
        mLoseTimes.set(0);
        isDead = true;
        cancelPulseSchedule();
        mPulseExecutor.shutdownNow();
    }

    private synchronized void updateFrequency() {
        if (mCurrentThreadMode != OkSocketOptions.IOThreadMode.SIMPLEX) {
            mCurrentFrequency = normalizePulseFrequency(mOkOptions.getPulseFrequency());
        } else {
            cancelPulseSchedule();
        }
    }

    @Override
    public synchronized void feed() {
        mLoseTimes.set(-1);
    }

    public int getLoseTimes() {
        return mLoseTimes.get();
    }

    protected synchronized void setOkOptions(OkSocketOptions okOptions) {
        mOkOptions = okOptions;
        mCurrentThreadMode = mOkOptions.getIOThreadMode();
        updateFrequency();
        if (isDead) {
            return;
        }
        if (mCurrentThreadMode == OkSocketOptions.IOThreadMode.SIMPLEX) {
            cancelPulseSchedule();
        } else if (hasScheduledPulse()) {
            reschedulePulse();
        }
    }

    static long normalizePulseFrequency(long pulseFrequency) {
        return pulseFrequency < MIN_PULSE_FREQUENCY_MILLIS ? MIN_PULSE_FREQUENCY_MILLIS : pulseFrequency;
    }

    private boolean hasScheduledPulse() {
        ScheduledFuture<?> pulseFuture = mPulseFuture;
        return pulseFuture != null && !pulseFuture.isCancelled() && !pulseFuture.isDone();
    }

    private void reschedulePulse() {
        synchronized (mScheduleLock) {
            cancelPulseScheduleLocked();
            if (isDead || mCurrentThreadMode == OkSocketOptions.IOThreadMode.SIMPLEX || mPulseExecutor.isShutdown()) {
                return;
            }
            mPulseFuture = mPulseExecutor.scheduleWithFixedDelay(new PulseTask(), 0L, mCurrentFrequency, TimeUnit.MILLISECONDS);
        }
    }

    private void cancelPulseSchedule() {
        synchronized (mScheduleLock) {
            cancelPulseScheduleLocked();
        }
    }

    private void cancelPulseScheduleLocked() {
        ScheduledFuture<?> pulseFuture = mPulseFuture;
        if (pulseFuture != null) {
            pulseFuture.cancel(true);
            mPulseFuture = null;
        }
    }

    private void handlePulseTick() {
        if (isDead) {
            cancelPulseSchedule();
            return;
        }
        if (mManager == null || mSendable == null) {
            return;
        }
        int pulseFeedLoseTimes = mOkOptions.getPulseFeedLoseTimes();
        if (pulseFeedLoseTimes != -1 && mLoseTimes.incrementAndGet() >= pulseFeedLoseTimes) {
            cancelPulseSchedule();
            mManager.disconnect(new DogDeadException("you need feed dog on time,otherwise he will die"));
            return;
        }
        mManager.send(mSendable);
    }

    private final class PulseTask implements Runnable {
        @Override
        public void run() {
            handlePulseTick();
        }
    }

    private static final class PulseThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "client_pulse_thread");
            thread.setDaemon(true);
            return thread;
        }
    }
}
