package com.xuhao.didi.socket.client.impl.client;

import com.xuhao.didi.socket.client.sdk.client.ConnectionInfo;
import com.xuhao.didi.socket.client.sdk.client.OkSocketFactory;
import com.xuhao.didi.socket.client.sdk.client.OkSocketOptions;
import com.xuhao.didi.socket.client.sdk.client.action.SocketActionAdapter;
import com.xuhao.didi.socket.client.sdk.client.connection.NoneReconnect;

import org.junit.Test;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ConnectionManagerImplDisconnectTest {

    @Test
    public void disconnectShouldCloseSocketBeforeWaitingForConnectThread() throws Exception {
        BlockingConnectSocket socket = new BlockingConnectSocket();
        ConnectionManagerImpl manager = newManager(socket, 9101);

        manager.connect();
        assertTrue(socket.awaitConnectAttempt(1, TimeUnit.SECONDS));

        manager.disconnect();

        assertTrue(socket.awaitClose(250, TimeUnit.MILLISECONDS));
        assertTrue(awaitDisconnectComplete(manager, 1500L));
    }

    @Test
    public void manualDisconnectDuringConnectShouldNotDispatchConnectionFailed() throws Exception {
        BlockingConnectSocket socket = new BlockingConnectSocket();
        ConnectionManagerImpl manager = newManager(socket, 9102);
        ConnectAttemptProbe probe = new ConnectAttemptProbe();
        manager.registerReceiver(probe);

        manager.connect();
        assertTrue(socket.awaitConnectAttempt(1, TimeUnit.SECONDS));

        manager.disconnect();

        assertTrue(probe.awaitDisconnection(1, TimeUnit.SECONDS));
        assertFalse(probe.awaitConnectionFailed(300, TimeUnit.MILLISECONDS));
        assertTrue(awaitDisconnectComplete(manager, 1500L));
    }

    private ConnectionManagerImpl newManager(Socket socket, int port) {
        ConnectionManagerImpl manager = new ConnectionManagerImpl(new ConnectionInfo("127.0.0.1", port), null);
        OkSocketOptions options = new OkSocketOptions.Builder()
                .setConnectTimeoutSecond(1)
                .setReconnectionManager(new NoneReconnect())
                .setSocketFactory(new SingleSocketFactory(socket))
                .build();
        manager.option(options);
        return manager;
    }

    private boolean awaitDisconnectComplete(ConnectionManagerImpl manager, long timeoutMs) throws InterruptedException {
        long deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs);
        while (manager.isDisconnecting() && System.nanoTime() < deadlineNanos) {
            Thread.sleep(10L);
        }
        return !manager.isDisconnecting();
    }

    private static final class ConnectAttemptProbe extends SocketActionAdapter {
        private final CountDownLatch disconnection = new CountDownLatch(1);
        private final CountDownLatch connectionFailed = new CountDownLatch(1);

        @Override
        public void onSocketDisconnection(ConnectionInfo info, String action, Exception e) {
            disconnection.countDown();
        }

        @Override
        public void onSocketConnectionFailed(ConnectionInfo info, String action, Exception e) {
            connectionFailed.countDown();
        }

        boolean awaitDisconnection(long timeout, TimeUnit unit) throws InterruptedException {
            return disconnection.await(timeout, unit);
        }

        boolean awaitConnectionFailed(long timeout, TimeUnit unit) throws InterruptedException {
            return connectionFailed.await(timeout, unit);
        }
    }

    private static final class SingleSocketFactory extends OkSocketFactory {
        private final Socket socket;

        private SingleSocketFactory(Socket socket) {
            this.socket = socket;
        }

        @Override
        public Socket createSocket(ConnectionInfo info, OkSocketOptions options) {
            return socket;
        }
    }

    private static final class BlockingConnectSocket extends Socket {
        private final CountDownLatch connectAttempted = new CountDownLatch(1);
        private final CountDownLatch closeCalled = new CountDownLatch(1);
        private volatile boolean closed;

        @Override
        public void connect(SocketAddress endpoint, int timeout) throws IOException {
            connectAttempted.countDown();
            long deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(Math.max(500L, timeout * 2L));
            while (!closed && System.nanoTime() < deadlineNanos) {
                try {
                    Thread.sleep(10L);
                } catch (InterruptedException ignored) {
                }
            }
            if (closed) {
                throw new SocketException("Socket closed");
            }
            throw new SocketTimeoutException("connect timeout");
        }

        @Override
        public void close() {
            closed = true;
            closeCalled.countDown();
        }

        @Override
        public boolean isClosed() {
            return closed;
        }

        boolean awaitConnectAttempt(long timeout, TimeUnit unit) throws InterruptedException {
            return connectAttempted.await(timeout, unit);
        }

        boolean awaitClose(long timeout, TimeUnit unit) throws InterruptedException {
            return closeCalled.await(timeout, unit);
        }
    }
}
