package com.xuhao.didi.socket.client.sdk.client.connection;

import com.xuhao.didi.socket.client.sdk.client.OkSocketOptions;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DefaultReconnectManagerTest {

    @Test
    public void resolveReconnectDelayShouldKeepDefaultFixedDelay() {
        OkSocketOptions options = OkSocketOptions.getDefault();

        assertEquals(10_000L, DefaultReconnectManager.resolveReconnectDelayMillis(options, 0));
        assertEquals(10_000L, DefaultReconnectManager.resolveReconnectDelayMillis(options, 5));
    }

    @Test
    public void resolveReconnectDelayShouldSupportExponentialBackoff() {
        OkSocketOptions options = new OkSocketOptions.Builder()
                .setConnectTimeoutSecond(0)
                .setReconnectDelayMillis(1_000L)
                .setReconnectMaxDelayMillis(8_000L)
                .setReconnectDelayScale(2.0d)
                .setReconnectJitterRatio(0.0d)
                .build();

        assertEquals(1_000L, DefaultReconnectManager.resolveReconnectDelayMillis(options, 0));
        assertEquals(2_000L, DefaultReconnectManager.resolveReconnectDelayMillis(options, 1));
        assertEquals(4_000L, DefaultReconnectManager.resolveReconnectDelayMillis(options, 2));
        assertEquals(8_000L, DefaultReconnectManager.resolveReconnectDelayMillis(options, 3));
        assertEquals(8_000L, DefaultReconnectManager.resolveReconnectDelayMillis(options, 10));
    }

    @Test
    public void resolveReconnectDelayShouldRespectConnectTimeoutFloor() {
        OkSocketOptions options = new OkSocketOptions.Builder()
                .setConnectTimeoutSecond(5)
                .setReconnectDelayMillis(1_000L)
                .setReconnectMaxDelayMillis(2_000L)
                .setReconnectDelayScale(1.0d)
                .setReconnectJitterRatio(0.0d)
                .build();

        assertEquals(5_000L, DefaultReconnectManager.resolveReconnectDelayMillis(options, 0));
    }

    @Test
    public void resolveBackupSwitchThresholdShouldUseOptions() {
        OkSocketOptions options = new OkSocketOptions.Builder()
                .setReconnectBackupSwitchThreshold(2)
                .build();

        assertEquals(2, DefaultReconnectManager.resolveBackupSwitchThreshold(options));
    }

    @Test
    public void shouldSwitchToBackupShouldRequireFailuresAboveThreshold() {
        OkSocketOptions options = new OkSocketOptions.Builder()
                .setReconnectBackupSwitchThreshold(2)
                .build();

        assertEquals(false, DefaultReconnectManager.shouldSwitchToBackup(options, 2));
        assertEquals(true, DefaultReconnectManager.shouldSwitchToBackup(options, 3));
    }
}
