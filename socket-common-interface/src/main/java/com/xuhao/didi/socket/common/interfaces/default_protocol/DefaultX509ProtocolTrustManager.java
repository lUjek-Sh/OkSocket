package com.xuhao.didi.socket.common.interfaces.default_protocol;

import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * Created by Tony on 2017/12/28.
 */

public class DefaultX509ProtocolTrustManager implements X509TrustManager {
    private final X509TrustManager mDelegate;

    public DefaultX509ProtocolTrustManager() {
        mDelegate = createSystemTrustManager();
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        mDelegate.checkClientTrusted(chain, authType);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        mDelegate.checkServerTrusted(chain, authType);
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return mDelegate.getAcceptedIssuers();
    }

    private static X509TrustManager createSystemTrustManager() {
        try {
            TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            factory.init((java.security.KeyStore) null);
            for (TrustManager trustManager : factory.getTrustManagers()) {
                if (trustManager instanceof X509TrustManager) {
                    return (X509TrustManager) trustManager;
                }
            }
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to load default trust manager", e);
        } catch (KeyStoreException e) {
            throw new IllegalStateException("Unable to initialize default trust manager", e);
        }
        throw new IllegalStateException("No X509TrustManager provided by default TrustManagerFactory");
    }
}
