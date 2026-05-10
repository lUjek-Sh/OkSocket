package com.xuhao.didi.socket.client.sdk.client;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class OkSocketOptionsValidationTest {

    @Test
    public void builderShouldCopyReconnectDelayOptions() {
        OkSocketOptions source = new OkSocketOptions.Builder()
                .setReconnectDelayMillis(1500L)
                .setReconnectMaxDelayMillis(9000L)
                .setReconnectDelayScale(2.0d)
                .setReconnectJitterRatio(0.25d)
                .setReconnectBackupSwitchThreshold(3)
                .setSocketReuseAddress(false)
                .setSocketKeepAlive(false)
                .setSocketTcpNoDelay(false)
                .build();

        OkSocketOptions copy = new OkSocketOptions.Builder(source).build();

        assertEquals(1500L, copy.getReconnectDelayMillis());
        assertEquals(9000L, copy.getReconnectMaxDelayMillis());
        assertEquals(2.0d, copy.getReconnectDelayScale(), 0.0d);
        assertEquals(0.25d, copy.getReconnectJitterRatio(), 0.0d);
        assertEquals(3, copy.getReconnectBackupSwitchThreshold());
        assertFalse(copy.isSocketReuseAddress());
        assertFalse(copy.isSocketKeepAlive());
        assertFalse(copy.isSocketTcpNoDelay());
    }

    @Test(expected = IllegalArgumentException.class)
    public void buildShouldRejectNegativeConnectTimeout() {
        new OkSocketOptions.Builder()
                .setConnectTimeoutSecond(-1)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void buildShouldRejectZeroReadPackageBytes() {
        new OkSocketOptions.Builder()
                .setReadPackageBytes(0)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void buildShouldRejectInvalidPulseFeedLoseTimes() {
        new OkSocketOptions.Builder()
                .setPulseFeedLoseTimes(0)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void buildShouldRejectNullReaderProtocol() {
        new OkSocketOptions.Builder()
                .setReaderProtocol(null)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void buildShouldRejectReconnectMaxDelayLowerThanBaseDelay() {
        new OkSocketOptions.Builder()
                .setReconnectDelayMillis(5000L)
                .setReconnectMaxDelayMillis(1000L)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void buildShouldRejectReconnectDelayScaleBelowOne() {
        new OkSocketOptions.Builder()
                .setReconnectDelayScale(0.5d)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void buildShouldRejectReconnectJitterRatioGreaterThanOne() {
        new OkSocketOptions.Builder()
                .setReconnectJitterRatio(1.5d)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void buildShouldRejectNegativeReconnectBackupSwitchThreshold() {
        new OkSocketOptions.Builder()
                .setReconnectBackupSwitchThreshold(-1)
                .build();
    }
}
