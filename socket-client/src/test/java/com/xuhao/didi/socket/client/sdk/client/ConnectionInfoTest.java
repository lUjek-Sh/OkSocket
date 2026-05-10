package com.xuhao.didi.socket.client.sdk.client;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

public class ConnectionInfoTest {

    @Test
    public void constructorShouldTrimIp() {
        ConnectionInfo info = new ConnectionInfo(" 127.0.0.1 ", 8080);

        assertEquals("127.0.0.1", info.getIp());
    }

    @Test
    public void cloneShouldCopyBackupInfo() {
        ConnectionInfo info = new ConnectionInfo("127.0.0.1", 8080);
        info.setBackupInfo(new ConnectionInfo("127.0.0.2", 8081));

        ConnectionInfo clone = info.clone();

        assertNotSame(info, clone);
        assertNotSame(info.getBackupInfo(), clone.getBackupInfo());
        assertEquals(info, clone);
        assertEquals(info.getBackupInfo(), clone.getBackupInfo());
    }

    @Test
    public void toStringShouldContainAddress() {
        ConnectionInfo info = new ConnectionInfo("127.0.0.1", 8080);

        assertTrue(info.toString().contains("127.0.0.1"));
        assertTrue(info.toString().contains("8080"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorShouldRejectBlankIp() {
        new ConnectionInfo("   ", 8080);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorShouldRejectNegativePort() {
        new ConnectionInfo("127.0.0.1", -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorShouldRejectPortGreaterThan65535() {
        new ConnectionInfo("127.0.0.1", 65536);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setBackupInfoShouldRejectCycle() {
        ConnectionInfo primary = new ConnectionInfo("127.0.0.1", 8080);
        ConnectionInfo backup = new ConnectionInfo("127.0.0.2", 8081);

        primary.setBackupInfo(backup);
        backup.setBackupInfo(primary);
    }
}
