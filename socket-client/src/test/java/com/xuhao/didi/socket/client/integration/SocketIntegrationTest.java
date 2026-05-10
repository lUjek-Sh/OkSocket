package com.xuhao.didi.socket.client.integration;

import com.xuhao.didi.core.iocore.interfaces.IPulseSendable;
import com.xuhao.didi.core.iocore.interfaces.ISendable;
import com.xuhao.didi.core.pojo.OriginalData;
import com.xuhao.didi.socket.client.sdk.OkSocket;
import com.xuhao.didi.socket.client.sdk.client.ConnectionInfo;
import com.xuhao.didi.socket.client.sdk.client.OkSocketOptions;
import com.xuhao.didi.socket.client.sdk.client.action.SocketActionAdapter;
import com.xuhao.didi.socket.client.sdk.client.connection.DefaultReconnectManager;
import com.xuhao.didi.socket.client.sdk.client.connection.IConnectionManager;
import com.xuhao.didi.socket.client.sdk.client.connection.NoneReconnect;
import com.xuhao.didi.socket.common.interfaces.common_interfacies.server.IClient;
import com.xuhao.didi.socket.common.interfaces.common_interfacies.server.IClientIOCallback;
import com.xuhao.didi.socket.common.interfaces.common_interfacies.server.IClientPool;
import com.xuhao.didi.socket.common.interfaces.common_interfacies.server.IServerManager;
import com.xuhao.didi.socket.server.action.ServerActionAdapter;
import com.xuhao.didi.socket.server.impl.OkServerOptions;

import org.junit.Test;

import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SocketIntegrationTest {
    private static final long WAIT_TIMEOUT_SECONDS = 8L;

    @Test
    public void clientAndServerShouldRoundTripMessages() throws Exception {
        int port = findFreePort();
        TestServerHarness server = TestServerHarness.start(port, "echo:");
        IConnectionManager client = null;
        try {
            ClientProbe probe = new ClientProbe();
            client = openClient(port, baseClientOptions(), probe);

            probe.awaitConnectionSuccess();
            server.awaitClientConnected();

            client.send(new TextSendable("hello"));

            assertEquals("hello", server.awaitRead());
            assertEquals("echo:hello", probe.awaitRead());
        } finally {
            close(client, server);
        }
    }

    @Test
    public void clientShouldSwitchConnectionInfoAndReconnectToNewServer() throws Exception {
        int firstPort = findFreePort();
        int secondPort = findFreePort();
        TestServerHarness firstServer = TestServerHarness.start(firstPort, "one:");
        TestServerHarness secondServer = TestServerHarness.start(secondPort, "two:");
        IConnectionManager client = null;
        try {
            ClientProbe probe = new ClientProbe();
            client = openClient(firstPort, baseClientOptions(), probe);

            ConnectionInfo firstInfo = probe.awaitConnectionSuccess();
            assertEquals(firstPort, firstInfo.getPort());
            firstServer.awaitClientConnected();

            client.send(new TextSendable("first"));
            assertEquals("one:first", probe.awaitRead());

            client.disconnect();
            probe.awaitDisconnection();

            client.switchConnectionInfo(new ConnectionInfo("127.0.0.1", secondPort));
            client.connect();

            ConnectionInfo secondInfo = probe.awaitConnectionSuccess();
            assertEquals(secondPort, secondInfo.getPort());
            secondServer.awaitClientConnected();

            client.send(new TextSendable("second"));
            assertEquals("two:second", probe.awaitRead());
        } finally {
            close(client, firstServer, secondServer);
        }
    }

    @Test
    public void clientShouldReconnectAfterServerRestart() throws Exception {
        int port = findFreePort();
        TestServerHarness server = TestServerHarness.start(port, "re:");
        IConnectionManager client = null;
        try {
            ClientProbe probe = new ClientProbe();
            OkSocketOptions options = new OkSocketOptions.Builder(baseClientOptions())
                    .setReconnectionManager(new DefaultReconnectManager())
                    .setReconnectDelayMillis(200L)
                    .setReconnectMaxDelayMillis(200L)
                    .setReconnectDelayScale(1.0d)
                    .setReconnectJitterRatio(0.0d)
                    .build();
            client = openClient(port, options, probe);

            probe.awaitConnectionSuccess();
            server.awaitClientConnected();
            client.send(new TextSendable("before"));
            assertEquals("re:before", probe.awaitRead());

            server.shutdown();
            probe.awaitDisconnection();

            server.listen();
            probe.awaitConnectionSuccess();
            server.awaitClientConnected();

            client.send(new TextSendable("after"));
            assertEquals("re:after", probe.awaitRead());
        } finally {
            close(client, server);
        }
    }

    @Test
    public void clientShouldFailOverToBackupConnectionInfo() throws Exception {
        int primaryPort = findFreePort();
        int backupPort = findFreePort();
        TestServerHarness backupServer = TestServerHarness.start(backupPort, "backup:");
        IConnectionManager client = null;
        try {
            ClientProbe probe = new ClientProbe();
            ConnectionInfo info = new ConnectionInfo("127.0.0.1", primaryPort);
            info.setBackupInfo(new ConnectionInfo("127.0.0.1", backupPort));

            OkSocketOptions options = new OkSocketOptions.Builder(baseClientOptions())
                    .setConnectTimeoutSecond(0)
                    .setReconnectionManager(new DefaultReconnectManager())
                    .setReconnectDelayMillis(50L)
                    .setReconnectMaxDelayMillis(50L)
                    .setReconnectDelayScale(1.0d)
                    .setReconnectJitterRatio(0.0d)
                    .setReconnectBackupSwitchThreshold(0)
                    .build();
            client = OkSocket.open(info, options);
            client.registerReceiver(probe);
            client.connect();

            ConnectionInfo connectedInfo = probe.awaitConnectionSuccess();
            assertEquals(backupPort, connectedInfo.getPort());
            assertEquals(backupPort, client.getRemoteConnectionInfo().getPort());

            backupServer.awaitClientConnected();
            client.send(new TextSendable("payload"));
            assertEquals("backup:payload", probe.awaitRead());
        } finally {
            close(client, backupServer);
        }
    }

    @Test
    public void pulseShouldReachServerAndTriggerPulseCallback() throws Exception {
        int port = findFreePort();
        TestServerHarness server = TestServerHarness.start(port, null);
        IConnectionManager client = null;
        try {
            ClientProbe probe = new ClientProbe();
            client = openClient(port, baseClientOptions(), probe);

            probe.awaitConnectionSuccess();
            server.awaitClientConnected();

            client.getPulseManager().setPulseSendable(new PulseSendable("pulse"));
            client.getPulseManager().trigger();

            assertEquals("pulse", server.awaitRead());
            assertEquals("pulse", probe.awaitPulse());
        } finally {
            close(client, server);
        }
    }

    private IConnectionManager openClient(int port, OkSocketOptions options, ClientProbe probe) {
        IConnectionManager manager = OkSocket.open(new ConnectionInfo("127.0.0.1", port), options);
        manager.registerReceiver(probe);
        manager.connect();
        return manager;
    }

    private OkSocketOptions baseClientOptions() {
        return new OkSocketOptions.Builder()
                .setConnectTimeoutSecond(1)
                .setReconnectionManager(new NoneReconnect())
                .build();
    }

    private void close(IConnectionManager client, TestServerHarness... servers) {
        if (client != null) {
            try {
                client.disconnect();
            } catch (Exception ignored) {
            }
        }
        if (servers != null) {
            for (TestServerHarness server : servers) {
                if (server != null) {
                    server.shutdown();
                }
            }
        }
    }

    private int findFreePort() throws Exception {
        ServerSocket serverSocket = new ServerSocket(0);
        try {
            return serverSocket.getLocalPort();
        } finally {
            serverSocket.close();
        }
    }

    private static String decodeBody(OriginalData data) {
        return new String(data.getBodyBytes(), StandardCharsets.UTF_8);
    }

    private static String decodeSendable(ISendable sendable) {
        byte[] bytes = sendable.parse();
        return new String(bytes, 4, bytes.length - 4, StandardCharsets.UTF_8);
    }

    private static final class ClientProbe extends SocketActionAdapter {
        private final BlockingQueue<ConnectionInfo> successQueue = new LinkedBlockingQueue<>();
        private final BlockingQueue<String> readQueue = new LinkedBlockingQueue<>();
        private final BlockingQueue<String> pulseQueue = new LinkedBlockingQueue<>();
        private final BlockingQueue<Throwable> disconnectQueue = new LinkedBlockingQueue<>();

        @Override
        public void onSocketConnectionSuccess(ConnectionInfo info, String action) {
            successQueue.offer(info);
        }

        @Override
        public void onSocketReadResponse(ConnectionInfo info, String action, OriginalData data) {
            readQueue.offer(decodeBody(data));
        }

        @Override
        public void onPulseSend(ConnectionInfo info, IPulseSendable data) {
            pulseQueue.offer(decodeSendable(data));
        }

        @Override
        public void onSocketDisconnection(ConnectionInfo info, String action, Exception e) {
            disconnectQueue.offer(e == null ? new RuntimeException("manual") : e);
        }

        ConnectionInfo awaitConnectionSuccess() throws Exception {
            ConnectionInfo info = successQueue.poll(WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertNotNull("Timed out waiting for connection success", info);
            return info;
        }

        String awaitRead() throws Exception {
            String message = readQueue.poll(WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertNotNull("Timed out waiting for client read", message);
            return message;
        }

        String awaitPulse() throws Exception {
            String message = pulseQueue.poll(WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertNotNull("Timed out waiting for pulse callback", message);
            return message;
        }

        void awaitDisconnection() throws Exception {
            Throwable throwable = disconnectQueue.poll(WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertNotNull("Timed out waiting for disconnection", throwable);
        }
    }

    private static final class TestServerHarness {
        private final IServerManager<OkServerOptions> manager;
        private final OkServerOptions options;
        private final String responsePrefix;
        private final BlockingQueue<IClient> connectedClients = new LinkedBlockingQueue<>();
        private final BlockingQueue<String> reads = new LinkedBlockingQueue<>();
        private final AtomicReference<IClientPool<IClient, String>> lastClientPool = new AtomicReference<>();

        private TestServerHarness(int port, String responsePrefix) {
            this.responsePrefix = responsePrefix;
            this.options = OkServerOptions.getDefault();
            this.manager = (IServerManager<OkServerOptions>) OkSocket.server(port).registerReceiver(new ServerActionAdapter() {
                @Override
                public void onClientConnected(IClient client, int serverPort, IClientPool clientPool) {
                    lastClientPool.set(clientPool);
                    client.addIOCallback(new IClientIOCallback() {
                        @Override
                        public void onClientRead(OriginalData originalData, IClient ioClient, IClientPool<IClient, String> pool) {
                            String body = decodeBody(originalData);
                            reads.offer(body);
                            if (TestServerHarness.this.responsePrefix != null) {
                                ioClient.send(new TextSendable(TestServerHarness.this.responsePrefix + body));
                            }
                        }

                        @Override
                        public void onClientWrite(ISendable sendable, IClient ioClient, IClientPool<IClient, String> pool) {
                        }
                    });
                    connectedClients.offer(client);
                }
            });
        }

        static TestServerHarness start(int port, String responsePrefix) throws Exception {
            TestServerHarness harness = new TestServerHarness(port, responsePrefix);
            harness.listen();
            return harness;
        }

        void listen() {
            manager.listen(options);
        }

        void shutdown() {
            try {
                manager.shutdown();
            } catch (Exception ignored) {
            }
        }

        IClient awaitClientConnected() throws Exception {
            IClient client = connectedClients.poll(WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertNotNull("Timed out waiting for server-side client connection", client);
            return client;
        }

        String awaitRead() throws Exception {
            String body = reads.poll(WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertNotNull("Timed out waiting for server read", body);
            return body;
        }
    }

    private static class TextSendable implements ISendable {
        private final String text;

        private TextSendable(String text) {
            this.text = text;
        }

        @Override
        public byte[] parse() {
            byte[] body = text.getBytes(StandardCharsets.UTF_8);
            ByteBuffer byteBuffer = ByteBuffer.allocate(4 + body.length);
            byteBuffer.order(ByteOrder.BIG_ENDIAN);
            byteBuffer.putInt(body.length);
            byteBuffer.put(body);
            return byteBuffer.array();
        }
    }

    private static final class PulseSendable extends TextSendable implements IPulseSendable {
        private PulseSendable(String text) {
            super(text);
        }
    }
}
