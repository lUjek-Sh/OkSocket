package com.xuhao.didi.socket.client.sdk.client.connection;


import com.xuhao.didi.core.utils.SLog;
import com.xuhao.didi.socket.client.impl.exceptions.ManuallyDisconnectException;
import com.xuhao.didi.socket.client.sdk.client.ConnectionInfo;
import com.xuhao.didi.socket.client.sdk.client.OkSocketOptions;
import com.xuhao.didi.socket.common.interfaces.basic.AbsLoopThread;

import java.util.Iterator;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by xuhao on 2017/6/5.
 */

public class DefaultReconnectManager extends AbsReconnectionManager {

    /**
     * 最大连接失败次数,不包括断开异常
     */
    /**
     * 连接失败次数,不包括断开异常
     */
    private int mConnectionFailedTimes = 0;

    private volatile ReconnectTestingThread mReconnectTestingThread;

    public DefaultReconnectManager() {
        mReconnectTestingThread = new ReconnectTestingThread();
    }

    @Override
    public void onSocketDisconnection(ConnectionInfo info, String action, Exception e) {
        if (isNeedReconnect(e)) {//break with exception
            reconnectDelay();
        } else {
            resetThread();
        }
    }

    @Override
    public void onSocketConnectionSuccess(ConnectionInfo info, String action) {
        mConnectionFailedTimes = 0;
        resetThread();
    }

    @Override
    public void onSocketConnectionFailed(ConnectionInfo info, String action, Exception e) {
        if (e != null) {
            mConnectionFailedTimes++;
            if (shouldSwitchToBackup(mConnectionManager.getOption(), mConnectionFailedTimes)) {
                resetThread();
                //连接失败达到阈值,需要切换备用线路.
                ConnectionInfo originInfo = mConnectionManager.getRemoteConnectionInfo();
                ConnectionInfo backupInfo = originInfo.getBackupInfo();
                if (backupInfo != null) {
                    ConnectionInfo bbInfo = new ConnectionInfo(originInfo.getIp(), originInfo.getPort());
                    backupInfo.setBackupInfo(bbInfo);
                    if (!mConnectionManager.isConnect()) {
                        SLog.i("Prepare switch to the backup line " + backupInfo.getIp() + ":" + backupInfo.getPort() + " ...");
                        synchronized (mConnectionManager) {
                            mConnectionManager.switchConnectionInfo(backupInfo);
                        }
                        reconnectDelay();
                    }
                } else {
                    reconnectDelay();
                }
            } else {
                reconnectDelay();
            }
        }
    }

    /**
     * 是否需要重连
     *
     * @param e
     * @return
     */
    private boolean isNeedReconnect(Exception e) {
        synchronized (mIgnoreDisconnectExceptionList) {
            if (e != null && !(e instanceof ManuallyDisconnectException)) {//break with exception
                Iterator<Class<? extends Exception>> it = mIgnoreDisconnectExceptionList.iterator();
                while (it.hasNext()) {
                    Class<? extends Exception> classException = it.next();
                    if (classException.isAssignableFrom(e.getClass())) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }
    }


    /**
     * 重置重连线程,关闭线程
     */
    private synchronized void resetThread() {
        if (mReconnectTestingThread != null) {
            mReconnectTestingThread.shutdown();
        }
    }

    /**
     * 开始延迟重连
     */
    private void reconnectDelay() {
        synchronized (mReconnectTestingThread) {
            if (mReconnectTestingThread.isShutdown()) {
                mReconnectTestingThread.start();
            }
        }
    }

    @Override
    public void detach() {
        resetThread();
        super.detach();
    }

    static long resolveReconnectDelayMillis(OkSocketOptions options, long attemptCount) {
        OkSocketOptions reconnectOptions = options == null ? OkSocketOptions.getDefault() : options;
        long connectTimeoutDelay = Math.max(0L, reconnectOptions.getConnectTimeoutSecond()) * 1000L;
        long maxConfiguredDelay = Math.max(reconnectOptions.getReconnectMaxDelayMillis(), connectTimeoutDelay);

        double scaledDelay = reconnectOptions.getReconnectDelayMillis() *
                Math.pow(reconnectOptions.getReconnectDelayScale(), Math.max(0L, attemptCount));
        long configuredDelay = scaledDelay >= Long.MAX_VALUE ? Long.MAX_VALUE : Math.round(scaledDelay);
        long boundedDelay = Math.min(configuredDelay, maxConfiguredDelay);
        long baseDelay = Math.max(boundedDelay, connectTimeoutDelay);

        double jitterRatio = reconnectOptions.getReconnectJitterRatio();
        if (jitterRatio <= 0.0d) {
            return baseDelay;
        }

        double factor = 1.0d + (ThreadLocalRandom.current().nextDouble() * 2.0d - 1.0d) * jitterRatio;
        long jitteredDelay = Math.round(baseDelay * factor);
        if (jitteredDelay < connectTimeoutDelay) {
            return connectTimeoutDelay;
        }
        if (jitteredDelay > maxConfiguredDelay) {
            return maxConfiguredDelay;
        }
        return jitteredDelay;
    }

    static int resolveBackupSwitchThreshold(OkSocketOptions options) {
        OkSocketOptions reconnectOptions = options == null ? OkSocketOptions.getDefault() : options;
        return reconnectOptions.getReconnectBackupSwitchThreshold();
    }

    static boolean shouldSwitchToBackup(OkSocketOptions options, int connectionFailedTimes) {
        return connectionFailedTimes > resolveBackupSwitchThreshold(options);
    }

    private class ReconnectTestingThread extends AbsLoopThread {
        /**
         * 延时连接时间
         */
        @Override
        protected void runInLoopThread() throws Exception {
            if (mDetach) {
                SLog.i("ReconnectionManager already detached by framework.We decide gave up this reconnection mission!");
                shutdown();
                return;
            }

            //延迟执行
            long reconnectDelayMillis = resolveReconnectDelayMillis(mConnectionManager.getOption(), getLoopTimes());
            SLog.i("Reconnect after " + reconnectDelayMillis + " mills ...");
            if (!sleepReconnectDelay(reconnectDelayMillis)) {
                return;
            }

            if (mDetach) {
                SLog.i("ReconnectionManager already detached by framework.We decide gave up this reconnection mission!");
                shutdown();
                return;
            }

            if (mConnectionManager.isConnect()) {
                shutdown();
                return;
            }
            boolean isHolden = mConnectionManager.getOption().isConnectionHolden();

            if (!isHolden) {
                detach();
                shutdown();
                return;
            }
            ConnectionInfo info = mConnectionManager.getRemoteConnectionInfo();
            SLog.i("Reconnect the server " + info.getIp() + ":" + info.getPort() + " ...");
            synchronized (mConnectionManager) {
                if (!mConnectionManager.isConnect()) {
                    mConnectionManager.connect();
                } else {
                    shutdown();
                }
            }
        }


        @Override
        protected void loopFinish(Exception e) {
        }

        private boolean sleepReconnectDelay(long reconnectDelayMillis) {
            try {
                Thread.sleep(reconnectDelayMillis);
                return true;
            } catch (InterruptedException ignored) {
                return false;
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        return true;
    }

}
