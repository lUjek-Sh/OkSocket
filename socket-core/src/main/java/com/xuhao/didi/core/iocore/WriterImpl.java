package com.xuhao.didi.core.iocore;

import com.xuhao.didi.core.exceptions.WriteException;
import com.xuhao.didi.core.iocore.interfaces.IIOCoreOptions;
import com.xuhao.didi.core.iocore.interfaces.IOAction;
import com.xuhao.didi.core.iocore.interfaces.IPulseSendable;
import com.xuhao.didi.core.iocore.interfaces.ISendable;
import com.xuhao.didi.core.iocore.interfaces.IStateSender;
import com.xuhao.didi.core.iocore.interfaces.IWriter;
import com.xuhao.didi.core.utils.BytesUtils;
import com.xuhao.didi.core.utils.SLog;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by xuhao on 2017/5/31.
 */
public class WriterImpl implements IWriter<IIOCoreOptions> {
    private static final int DEFAULT_QUEUE_CAPACITY = 256;
    private static final long QUEUE_POLL_TIMEOUT_MILLIS = 100L;

    private volatile IIOCoreOptions mOkOptions;
    private final Object mQueueLock = new Object();
    private IStateSender mStateSender;
    private OutputStream mOutputStream;
    private volatile LinkedBlockingQueue<ISendable> mQueue =
            new LinkedBlockingQueue<>(DEFAULT_QUEUE_CAPACITY);
    private volatile int mQueueCapacity = DEFAULT_QUEUE_CAPACITY;

    @Override
    public void initialize(OutputStream outputStream, IStateSender stateSender) {
        mStateSender = stateSender;
        mOutputStream = outputStream;
    }

    @Override
    public boolean write() throws RuntimeException {
        ISendable sendable = takeNextSendable();
        if (sendable == null) {
            return false;
        }

        try {
            byte[] sendBytes = sendable.parse();
            int packageSize = mOkOptions.getWritePackageBytes();
            if (packageSize <= 0) {
                throw new WriteException("write package bytes must be greater than 0");
            }

            int remainingCount = sendBytes.length;
            int index = 0;
            while (remainingCount > 0) {
                int realWriteLength = Math.min(packageSize, remainingCount);
                mOutputStream.write(sendBytes, index, realWriteLength);

                if (SLog.isDebug()) {
                    byte[] forLogBytes = Arrays.copyOfRange(sendBytes, index, index + realWriteLength);
                    SLog.i("write bytes: " + BytesUtils.toHexStringForLog(forLogBytes));
                    SLog.i("bytes write length:" + realWriteLength);
                }

                index += realWriteLength;
                remainingCount -= realWriteLength;
            }

            mOutputStream.flush();
            if (sendable instanceof IPulseSendable) {
                mStateSender.sendBroadcast(IOAction.ACTION_PULSE_REQUEST, sendable);
            } else {
                mStateSender.sendBroadcast(IOAction.ACTION_WRITE_COMPLETE, sendable);
            }
            return true;
        } catch (WriteException e) {
            throw e;
        } catch (Exception e) {
            throw new WriteException(e);
        }
    }

    @Override
    public void setOption(IIOCoreOptions option) {
        mOkOptions = option;
        int newCapacity = option == null ? DEFAULT_QUEUE_CAPACITY : option.getWritePackageQueueCapacity();
        synchronized (mQueueLock) {
            if (newCapacity == mQueueCapacity) {
                return;
            }
            if (mQueue.size() > newCapacity) {
                throw new IllegalStateException(
                        "Cannot shrink write queue below pending message count: pending="
                                + mQueue.size() + ", capacity=" + newCapacity);
            }
            LinkedBlockingQueue<ISendable> newQueue = new LinkedBlockingQueue<>(newCapacity);
            mQueue.drainTo(newQueue);
            mQueue = newQueue;
            mQueueCapacity = newCapacity;
        }
    }

    @Override
    public void offer(ISendable sendable) {
        if (sendable == null) {
            return;
        }
        synchronized (mQueueLock) {
            if (!mQueue.offer(sendable)) {
                throw new WriteException("write queue is full, capacity=" + mQueueCapacity);
            }
        }
    }

    @Override
    public void close() {
        mQueue.clear();
        if (mOutputStream != null) {
            try {
                mOutputStream.close();
            } catch (IOException ignored) {
            }
        }
    }

    private ISendable takeNextSendable() {
        while (!Thread.currentThread().isInterrupted()) {
            LinkedBlockingQueue<ISendable> queue = mQueue;
            try {
                ISendable sendable = queue.poll(QUEUE_POLL_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
                if (sendable != null) {
                    return sendable;
                }
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return null;
    }
}
