package com.xuhao.didi.socket.client.impl.client;

import com.xuhao.didi.socket.client.sdk.client.OkSocketSSLConfig;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ConnectionManagerImplSslProtocolTest {

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
}
