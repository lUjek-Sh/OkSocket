package com.xuhao.didi.socket.common.interfaces.common_interfacies.server;


public interface IServerActionListener {
    void onServerListening(int serverPort);

    default void onServerListenFailed(int serverPort, Throwable throwable) {
    }

    void onClientConnected(IClient client, int serverPort, IClientPool clientPool);

    void onClientDisconnected(IClient client, int serverPort, IClientPool clientPool);

    void onServerWillBeShutdown(int serverPort, IServerShutdown shutdown, IClientPool clientPool, Throwable throwable);

    void onServerAlreadyShutdown(int serverPort);

}
