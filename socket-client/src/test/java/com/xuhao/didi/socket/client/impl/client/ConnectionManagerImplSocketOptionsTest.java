package com.xuhao.didi.socket.client.impl.client;

import com.xuhao.didi.socket.client.sdk.client.ConnectionInfo;
import com.xuhao.didi.socket.client.sdk.client.OkSocketOptions;

import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ConnectionManagerImplSocketOptionsTest {

    @Test
    public void prepareSocketMethodsShouldApplyConfiguredTcpOptions() throws Exception {
        ConnectionManagerImpl manager = new ConnectionManagerImpl(new ConnectionInfo("127.0.0.1", 9001), null);
        manager.option(OkSocketOptions.getDefault());

        ServerSocket serverSocket = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
        Socket clientSocket = new Socket();
        Socket serverSideSocket = null;
        try {
            invokeSocketMethod(manager, "prepareSocketBeforeConnect", clientSocket);
            assertTrue(clientSocket.getReuseAddress());

            clientSocket.connect(new InetSocketAddress("127.0.0.1", serverSocket.getLocalPort()));
            serverSideSocket = serverSocket.accept();

            invokeSocketMethod(manager, "prepareSocketAfterConnect", clientSocket);
            assertTrue(clientSocket.getKeepAlive());
            assertTrue(clientSocket.getTcpNoDelay());
        } finally {
            if (serverSideSocket != null) {
                serverSideSocket.close();
            }
            clientSocket.close();
            serverSocket.close();
        }
    }

    @Test
    public void optionShouldApplyRuntimeSocketOptionsToActiveSocket() throws Exception {
        ConnectionManagerImpl manager = new ConnectionManagerImpl(new ConnectionInfo("127.0.0.1", 9002), null);
        manager.option(OkSocketOptions.getDefault());

        ServerSocket serverSocket = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
        Socket clientSocket = new Socket();
        Socket serverSideSocket = null;
        try {
            clientSocket.connect(new InetSocketAddress("127.0.0.1", serverSocket.getLocalPort()));
            serverSideSocket = serverSocket.accept();
            clientSocket.setKeepAlive(true);
            clientSocket.setTcpNoDelay(true);

            Field socketField = ConnectionManagerImpl.class.getDeclaredField("mSocket");
            socketField.setAccessible(true);
            socketField.set(manager, clientSocket);

            manager.option(new OkSocketOptions.Builder(manager.getOption())
                    .setSocketKeepAlive(false)
                    .setSocketTcpNoDelay(false)
                    .build());

            assertFalse(clientSocket.getKeepAlive());
            assertFalse(clientSocket.getTcpNoDelay());
        } finally {
            if (serverSideSocket != null) {
                serverSideSocket.close();
            }
            clientSocket.close();
            serverSocket.close();
        }
    }

    private void invokeSocketMethod(ConnectionManagerImpl manager, String methodName, Socket socket) throws Exception {
        Method method = ConnectionManagerImpl.class.getDeclaredMethod(methodName, Socket.class);
        method.setAccessible(true);
        method.invoke(manager, socket);
    }
}
