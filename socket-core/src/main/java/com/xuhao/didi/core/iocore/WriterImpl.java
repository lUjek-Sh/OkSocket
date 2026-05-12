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

/**
 * Created by xuhao on 2017/5/31.
 */
public class WriterImpl implements IWriter<IIOCoreOptions> {
    private static final int DEFAULT_QUEUE_CAPACITY = 256;

    private volatile IIOCoreOptions mOkOptions;
    private final Object mQueueLock = new Object();
    private IStateSender mStateSender;
    private OutputStream mOutputStream;
    private final LinkedBlockingQueue<ISendable> mQueue = new LinkedBlockingQueue<>();
    private volatile int mQueueCapacity = DEFAULT_QUEUE_CAPACITY;

    @Override
    public void initialize(OutputStream outputStream, IStateSender stateSender) {
        mStateSender = stateSender;
        mOutputStream = outputStream;
    }

    @Override
    public boolean write() throws RuntimeException {
        ISendable sendable = null;
        try {
            sendable = mQueue.take();
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }

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
        mQueueCapacity = option == null ? DEFAULT_QUEUE_CAPACITY : option.getWritePackageQueueCapacity();
    }

    @Override
    public void offer(ISendable sendable) {
        if (sendable == null) {
            return;
        }
        synchronized (mQueueLock) {
            if (mQueue.size() >= mQueueCapacity) {
                throw new WriteException("write queue is full, capacity=" + mQueueCapacity);
            }
            mQueue.offer(sendable);
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
}
