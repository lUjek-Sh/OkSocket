package com.xuhao.didi.socket.common.interfaces.default_protocol;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

/**
 * Trust manager that accepts any X.509 certificate.
 *
 * <p>Use this only in controlled environments such as local development or tests.
 * It disables certificate validation and makes TLS vulnerable to man-in-the-middle attacks.</p>
 */
public class TrustAllX509TrustManager implements X509TrustManager {
    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        // Intentionally trust all client certificates.
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        // Intentionally trust all server certificates.
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }
}
