package com.xuhao.didi.socket.client.impl.client.action;

import com.xuhao.didi.core.exceptions.ReadException;
import com.xuhao.didi.core.exceptions.WriteException;
import com.xuhao.didi.core.iocore.interfaces.ISendable;
import com.xuhao.didi.socket.client.impl.client.PulseManager;
import com.xuhao.didi.socket.client.sdk.client.ConnectionInfo;
import com.xuhao.didi.socket.client.sdk.client.OkSocketOptions;
import com.xuhao.didi.socket.client.sdk.client.action.IAction;
import com.xuhao.didi.socket.client.sdk.client.action.ISocketActionListener;
import com.xuhao.didi.socket.client.sdk.client.action.SocketActionAdapter;
import com.xuhao.didi.socket.client.sdk.client.connection.AbsReconnectionManager;
import com.xuhao.didi.socket.client.sdk.client.connection.IConnectionManager;

import org.junit.Test;

import java.net.SocketException;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ActionDispatcherTest {
    @Test
    public void writeShutdownShouldHideConnectionClosureForExternalListeners() {
        FakeConnectionManager manager = new FakeConnectionManager(syncOptions());
        ActionDispatcher dispatcher = new ActionDispatcher(manager.getRemoteConnectionInfo(), manager);
        RecordingListener listener = new RecordingListener();
        dispatcher.registerReceiver(listener);
        new ActionHandler().attach(manager, dispatcher);

        dispatcher.sendBroadcast(IAction.ACTION_READ_THREAD_START);

        WriteException exception = new WriteException(new SocketException("Broken pipe"));
        dispatcher.sendBroadcast(IAction.ACTION_WRITE_THREAD_SHUTDOWN, exception);

        assertSame(exception, manager.disconnectException);
        assertTrue(manager.disconnecting);
        assertNull(listener.lastWriteShutdownException);
    }

    @Test
    public void readShutdownShouldStillReachExternalListeners() {
        FakeConnectionManager manager = new FakeConnectionManager(syncOptions());
        ActionDispatcher dispatcher = new ActionDispatcher(manager.getRemoteConnectionInfo(), manager);
        RecordingListener listener = new RecordingListener();
        dispatcher.registerReceiver(listener);
        new ActionHandler().attach(manager, dispatcher);

        dispatcher.sendBroadcast(IAction.ACTION_READ_THREAD_START);

        ReadException exception = new ReadException("socket input stream reached end of file while reading 4 bytes");
        dispatcher.sendBroadcast(IAction.ACTION_READ_THREAD_SHUTDOWN, exception);

        assertSame(exception, manager.disconnectException);
        assertSame(exception, listener.lastReadShutdownException);
    }

    @Test
    public void writeShutdownShouldKeepNonClosureExceptionsVisible() {
        FakeConnectionManager manager = new FakeConnectionManager(syncOptions());
        ActionDispatcher dispatcher = new ActionDispatcher(manager.getRemoteConnectionInfo(), manager);
        RecordingListener listener = new RecordingListener();
        dispatcher.registerReceiver(listener);
        new ActionHandler().attach(manager, dispatcher);

        dispatcher.sendBroadcast(IAction.ACTION_READ_THREAD_START);

        WriteException exception = new WriteException("protocol mismatch");
        dispatcher.sendBroadcast(IAction.ACTION_WRITE_THREAD_SHUTDOWN, exception);

        assertSame(exception, manager.disconnectException);
        assertSame(exception, listener.lastWriteShutdownException);
    }

    private OkSocketOptions syncOptions() {
        return new OkSocketOptions.Builder()
                .setCallbackThreadModeToken(new OkSocketOptions.ThreadModeToken() {
                    @Override
                    public void handleCallbackEvent(ActionDispatcher.ActionRunnable runnable) {
                        runnable.run();
                    }
                })
                .build();
    }

    private static final class RecordingListener extends SocketActionAdapter {
        private Exception lastReadShutdownException;
        private Exception lastWriteShutdownException;

        @Override
        public void onSocketIOThreadShutdown(String action, Exception e) {
            if (IAction.ACTION_READ_THREAD_SHUTDOWN.equals(action)) {
                lastReadShutdownException = e;
            } else if (IAction.ACTION_WRITE_THREAD_SHUTDOWN.equals(action)) {
                lastWriteShutdownException = e;
            }
        }
    }

    private static final class FakeConnectionManager implements IConnectionManager {
        private final ConnectionInfo remoteConnectionInfo = new ConnectionInfo("127.0.0.1", 12000);
        private OkSocketOptions options;
        private boolean disconnecting;
        private Exception disconnectException;

        private FakeConnectionManager(OkSocketOptions options) {
            this.options = options;
        }

        @Override
        public void connect() {
        }

        @Override
        public IConnectionManager option(OkSocketOptions okOptions) {
            options = okOptions;
            return this;
        }

        @Override
        public OkSocketOptions getOption() {
            return options;
        }

        @Override
        public void disconnect(Exception e) {
            disconnecting = true;
            disconnectException = e;
        }

        @Override
        public void disconnect() {
            disconnect(null);
        }

        @Override
        public IConnectionManager send(ISendable sendable) {
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
            return false;
        }

        @Override
        public boolean isDisconnecting() {
            return disconnecting;
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
            return remoteConnectionInfo.clone();
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
