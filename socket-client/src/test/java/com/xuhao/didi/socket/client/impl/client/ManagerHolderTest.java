package com.xuhao.didi.socket.client.impl.client;

import com.xuhao.didi.socket.client.sdk.client.ConnectionInfo;
import com.xuhao.didi.socket.client.sdk.client.OkSocketOptions;
import com.xuhao.didi.socket.client.sdk.client.connection.IConnectionManager;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

public class ManagerHolderTest {

    @Test
    public void okSocketOptionsBuilderShouldCopySourceOptions() {
        OkSocketOptions source = OkSocketOptions.getDefault();
        int sourceTimeout = source.getConnectTimeoutSecond();

        OkSocketOptions copy = new OkSocketOptions.Builder(source)
                .setConnectTimeoutSecond(sourceTimeout + 7)
                .build();

        assertNotSame(source, copy);
        assertEquals(sourceTimeout, source.getConnectTimeoutSecond());
        assertEquals(sourceTimeout + 7, copy.getConnectTimeoutSecond());
    }

    @Test
    public void holdenConnectionShouldBeCached() {
        ManagerHolder holder = ManagerHolder.getInstance();
        ConnectionInfo info = new ConnectionInfo("127.0.0.1", 33001);
        OkSocketOptions options = new OkSocketOptions.Builder()
                .setConnectionHolden(true)
                .build();

        IConnectionManager first = holder.getConnection(info, options);
        IConnectionManager second = holder.getConnection(info, options);

        assertSame(first, second);
    }

    @Test
    public void nonHoldenConnectionShouldNotBeCached() {
        ManagerHolder holder = ManagerHolder.getInstance();
        ConnectionInfo info = new ConnectionInfo("127.0.0.1", 33002);
        OkSocketOptions options = new OkSocketOptions.Builder()
                .setConnectionHolden(false)
                .build();

        IConnectionManager first = holder.getConnection(info, options);
        IConnectionManager second = holder.getConnection(info, options);

        assertNotSame(first, second);
        assertFalse(holder.getList().contains(first));
        assertFalse(holder.getList().contains(second));
    }

    @Test
    public void getListShouldEvictManagersMarkedAsNonHolden() {
        ManagerHolder holder = ManagerHolder.getInstance();
        ConnectionInfo info = new ConnectionInfo("127.0.0.1", 33003);
        IConnectionManager manager = holder.getConnection(info, OkSocketOptions.getDefault());

        manager.setIsConnectionHolder(false);

        assertFalse(holder.getList().contains(manager));
    }
}
