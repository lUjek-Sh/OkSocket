package com.xuhao.didi.socket.server.impl;

import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import static org.junit.Assert.assertFalse;

public class ServerManagerImplSocketOptionsTest {

    @Test
    public void privateSocketConfigurationShouldRespectServerOptions() throws Exception {
        ServerManagerImpl manager = new ServerManagerImpl();
        OkServerOptions options = new OkServerOptions.Builder()
                .setServerSocketReuseAddress(false)
                .setClientSocketReuseAddress(false)
                .setClientSocketKeepAlive(false)
                .setClientSocketTcpNoDelay(false)
                .build();

        Field optionsField = ServerManagerImpl.class.getDeclaredField("mServerOptions");
        optionsField.setAccessible(true);
        optionsField.set(manager, options);

        ServerSocket serverSocket = new ServerSocket();
        Socket clientSocket = new Socket();
        Socket serverSideSocket = null;
        try {
            serverSocket.setReuseAddress(true);
            invokeServerSocketMethod(manager, "configuration", serverSocket);
            assertFalse(serverSocket.getReuseAddress());

            serverSocket.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0));
            clientSocket.connect(new InetSocketAddress("127.0.0.1", serverSocket.getLocalPort()));
            serverSideSocket = serverSocket.accept();
            serverSideSocket.setReuseAddress(true);
            serverSideSocket.setKeepAlive(true);
            serverSideSocket.setTcpNoDelay(true);

            invokeSocketMethod(manager, "configure", serverSideSocket);
            assertFalse(serverSideSocket.getReuseAddress());
            assertFalse(serverSideSocket.getKeepAlive());
            assertFalse(serverSideSocket.getTcpNoDelay());
        } finally {
            if (serverSideSocket != null) {
                serverSideSocket.close();
            }
            clientSocket.close();
            serverSocket.close();
        }
    }

    private void invokeServerSocketMethod(ServerManagerImpl manager, String methodName, ServerSocket serverSocket) throws Exception {
        Method method = ServerManagerImpl.class.getDeclaredMethod(methodName, ServerSocket.class);
        method.setAccessible(true);
        method.invoke(manager, serverSocket);
    }

    private void invokeSocketMethod(ServerManagerImpl manager, String methodName, Socket socket) throws Exception {
        Method method = ServerManagerImpl.class.getDeclaredMethod(methodName, Socket.class);
        method.setAccessible(true);
        method.invoke(manager, socket);
    }
}
