package com.xuhao.didi.core.pojo;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * 原始数据结构体
 * Created by xuhao on 2017/5/16.
 */
public final class OriginalData implements Serializable {
    /**
     * 原始数据包头字节数组
     */
    private byte[] mHeadBytes;
    /**
     * 原始数据包体字节数组
     */
    private byte[] mBodyBytes;
    private long mReadSequence;
    private long mReadStartedAtEpochMillis;
    private long mReadCompletedAtEpochMillis;
    private long mReadStartNanoTime;
    private long mReadHeaderFirstByteNanoTime;
    private long mReadHeaderCompleteNanoTime;
    private int mReadHeaderChunkCount;
    private long mReadBodyFirstByteNanoTime;
    private long mReadBodyCompleteNanoTime;
    private int mReadBodyChunkCount;
    private long mReadCompleteNanoTime;

    public byte[] getHeadBytes() {
        return mHeadBytes;
    }

    public void setHeadBytes(byte[] headBytes) {
        mHeadBytes = headBytes;
    }

    public byte[] getBodyBytes() {
        return mBodyBytes;
    }

    public void setBodyBytes(byte[] bodyBytes) {
        mBodyBytes = bodyBytes;
    }

    public long getReadSequence() {
        return mReadSequence;
    }

    public void setReadSequence(long readSequence) {
        mReadSequence = readSequence;
    }

    public long getReadStartedAtEpochMillis() {
        return mReadStartedAtEpochMillis;
    }

    public void setReadStartedAtEpochMillis(long readStartedAtEpochMillis) {
        mReadStartedAtEpochMillis = readStartedAtEpochMillis;
    }

    public long getReadCompletedAtEpochMillis() {
        return mReadCompletedAtEpochMillis;
    }

    public void setReadCompletedAtEpochMillis(long readCompletedAtEpochMillis) {
        mReadCompletedAtEpochMillis = readCompletedAtEpochMillis;
    }

    public long getReadStartNanoTime() {
        return mReadStartNanoTime;
    }

    public void setReadStartNanoTime(long readStartNanoTime) {
        mReadStartNanoTime = readStartNanoTime;
    }

    public long getReadHeaderCompleteNanoTime() {
        return mReadHeaderCompleteNanoTime;
    }

    public long getReadHeaderFirstByteNanoTime() {
        return mReadHeaderFirstByteNanoTime;
    }

    public void setReadHeaderFirstByteNanoTime(long readHeaderFirstByteNanoTime) {
        mReadHeaderFirstByteNanoTime = readHeaderFirstByteNanoTime;
    }

    public void setReadHeaderCompleteNanoTime(long readHeaderCompleteNanoTime) {
        mReadHeaderCompleteNanoTime = readHeaderCompleteNanoTime;
    }

    public long getReadBodyCompleteNanoTime() {
        return mReadBodyCompleteNanoTime;
    }

    public long getReadBodyFirstByteNanoTime() {
        return mReadBodyFirstByteNanoTime;
    }

    public void setReadBodyFirstByteNanoTime(long readBodyFirstByteNanoTime) {
        mReadBodyFirstByteNanoTime = readBodyFirstByteNanoTime;
    }

    public void setReadBodyCompleteNanoTime(long readBodyCompleteNanoTime) {
        mReadBodyCompleteNanoTime = readBodyCompleteNanoTime;
    }

    public int getReadHeaderChunkCount() {
        return mReadHeaderChunkCount;
    }

    public void setReadHeaderChunkCount(int readHeaderChunkCount) {
        mReadHeaderChunkCount = readHeaderChunkCount;
    }

    public int getReadBodyChunkCount() {
        return mReadBodyChunkCount;
    }

    public void setReadBodyChunkCount(int readBodyChunkCount) {
        mReadBodyChunkCount = readBodyChunkCount;
    }

    public long getReadCompleteNanoTime() {
        return mReadCompleteNanoTime;
    }

    public void setReadCompleteNanoTime(long readCompleteNanoTime) {
        mReadCompleteNanoTime = readCompleteNanoTime;
    }

    public long getReadHeaderDurationMillis() {
        return nanosToMillis(mReadHeaderCompleteNanoTime - mReadStartNanoTime);
    }

    public long getReadHeaderFirstByteWaitMillis() {
        return nanosToMillis(mReadHeaderFirstByteNanoTime - mReadStartNanoTime);
    }

    public long getReadHeaderAssemblyDurationMillis() {
        return nanosToMillis(mReadHeaderCompleteNanoTime - mReadHeaderFirstByteNanoTime);
    }

    public long getReadBodyDurationMillis() {
        return nanosToMillis(mReadBodyCompleteNanoTime - mReadHeaderCompleteNanoTime);
    }

    public long getReadBodyFirstByteWaitMillis() {
        return nanosToMillis(mReadBodyFirstByteNanoTime - mReadHeaderCompleteNanoTime);
    }

    public long getReadBodyAssemblyDurationMillis() {
        return nanosToMillis(mReadBodyCompleteNanoTime - mReadBodyFirstByteNanoTime);
    }

    public long getReadDurationMillis() {
        return nanosToMillis(mReadCompleteNanoTime - mReadStartNanoTime);
    }

    public String getReadDelayHint() {
        long headerDurationNanos = positiveDiff(mReadHeaderCompleteNanoTime, mReadStartNanoTime);
        long bodyDurationNanos = positiveDiff(mReadBodyCompleteNanoTime, mReadHeaderCompleteNanoTime);
        if (bodyDurationNanos > headerDurationNanos) {
            return buildBodyDelayHint(bodyDurationNanos);
        }
        if (headerDurationNanos > 0L) {
            return buildHeaderDelayHint(headerDurationNanos);
        }
        if (bodyDurationNanos > 0L) {
            return buildBodyDelayHint(bodyDurationNanos);
        }
        return "none";
    }

    public String getReadDiagnosticsSummary() {
        return "seq=" + mReadSequence
                + ", startedAt=" + mReadStartedAtEpochMillis
                + ", completedAt=" + mReadCompletedAtEpochMillis
                + ", headerLength=" + safeArrayLength(mHeadBytes)
                + ", bodyLength=" + safeArrayLength(mBodyBytes)
                + ", headerMs=" + getReadHeaderDurationMillis()
                + ", bodyMs=" + getReadBodyDurationMillis()
                + ", totalMs=" + getReadDurationMillis()
                + ", headerWaitFirstByteMs=" + getReadHeaderFirstByteWaitMillis()
                + ", headerAssembleMs=" + getReadHeaderAssemblyDurationMillis()
                + ", headerChunks=" + mReadHeaderChunkCount
                + ", bodyWaitFirstByteMs=" + getReadBodyFirstByteWaitMillis()
                + ", bodyAssembleMs=" + getReadBodyAssemblyDurationMillis()
                + ", bodyChunks=" + mReadBodyChunkCount
                + ", hint=" + getReadDelayHint();
    }

    private long nanosToMillis(long nanos) {
        if (nanos <= 0L) {
            return 0L;
        }
        return TimeUnit.NANOSECONDS.toMillis(nanos);
    }

    private long positiveDiff(long endNanos, long startNanos) {
        if (endNanos <= startNanos) {
            return 0L;
        }
        return endNanos - startNanos;
    }

    private String buildHeaderDelayHint(long headerDurationNanos) {
        if (headerDurationNanos <= 0L) {
            return "none";
        }
        if (mReadHeaderChunkCount <= 1) {
            return "waiting_for_header_bytes";
        }
        return "header_fragmentation_or_partial_delivery";
    }

    private String buildBodyDelayHint(long bodyDurationNanos) {
        if (bodyDurationNanos <= 0L) {
            return "none";
        }
        if (mReadBodyChunkCount <= 1) {
            return "waiting_for_body_bytes";
        }
        return "body_fragmentation_or_partial_delivery";
    }

    private int safeArrayLength(byte[] bytes) {
        return bytes == null ? 0 : bytes.length;
    }
}
