package com.xuhao.didi.socket.server.impl;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;

public class OkServerOptionsTest {

    @Test
    public void builderShouldCopySourceOptions() {
        OkServerOptions source = OkServerOptions.getDefault();
        int sourceCapacity = source.getConnectCapacity();

        OkServerOptions copy = new OkServerOptions.Builder(source)
                .setConnectCapacity(sourceCapacity + 1)
                .setServerSocketReuseAddress(false)
                .setClientSocketReuseAddress(false)
                .setClientSocketKeepAlive(false)
                .setClientSocketTcpNoDelay(false)
                .setClientPoolOverflowStrategy(OkServerOptions.ClientPoolOverflowStrategy.EVICT_OLDEST_CLIENT)
                .build();

        assertNotSame(source, copy);
        assertEquals(sourceCapacity, source.getConnectCapacity());
        assertEquals(sourceCapacity + 1, copy.getConnectCapacity());
        assertFalse(copy.isServerSocketReuseAddress());
        assertFalse(copy.isClientSocketReuseAddress());
        assertFalse(copy.isClientSocketKeepAlive());
        assertFalse(copy.isClientSocketTcpNoDelay());
        assertEquals(OkServerOptions.ClientPoolOverflowStrategy.EVICT_OLDEST_CLIENT,
                copy.getClientPoolOverflowStrategy());
    }

    @Test(expected = IllegalArgumentException.class)
    public void buildShouldRejectZeroConnectCapacity() {
        new OkServerOptions.Builder()
                .setConnectCapacity(0)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void buildShouldRejectZeroWritePackageBytes() {
        new OkServerOptions.Builder()
                .setWritePackageBytes(0)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void buildShouldRejectNullReaderProtocol() {
        new OkServerOptions.Builder()
                .setReaderProtocol(null)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void buildShouldRejectNullClientPoolOverflowStrategy() {
        new OkServerOptions.Builder()
                .setClientPoolOverflowStrategy(null)
                .build();
    }
}
