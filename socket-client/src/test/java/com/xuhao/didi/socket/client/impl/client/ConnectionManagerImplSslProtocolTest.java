package com.xuhao.didi.socket.client.impl.client;

import com.xuhao.didi.socket.client.sdk.client.ConnectionInfo;
import com.xuhao.didi.socket.client.sdk.client.OkSocketOptions;
import com.xuhao.didi.socket.client.sdk.client.OkSocketSSLConfig;
import com.xuhao.didi.socket.client.sdk.client.connection.NoneReconnect;
import com.xuhao.didi.socket.common.interfaces.default_protocol.TrustAllX509TrustManager;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ConnectionManagerImplSslProtocolTest {
    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @Test
    public void resolveSslProtocolShouldDefaultToTls() {
        assertEquals("TLS", ConnectionManagerImpl.resolveSslProtocol(null));
        assertEquals("TLS", ConnectionManagerImpl.resolveSslProtocol(new OkSocketSSLConfig.Builder().build()));
    }

    @Test
    public void resolveSslProtocolShouldPreferExplicitProtocol() {
        OkSocketSSLConfig config = new OkSocketSSLConfig.Builder()
                .setProtocol("TLSv1.3")
                .build();

        assertEquals("TLSv1.3", ConnectionManagerImpl.resolveSslProtocol(config));
    }

    @Test
    public void secureDefaultSslShouldRejectSelfSignedServer() throws Exception {
        SelfSignedTlsServer server = SelfSignedTlsServer.start();
        ConnectionManagerImpl manager = null;
        Socket socket = null;
        try {
            manager = newManager(server.getPort(), new OkSocketSSLConfig.Builder().build());
            socket = createSocket(manager);

            invokeSocketMethod(manager, "prepareSocketBeforeConnect", socket);
            socket.connect(new InetSocketAddress("127.0.0.1", server.getPort()), 1_000);

            try {
                invokeSocketMethod(manager, "prepareSocketAfterConnect", socket);
                fail("Expected TLS handshake failure");
            } catch (InvocationTargetException e) {
                assertTrue(containsCause(e.getCause(), SSLHandshakeException.class));
            }
        } finally {
            if (socket != null) {
                socket.close();
            }
            server.close();
        }
    }

    @Test
    public void explicitTrustAllManagerShouldAllowSelfSignedServer() throws Exception {
        SelfSignedTlsServer server = SelfSignedTlsServer.start();
        ConnectionManagerImpl manager = null;
        Socket socket = null;
        try {
            OkSocketSSLConfig sslConfig = new OkSocketSSLConfig.Builder()
                    .setTrustManagers(new TrustManager[]{new TrustAllX509TrustManager()})
                    .build();
            manager = newManager(server.getPort(), sslConfig);
            socket = createSocket(manager);

            invokeSocketMethod(manager, "prepareSocketBeforeConnect", socket);
            socket.connect(new InetSocketAddress("127.0.0.1", server.getPort()), 1_000);
            invokeSocketMethod(manager, "prepareSocketAfterConnect", socket);

            assertTrue(server.awaitAccepted());
        } finally {
            if (socket != null) {
                socket.close();
            }
            server.close();
        }
    }

    @Test
    public void sslFactoryFailureShouldNotFallbackToPlainSocket() throws Exception {
        ConnectionManagerImpl manager = new ConnectionManagerImpl(new ConnectionInfo("127.0.0.1", 65535), null);
        OkSocketSSLConfig sslConfig = new OkSocketSSLConfig.Builder()
                .setCustomSSLFactory(new FailingSslSocketFactory())
                .build();
        manager.option(new OkSocketOptions.Builder()
                .setSSLConfig(sslConfig)
                .setReconnectionManager(new NoneReconnect())
                .build());

        Method method = ConnectionManagerImpl.class.getDeclaredMethod("getSocketByConfig");
        method.setAccessible(true);

        try {
            method.invoke(manager);
            fail("Expected SSL socket creation to fail");
        } catch (InvocationTargetException e) {
            assertTrue(e.getCause() instanceof IOException);
            assertEquals("factory boom", e.getCause().getMessage());
        }
    }

    private ConnectionManagerImpl newManager(int port, OkSocketSSLConfig sslConfig) {
        ConnectionManagerImpl manager = new ConnectionManagerImpl(new ConnectionInfo("127.0.0.1", port), null);
        manager.option(new OkSocketOptions.Builder()
                .setConnectTimeoutSecond(1)
                .setReconnectionManager(new NoneReconnect())
                .setSSLConfig(sslConfig)
                .build());
        return manager;
    }

    private Socket createSocket(ConnectionManagerImpl manager) throws Exception {
        Method method = ConnectionManagerImpl.class.getDeclaredMethod("getSocketByConfig");
        method.setAccessible(true);
        return (Socket) method.invoke(manager);
    }

    private void invokeSocketMethod(ConnectionManagerImpl manager, String methodName, Socket socket) throws Exception {
        Method method = ConnectionManagerImpl.class.getDeclaredMethod(methodName, Socket.class);
        method.setAccessible(true);
        method.invoke(manager, socket);
    }

    private boolean containsCause(Throwable throwable, Class<? extends Throwable> expectedType) {
        Throwable current = throwable;
        while (current != null) {
            if (expectedType.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static final class FailingSslSocketFactory extends SSLSocketFactory {
        @Override
        public String[] getDefaultCipherSuites() {
            return new String[0];
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return new String[0];
        }

        @Override
        public java.net.Socket createSocket() throws IOException {
            throw new IOException("factory boom");
        }

        @Override
        public java.net.Socket createSocket(java.net.Socket s, String host, int port, boolean autoClose) throws IOException {
            throw new IOException("factory boom");
        }

        @Override
        public java.net.Socket createSocket(String host, int port) throws IOException {
            throw new IOException("factory boom");
        }

        @Override
        public java.net.Socket createSocket(String host, int port, java.net.InetAddress localHost, int localPort) throws IOException {
            throw new IOException("factory boom");
        }

        @Override
        public java.net.Socket createSocket(java.net.InetAddress host, int port) throws IOException {
            throw new IOException("factory boom");
        }

        @Override
        public java.net.Socket createSocket(java.net.InetAddress address, int port, java.net.InetAddress localAddress, int localPort) throws IOException {
            throw new IOException("factory boom");
        }
    }

    private static final class SelfSignedTlsServer implements AutoCloseable {
        private static final char[] PASSWORD = "changeit".toCharArray();

        private final SSLServerSocket serverSocket;
        private final Thread acceptThread;
        private final java.util.concurrent.CountDownLatch accepted = new java.util.concurrent.CountDownLatch(1);
        private volatile java.net.Socket acceptedSocket;

        private SelfSignedTlsServer(SSLServerSocket serverSocket) {
            this.serverSocket = serverSocket;
            this.acceptThread = new Thread(() -> {
                try {
                    acceptedSocket = serverSocket.accept();
                    if (acceptedSocket instanceof SSLSocket) {
                        ((SSLSocket) acceptedSocket).startHandshake();
                    }
                    accepted.countDown();
                } catch (IOException ignored) {
                }
            }, "self-signed-tls-server");
            this.acceptThread.setDaemon(true);
            this.acceptThread.start();
        }

        static SelfSignedTlsServer start() throws Exception {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(createKeyManagers(), null, null);
            SSLServerSocketFactory factory = sslContext.getServerSocketFactory();
            SSLServerSocket socket = (SSLServerSocket) factory.createServerSocket(0);
            return new SelfSignedTlsServer(socket);
        }

        int getPort() {
            return serverSocket.getLocalPort();
        }

        boolean awaitAccepted() throws InterruptedException {
            return accepted.await(3, TimeUnit.SECONDS);
        }

        @Override
        public void close() throws Exception {
            if (acceptedSocket != null) {
                acceptedSocket.close();
            }
            serverSocket.close();
            acceptThread.interrupt();
            acceptThread.join(500L);
        }

        private static javax.net.ssl.KeyManager[] createKeyManagers() throws Exception {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(null, PASSWORD);

            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME);
            keyPairGenerator.initialize(2048);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            X509Certificate certificate = createSelfSignedCertificate(keyPair);
            keyStore.setKeyEntry("server", keyPair.getPrivate(), PASSWORD, new java.security.cert.Certificate[]{certificate});

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, PASSWORD);
            return keyManagerFactory.getKeyManagers();
        }

        private static X509Certificate createSelfSignedCertificate(KeyPair keyPair) throws Exception {
            long now = System.currentTimeMillis();
            X500Name subject = new X500Name("CN=127.0.0.1");
            JcaX509v3CertificateBuilder certificateBuilder = new JcaX509v3CertificateBuilder(
                    subject,
                    BigInteger.valueOf(now),
                    new Date(now - TimeUnit.HOURS.toMillis(1)),
                    new Date(now + TimeUnit.DAYS.toMillis(1)),
                    subject,
                    keyPair.getPublic());
            JcaX509ExtensionUtils extensionUtils = new JcaX509ExtensionUtils();
            certificateBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
            certificateBuilder.addExtension(Extension.subjectKeyIdentifier, false,
                    extensionUtils.createSubjectKeyIdentifier(keyPair.getPublic()));
            certificateBuilder.addExtension(Extension.authorityKeyIdentifier, false,
                    extensionUtils.createAuthorityKeyIdentifier(keyPair.getPublic()));

            ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                    .build(keyPair.getPrivate());
            X509CertificateHolder holder = certificateBuilder.build(signer);
            return new JcaX509CertificateConverter()
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                    .getCertificate(holder);
        }
    }
}
