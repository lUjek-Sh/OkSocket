package com.xuhao.didi.socket.client.sdk.client;

import java.util.Arrays;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

/**
 * Created by Tony on 2017/12/27.
 */
public final class OkSocketSSLConfig {
    /**
     * 安全协议名称(缺省为 SSL)
     */
    private final String mProtocol;
    /**
     * 信任证书管理器(缺省为 X509)
     */
    private final TrustManager[] mTrustManagers;
    /**
     * 证书秘钥管理器(缺省为 null)
     */
    private final KeyManager[] mKeyManagers;
    /**
     * 自定义 SSLFactory(缺省为 null)
     */
    private final SSLSocketFactory mCustomSSLFactory;

    private OkSocketSSLConfig(Builder builder) {
        mProtocol = builder.mProtocol;
        mTrustManagers = copyOf(builder.mTrustManagers);
        mKeyManagers = copyOf(builder.mKeyManagers);
        mCustomSSLFactory = builder.mCustomSSLFactory;
    }

    public static class Builder {
        private String mProtocol;
        private TrustManager[] mTrustManagers;
        private KeyManager[] mKeyManagers;
        private SSLSocketFactory mCustomSSLFactory;

        public Builder setProtocol(String protocol) {
            mProtocol = protocol;
            return this;
        }

        public Builder setTrustManagers(TrustManager[] trustManagers) {
            mTrustManagers = copyOf(trustManagers);
            return this;
        }

        public Builder setKeyManagers(KeyManager[] keyManagers) {
            mKeyManagers = copyOf(keyManagers);
            return this;
        }

        public Builder setCustomSSLFactory(SSLSocketFactory customSSLFactory) {
            mCustomSSLFactory = customSSLFactory;
            return this;
        }

        public OkSocketSSLConfig build() {
            return new OkSocketSSLConfig(this);
        }
    }

    public KeyManager[] getKeyManagers() {
        return copyOf(mKeyManagers);
    }

    public String getProtocol() {
        return mProtocol;
    }

    public TrustManager[] getTrustManagers() {
        return copyOf(mTrustManagers);
    }

    public SSLSocketFactory getCustomSSLFactory() {
        return mCustomSSLFactory;
    }

    private static TrustManager[] copyOf(TrustManager[] source) {
        return source == null ? null : Arrays.copyOf(source, source.length);
    }

    private static KeyManager[] copyOf(KeyManager[] source) {
        return source == null ? null : Arrays.copyOf(source, source.length);
    }
}
