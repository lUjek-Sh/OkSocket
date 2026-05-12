package com.xuhao.didi.socket.client.sdk.client;

import org.junit.Test;

import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

public class OkSocketSSLConfigTest {

    @Test
    public void buildShouldCopyInputArrays() {
        TrustManager[] trustManagers = new TrustManager[]{new NoOpTrustManager()};
        KeyManager[] keyManagers = new KeyManager[]{new NoOpKeyManager()};

        OkSocketSSLConfig config = new OkSocketSSLConfig.Builder()
                .setTrustManagers(trustManagers)
                .setKeyManagers(keyManagers)
                .build();

        trustManagers[0] = null;
        keyManagers[0] = null;

        assertSame(NoOpTrustManager.class, config.getTrustManagers()[0].getClass());
        assertSame(NoOpKeyManager.class, config.getKeyManagers()[0].getClass());
    }

    @Test
    public void gettersShouldReturnDefensiveCopies() {
        OkSocketSSLConfig config = new OkSocketSSLConfig.Builder()
                .setTrustManagers(new TrustManager[]{new NoOpTrustManager()})
                .setKeyManagers(new KeyManager[]{new NoOpKeyManager()})
                .build();

        TrustManager[] trustManagers = config.getTrustManagers();
        KeyManager[] keyManagers = config.getKeyManagers();

        assertNotSame(trustManagers, config.getTrustManagers());
        assertNotSame(keyManagers, config.getKeyManagers());

        trustManagers[0] = null;
        keyManagers[0] = null;

        assertSame(NoOpTrustManager.class, config.getTrustManagers()[0].getClass());
        assertSame(NoOpKeyManager.class, config.getKeyManagers()[0].getClass());
    }

    private static final class NoOpTrustManager implements TrustManager {
    }

    private static final class NoOpKeyManager implements KeyManager {
    }
}
