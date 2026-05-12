package com.xuhao.didi.socket.server.impl.clientpojo;

import com.xuhao.didi.core.iocore.interfaces.ISendable;
import com.xuhao.didi.core.protocol.IReaderProtocol;
import com.xuhao.didi.socket.common.interfaces.common_interfacies.server.IClient;
import com.xuhao.didi.socket.common.interfaces.common_interfacies.server.IClientIOCallback;
import com.xuhao.didi.socket.server.exceptions.CacheException;
import com.xuhao.didi.socket.server.impl.OkServerOptions;

import org.junit.Test;

import java.io.Serializable;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ClientPoolImplTest {

    @Test
    public void rejectStrategyShouldKeepExistingClient() {
        ClientPoolImpl pool = new ClientPoolImpl(1, OkServerOptions.ClientPoolOverflowStrategy.REJECT_NEW_CLIENT);
        FakeClient first = new FakeClient("first");
        FakeClient second = new FakeClient("second");

        pool.cache(first);
        pool.cache(second);

        assertSame(first, pool.findByUniqueTag("first"));
        assertNull(pool.findByUniqueTag("second"));
        assertNull(first.disconnectCause);
        assertTrue(second.disconnectCause instanceof CacheException);
    }

    @Test
    public void evictOldestStrategyShouldReplaceExistingClient() {
        ClientPoolImpl pool = new ClientPoolImpl(1, OkServerOptions.ClientPoolOverflowStrategy.EVICT_OLDEST_CLIENT);
        FakeClient first = new FakeClient("first");
        FakeClient second = new FakeClient("second");

        pool.cache(first);
        pool.cache(second);

        assertNull(pool.findByUniqueTag("first"));
        assertSame(second, pool.findByUniqueTag("second"));
        assertTrue(first.disconnectCause instanceof CacheException);
        assertNull(second.disconnectCause);
    }

    private static final class FakeClient implements IClient {
        private final String uniqueTag;
        private Exception disconnectCause;

        private FakeClient(String uniqueTag) {
            this.uniqueTag = uniqueTag;
        }

        @Override
        public void disconnect(Exception e) {
            disconnectCause = e;
        }

        @Override
        public void disconnect() {
            disconnectCause = null;
        }

        @Override
        public IClient send(ISendable sendable) {
            return this;
        }

        @Override
        public String getHostIp() {
            return "127.0.0.1";
        }

        @Override
        public String getHostName() {
            return "localhost";
        }

        @Override
        public String getUniqueTag() {
            return uniqueTag;
        }

        @Override
        public void setReaderProtocol(IReaderProtocol protocol) {
        }

        @Override
        public void addIOCallback(IClientIOCallback clientIOCallback) {
        }

        @Override
        public void removeIOCallback(IClientIOCallback clientIOCallback) {
        }

        @Override
        public void removeAllIOCallback() {
        }
    }
}
