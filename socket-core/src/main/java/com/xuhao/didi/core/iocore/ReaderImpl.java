package com.xuhao.didi.core.iocore;

import com.xuhao.didi.core.exceptions.ReadException;
import com.xuhao.didi.core.iocore.interfaces.IOAction;
import com.xuhao.didi.core.pojo.OriginalData;
import com.xuhao.didi.core.protocol.IReaderProtocol;
import com.xuhao.didi.core.utils.BytesUtils;
import com.xuhao.didi.core.utils.SLog;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by xuhao on 2017/5/31.
 */

public class ReaderImpl extends AbsReader {
    private static final long SLOW_READ_LOG_THRESHOLD_MILLIS = 1000L;

    private final AtomicLong mReadSequence = new AtomicLong();

    @Override
    public void read() throws RuntimeException {
        OriginalData originalData = new OriginalData();
        IReaderProtocol headerProtocol = mOkOptions.getReaderProtocol();
        int headerLength = headerProtocol.getHeaderLength();
        try {
            originalData.setReadSequence(mReadSequence.incrementAndGet());
            originalData.setReadStartedAtEpochMillis(System.currentTimeMillis());
            originalData.setReadStartNanoTime(System.nanoTime());

            byte[] headBytes = new byte[headerLength];
            ReadProgress headerProgress = readFully(headBytes, 0, headBytes.length, originalData.getReadStartNanoTime());
            originalData.setHeadBytes(headBytes);
            originalData.setReadHeaderFirstByteNanoTime(headerProgress.firstByteNanoTime);
            originalData.setReadHeaderCompleteNanoTime(headerProgress.completedNanoTime);
            originalData.setReadHeaderChunkCount(headerProgress.chunkCount);
            if (SLog.isDebug()) {
                SLog.i("read head: " + BytesUtils.toHexStringForLog(headBytes));
            }
            int bodyLength = headerProtocol.getBodyLength(originalData.getHeadBytes(), mOkOptions.getReadByteOrder());
            if (SLog.isDebug()) {
                SLog.i("need read body length: " + bodyLength);
            }
            if (bodyLength > 0) {
                if (bodyLength > mOkOptions.getMaxReadDataMB() * 1024 * 1024) {
                    throw new ReadException("Need to follow the transmission protocol.\r\n" +
                            "Please check the client/server code.\r\n" +
                            "According to the packet header data in the transport protocol, the package length is " + bodyLength + " Bytes.\r\n" +
                            "You need check your <ReaderProtocol> definition");
                }
                byte[] bodyBytes = new byte[bodyLength];
                ReadProgress bodyProgress = readBodyFromChannel(bodyBytes, originalData.getReadHeaderCompleteNanoTime());
                originalData.setBodyBytes(bodyBytes);
                originalData.setReadBodyFirstByteNanoTime(bodyProgress.firstByteNanoTime);
                originalData.setReadBodyCompleteNanoTime(bodyProgress.completedNanoTime);
                originalData.setReadBodyChunkCount(bodyProgress.chunkCount);
            } else if (bodyLength == 0) {
                originalData.setBodyBytes(new byte[0]);
                originalData.setReadBodyFirstByteNanoTime(originalData.getReadHeaderCompleteNanoTime());
                originalData.setReadBodyCompleteNanoTime(originalData.getReadHeaderCompleteNanoTime());
                originalData.setReadBodyChunkCount(0);
            } else if (bodyLength < 0) {
                throw new ReadException(
                        "read body is wrong,this socket input stream is end of file read " + bodyLength + " ,that mean this socket is disconnected by server");
            }
            long readCompletedNanoTime = System.nanoTime();
            originalData.setReadCompleteNanoTime(readCompletedNanoTime);
            originalData.setReadCompletedAtEpochMillis(System.currentTimeMillis());
            logSlowReadIfNeeded(originalData);
            mStateSender.sendBroadcast(IOAction.ACTION_READ_COMPLETE, originalData);
        } catch (ReadException e) {
            throw e;
        } catch (Exception e) {
            throw new ReadException(e);
        }
    }

    private ReadProgress readFully(byte[] target, int offset, int length, long startNanoTime) throws IOException {
        ReadProgress progress = new ReadProgress(startNanoTime);
        int totalRead = 0;
        while (totalRead < length) {
            int readSize = mInputStream.read(target, offset + totalRead, length - totalRead);
            if (readSize == -1) {
                throw new ReadException(
                        "socket input stream reached end of file while reading " + length + " bytes");
            }
            progress.onBytesRead(readSize);
            totalRead += readSize;
        }
        return progress;
    }

    private ReadProgress readBodyFromChannel(byte[] bodyBytes, long startNanoTime) throws IOException {
        int readPackageBytes = mOkOptions.getReadPackageBytes();
        if (readPackageBytes <= 0) {
            throw new ReadException("read package bytes must be greater than 0");
        }

        ReadProgress progress = new ReadProgress(startNanoTime);
        int offset = 0;
        while (offset < bodyBytes.length) {
            int readSize = Math.min(readPackageBytes, bodyBytes.length - offset);
            ReadProgress chunkProgress = readFully(bodyBytes, offset, readSize, progress.resolveChunkStartNanoTime());
            progress.merge(chunkProgress);
            offset += readSize;
        }
        if (SLog.isDebug()) {
            SLog.i("read total bytes: " + BytesUtils.toHexStringForLog(bodyBytes));
            SLog.i("read total length:" + bodyBytes.length);
        }
        return progress;
    }

    private void logSlowReadIfNeeded(OriginalData originalData) {
        if (!SLog.isDebug()) {
            return;
        }
        if (originalData.getReadDurationMillis() < SLOW_READ_LOG_THRESHOLD_MILLIS) {
            return;
        }
        SLog.w("socket read slow: " + originalData.getReadDiagnosticsSummary());
    }

    private static final class ReadProgress {
        private final long startNanoTime;
        private long firstByteNanoTime;
        private long completedNanoTime;
        private int chunkCount;

        private ReadProgress(long startNanoTime) {
            this.startNanoTime = startNanoTime;
            this.completedNanoTime = startNanoTime;
        }

        private void onBytesRead(int readSize) {
            if (readSize <= 0) {
                return;
            }
            long now = System.nanoTime();
            if (chunkCount == 0) {
                firstByteNanoTime = now;
            }
            completedNanoTime = now;
            chunkCount++;
        }

        private long resolveChunkStartNanoTime() {
            return chunkCount == 0 ? startNanoTime : completedNanoTime;
        }

        private void merge(ReadProgress other) {
            if (other == null || other.chunkCount == 0) {
                return;
            }
            if (chunkCount == 0) {
                firstByteNanoTime = other.firstByteNanoTime;
            }
            completedNanoTime = other.completedNanoTime;
            chunkCount += other.chunkCount;
        }
    }

}
