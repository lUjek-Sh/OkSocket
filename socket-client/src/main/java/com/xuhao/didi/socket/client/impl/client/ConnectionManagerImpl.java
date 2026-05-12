package com.xuhao.didi.socket.client.impl.client;

import com.xuhao.didi.core.iocore.interfaces.ISendable;
import com.xuhao.didi.core.utils.SLog;
import com.xuhao.didi.socket.client.impl.client.action.ActionHandler;
import com.xuhao.didi.socket.client.impl.client.iothreads.IOThreadManager;
import com.xuhao.didi.socket.client.impl.exceptions.ManuallyDisconnectException;
import com.xuhao.didi.socket.client.impl.exceptions.UnConnectException;
import com.xuhao.didi.socket.client.sdk.client.ConnectionInfo;
import com.xuhao.didi.socket.client.sdk.client.OkSocketOptions;
import com.xuhao.didi.socket.client.sdk.client.OkSocketSSLConfig;
import com.xuhao.didi.socket.client.sdk.client.action.IAction;
import com.xuhao.didi.socket.client.sdk.client.connection.AbsReconnectionManager;
import com.xuhao.didi.socket.client.sdk.client.connection.IConnectionManager;
import com.xuhao.didi.socket.common.interfaces.common_interfacies.IIOManager;
import com.xuhao.didi.socket.common.interfaces.utils.TextUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

/**
 * Created by xuhao on 2017/5/16.
 */
public class ConnectionManagerImpl extends AbsConnectionManager {
    static final String DEFAULT_SSL_PROTOCOL = "TLS";

    private volatile Socket mSocket;
    private volatile OkSocketOptions mOptions;
    private IIOManager mManager;
    private Thread mConnectThread;
    private ActionHandler mActionHandler;
    private volatile PulseManager mPulseManager;
    private volatile AbsReconnectionManager mReconnectionManager;
    private final AtomicReference<ConnectionState> mConnectionState =
            new AtomicReference<>(ConnectionState.IDLE);
    private volatile Exception mDisconnectCause;

    protected ConnectionManagerImpl(ConnectionInfo info) {
        this(info, null);
    }

    public ConnectionManagerImpl(ConnectionInfo remoteInfo, ConnectionInfo localInfo) {
        super(remoteInfo, localInfo);
        String ip = "";
        String port = "";
        if (remoteInfo != null) {
            ip = remoteInfo.getIp();
            port = remoteInfo.getPort() + "";
        }
        SLog.i("block connection init with:" + ip + ":" + port);

        if (localInfo != null) {
            SLog.i("binding local addr:" + localInfo.getIp() + " port:" + localInfo.getPort());
        }
    }

    @Override
    public synchronized void connect() {
        SLog.i("Thread name:" + Thread.currentThread().getName() + " id:" + Thread.currentThread().getId());

        ConnectionState currentState = mConnectionState.get();
        if (currentState == ConnectionState.CONNECTING || currentState == ConnectionState.DISCONNECTING) {
            return;
        }
        if (currentState == ConnectionState.CONNECTED && isConnect()) {
            return;
        }

        mDisconnectCause = null;
        if (mRemoteConnectionInfo == null) {
            mConnectionState.set(ConnectionState.DISCONNECTED);
            throw new UnConnectException("连接参数为空,检查连接参数");
        }

        if (mActionHandler != null) {
            mActionHandler.detach(this);
            SLog.i("mActionHandler is detached.");
        }
        mActionHandler = new ActionHandler();
        mActionHandler.attach(this, this);
        SLog.i("mActionHandler is attached.");

        AbsReconnectionManager configuredReconnectionManager = mOptions.getReconnectionManager();
        if (mReconnectionManager != null && mReconnectionManager != configuredReconnectionManager) {
            mReconnectionManager.detach();
            SLog.i("ReconnectionManager is detached.");
        }
        mReconnectionManager = configuredReconnectionManager;
        if (mReconnectionManager != null) {
            mReconnectionManager.attach(this);
            SLog.i("ReconnectionManager is attached.");
        }

        mConnectionState.set(ConnectionState.CONNECTING);
        String info = mRemoteConnectionInfo.getIp() + ":" + mRemoteConnectionInfo.getPort();
        mConnectThread = new ConnectionThread(" Connect thread for " + info);
        mConnectThread.setDaemon(true);
        mConnectThread.start();
    }

    private synchronized Socket getSocketByConfig() throws Exception {
        if (mOptions.getOkSocketFactory() != null) {
            return mOptions.getOkSocketFactory().createSocket(mRemoteConnectionInfo, mOptions);
        }

        OkSocketSSLConfig config = mOptions.getSSLConfig();
        if (config == null) {
            return new Socket();
        }

        SSLSocketFactory factory = config.getCustomSSLFactory();
        if (factory != null) {
            return factory.createSocket();
        }

        String protocol = resolveSslProtocol(config);
        TrustManager[] trustManagers = normalizeTrustManagers(config.getTrustManagers());
        SSLContext sslContext = SSLContext.getInstance(protocol);
        sslContext.init(config.getKeyManagers(), trustManagers, new SecureRandom());
        return sslContext.getSocketFactory().createSocket();
    }

    static String resolveSslProtocol(OkSocketSSLConfig config) {
        if (config == null || TextUtils.isEmpty(config.getProtocol())) {
            return DEFAULT_SSL_PROTOCOL;
        }
        return config.getProtocol();
    }

    private class ConnectionThread extends Thread {
        ConnectionThread(String name) {
            super(name);
        }

        @Override
        public void run() {
            boolean connectSucceeded = false;
            try {
                try {
                    mSocket = getSocketByConfig();
                    prepareSocketBeforeConnect(mSocket);
                    if (isDisconnectInProgress()) {
                        closeSocketQuietly(mSocket);
                        return;
                    }
                } catch (UnConnectException e) {
                    throw e;
                } catch (Exception e) {
                    if (mOptions.isDebug()) {
                        SLog.e("Failed to create socket before connect", e);
                    }
                    throw new UnConnectException("Create socket failed.", e);
                }

                if (mLocalConnectionInfo != null) {
                    SLog.i("try bind: " + mLocalConnectionInfo.getIp() + " port:" + mLocalConnectionInfo.getPort());
                    mSocket.bind(new InetSocketAddress(mLocalConnectionInfo.getIp(), mLocalConnectionInfo.getPort()));
                }
                if (isDisconnectInProgress()) {
                    closeSocketQuietly(mSocket);
                    return;
                }

                SLog.i("Start connect: " + mRemoteConnectionInfo.getIp() + ":" + mRemoteConnectionInfo.getPort() + " socket server...");
                mSocket.connect(new InetSocketAddress(mRemoteConnectionInfo.getIp(), mRemoteConnectionInfo.getPort()),
                        mOptions.getConnectTimeoutSecond() * 1000);
                prepareSocketAfterConnect(mSocket);
                if (isDisconnectInProgress()) {
                    closeSocketQuietly(mSocket);
                    return;
                }

                resolveManager();
                if (isDisconnectInProgress()) {
                    closeManagerQuietly(mDisconnectCause);
                    closeSocketQuietly(mSocket);
                    return;
                }

                mConnectionState.set(ConnectionState.CONNECTED);
                connectSucceeded = true;
                sendBroadcast(IAction.ACTION_CONNECTION_SUCCESS);
                SLog.i("Socket server: " + mRemoteConnectionInfo.getIp() + ":" + mRemoteConnectionInfo.getPort() + " connect successful!");
            } catch (Exception e) {
                if (mOptions.isDebug()) {
                    SLog.e("Connection thread failed", e);
                }
                Exception exception = e instanceof UnConnectException ? e : new UnConnectException(e);
                if (shouldSuppressConnectionFailure()) {
                    SLog.i("Connection attempt cancelled manually for " + mRemoteConnectionInfo.getIp() + ":" + mRemoteConnectionInfo.getPort());
                } else {
                    mConnectionState.set(ConnectionState.DISCONNECTED);
                    SLog.e("Socket server " + mRemoteConnectionInfo.getIp() + ":" + mRemoteConnectionInfo.getPort() + " connect failed! error msg:" + e.getMessage());
                    sendBroadcast(IAction.ACTION_CONNECTION_FAILED, exception);
                }
            } finally {
                if (!connectSucceeded && mConnectionState.get() == ConnectionState.CONNECTING) {
                    mConnectionState.set(ConnectionState.DISCONNECTED);
                }
            }
        }
    }

    private void resolveManager() throws IOException {
        mPulseManager = new PulseManager(this, mOptions);
        mManager = new IOThreadManager(
                mSocket.getInputStream(),
                mSocket.getOutputStream(),
                mOptions,
                mActionDispatcher);
        mManager.startEngine();
    }

    @Override
    public void disconnect(Exception exception) {
        synchronized (this) {
            if (isDisconnectInProgress()) {
                return;
            }

            if (mConnectionState.get() == ConnectionState.IDLE
                    && mSocket == null
                    && mConnectThread == null
                    && mManager == null) {
                mConnectionState.set(ConnectionState.DISCONNECTED);
                mDisconnectCause = null;
                return;
            }

            mConnectionState.set(ConnectionState.DISCONNECTING);
            mDisconnectCause = exception;

            if (mPulseManager != null) {
                mPulseManager.dead();
                mPulseManager = null;
            }
        }

        if (exception instanceof ManuallyDisconnectException) {
            if (mReconnectionManager != null) {
                mReconnectionManager.detach();
                SLog.i("ReconnectionManager is detached.");
            }
        }

        String info = mRemoteConnectionInfo.getIp() + ":" + mRemoteConnectionInfo.getPort();
        DisconnectThread thread = new DisconnectThread(exception, "Disconnect Thread for " + info);
        thread.setDaemon(true);
        thread.start();
    }

    private class DisconnectThread extends Thread {
        private Exception mException;

        DisconnectThread(Exception exception, String name) {
            super(name);
            mException = exception;
        }

        @Override
        public void run() {
            boolean shouldBroadcastDisconnection = mSocket != null || mManager != null || mConnectThread != null;
            try {
                closeManagerQuietly(mException);

                if (mConnectThread != null && mConnectThread.isAlive()) {
                    closeSocketQuietly(mSocket);
                    mConnectThread.interrupt();
                    try {
                        SLog.i("disconnect thread need waiting for connection thread done.");
                        mConnectThread.join();
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                    SLog.i("connection thread is done. disconnection thread going on");
                    mConnectThread = null;
                }

                closeSocketQuietly(mSocket);

                if (mActionHandler != null) {
                    mActionHandler.detach(ConnectionManagerImpl.this);
                    SLog.i("mActionHandler is detached.");
                    mActionHandler = null;
                }
            } finally {
                mConnectionState.set(ConnectionState.DISCONNECTED);
                if (!(mException instanceof UnConnectException) && shouldBroadcastDisconnection) {
                    mException = mException instanceof ManuallyDisconnectException ? null : mException;
                    sendBroadcast(IAction.ACTION_DISCONNECTION, mException);
                }
                mSocket = null;
                mManager = null;

                if (mException != null) {
                    SLog.e("socket is disconnecting because: " + mException.getMessage());
                    if (mOptions.isDebug()) {
                        SLog.e("Disconnect thread captured exception", mException);
                    }
                }
                mDisconnectCause = null;
            }
        }
    }

    @Override
    public void disconnect() {
        disconnect(new ManuallyDisconnectException());
    }

    @Override
    public IConnectionManager send(ISendable sendable) {
        if (mManager != null && sendable != null && isConnect()) {
            mManager.send(sendable);
        }
        return this;
    }

    @Override
    public IConnectionManager option(OkSocketOptions okOptions) {
        if (okOptions == null) {
            return this;
        }
        mOptions = okOptions;
        if (mManager != null) {
            mManager.setOkOptions(mOptions);
        }

        if (mPulseManager != null) {
            mPulseManager.setOkOptions(mOptions);
        }
        if (hasActiveSocket(mSocket)) {
            applyRuntimeSocketOptions(mSocket);
        }
        if (mReconnectionManager != null && !mReconnectionManager.equals(mOptions.getReconnectionManager())) {
            mReconnectionManager.detach();
            SLog.i("reconnection manager is replaced");
            mReconnectionManager = mOptions.getReconnectionManager();
            if (mReconnectionManager != null) {
                mReconnectionManager.attach(this);
            }
        }
        return this;
    }

    @Override
    public OkSocketOptions getOption() {
        return mOptions;
    }

    @Override
    public boolean isConnect() {
        if (mConnectionState.get() != ConnectionState.CONNECTED || mSocket == null) {
            return false;
        }

        return mSocket.isConnected()
                && !mSocket.isClosed()
                && !mSocket.isInputShutdown()
                && !mSocket.isOutputShutdown();
    }

    @Override
    public boolean isDisconnecting() {
        return isDisconnectInProgress();
    }

    @Override
    public PulseManager getPulseManager() {
        return mPulseManager;
    }

    @Override
    public void setIsConnectionHolder(boolean isHold) {
        mOptions = new OkSocketOptions.Builder(mOptions).setConnectionHolden(isHold).build();
    }

    @Override
    public AbsReconnectionManager getReconnectionManager() {
        return mOptions.getReconnectionManager();
    }

    @Override
    public ConnectionInfo getLocalConnectionInfo() {
        ConnectionInfo local = super.getLocalConnectionInfo();
        if (local == null && isConnect()) {
            InetSocketAddress address = (InetSocketAddress) mSocket.getLocalSocketAddress();
            if (address != null) {
                if (address.getAddress() != null) {
                    local = new ConnectionInfo(address.getAddress().getHostAddress(), address.getPort());
                } else {
                    local = new ConnectionInfo(address.getHostString(), address.getPort());
                }
            }
        }
        return local;
    }

    @Override
    public void setLocalConnectionInfo(ConnectionInfo localConnectionInfo) {
        if (isConnect()) {
            throw new IllegalStateException("Socket is connected, can't set local info after connect.");
        }
        mLocalConnectionInfo = localConnectionInfo;
    }

    private void prepareSocketBeforeConnect(Socket socket) throws SocketException {
        if (socket == null) {
            return;
        }
        socket.setReuseAddress(mOptions.isSocketReuseAddress());
    }

    private void prepareSocketAfterConnect(Socket socket) throws IOException {
        if (socket == null) {
            return;
        }
        socket.setKeepAlive(mOptions.isSocketKeepAlive());
        socket.setTcpNoDelay(mOptions.isSocketTcpNoDelay());
        if (socket instanceof SSLSocket) {
            ((SSLSocket) socket).startHandshake();
        }
    }

    private void applyRuntimeSocketOptions(Socket socket) {
        if (socket == null) {
            return;
        }
        try {
            socket.setKeepAlive(mOptions.isSocketKeepAlive());
            socket.setTcpNoDelay(mOptions.isSocketTcpNoDelay());
        } catch (SocketException e) {
            if (mOptions.isDebug()) {
                SLog.e("Unable to apply runtime socket options", e);
            }
            SLog.e("Unable to apply runtime socket options: " + e.getMessage());
        }
    }

    private boolean isDisconnectInProgress() {
        return mConnectionState.get() == ConnectionState.DISCONNECTING;
    }

    private boolean hasActiveSocket(Socket socket) {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    private boolean isManualDisconnectInProgress() {
        return isDisconnectInProgress() && mDisconnectCause instanceof ManuallyDisconnectException;
    }

    private boolean shouldSuppressConnectionFailure() {
        return isManualDisconnectInProgress();
    }

    private TrustManager[] normalizeTrustManagers(TrustManager[] trustManagers) {
        return trustManagers == null || trustManagers.length == 0 ? null : trustManagers;
    }

    private void closeManagerQuietly(Exception exception) {
        if (mManager == null) {
            return;
        }
        mManager.close(exception);
    }

    private void closeSocketQuietly(Socket socket) {
        if (socket == null) {
            return;
        }
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    private enum ConnectionState {
        IDLE,
        CONNECTING,
        CONNECTED,
        DISCONNECTING,
        DISCONNECTED
    }
}
