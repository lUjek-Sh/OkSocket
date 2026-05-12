package com.xuhao.didi.socket.server.impl;


import com.xuhao.didi.core.iocore.interfaces.IIOCoreOptions;
import com.xuhao.didi.core.protocol.IReaderProtocol;
import com.xuhao.didi.socket.common.interfaces.default_protocol.DefaultNormalReaderProtocol;

import java.nio.ByteOrder;

public class OkServerOptions implements IIOCoreOptions {
    private static boolean isDebug;
    /**
     * 服务器连接能力数
     */
    private int mConnectCapacity;
    /**
     * 写入Socket管道中的字节序
     */
    private ByteOrder mWriteOrder;
    /**
     * 从Socket管道中读取字节序时的字节序
     */
    private ByteOrder mReadOrder;
    /**
     * Socket通讯中,业务层定义的数据包包头格式
     */
    private IReaderProtocol mReaderProtocol;
    /**
     * 发送时单个数据包的总长度
     */
    private int mWritePackageBytes;
    private int mWritePackageQueueCapacity;
    /**
     * 读取时单次读取的缓存字节长度,数值越大,读取效率越高.但是相应的系统消耗将越大
     */
    private int mReadPackageBytes;
    /**
     * 最大读取数据的兆数(MB)<br>
     * 防止数据体过大的数据导致前端内存溢出.
     */
    private int mMaxReadDataMB;
    private boolean isServerSocketReuseAddress;
    private boolean isClientSocketReuseAddress;
    private boolean isClientSocketKeepAlive;
    private boolean isClientSocketTcpNoDelay;

    private OkServerOptions() {
    }

    public static void setIsDebug(boolean isDebug) {
        OkServerOptions.isDebug = isDebug;
    }

    public static OkServerOptions getDefault() {
        OkServerOptions okOptions = new OkServerOptions();
        okOptions.mReaderProtocol = new DefaultNormalReaderProtocol();
        okOptions.mConnectCapacity = 50;
        okOptions.mMaxReadDataMB = 10;
        okOptions.mWritePackageBytes = 100;
        okOptions.mWritePackageQueueCapacity = 256;
        okOptions.mReadPackageBytes = 50;
        okOptions.mReadOrder = ByteOrder.BIG_ENDIAN;
        okOptions.mWriteOrder = ByteOrder.BIG_ENDIAN;
        okOptions.isServerSocketReuseAddress = true;
        okOptions.isClientSocketReuseAddress = true;
        okOptions.isClientSocketKeepAlive = true;
        okOptions.isClientSocketTcpNoDelay = true;
        return okOptions;
    }

    public static class Builder {
        private OkServerOptions mOptions;

        public Builder() {
            this(OkServerOptions.getDefault());
        }

        public Builder(OkServerOptions options) {
            mOptions = copyOf(options);
        }

        public Builder setConnectCapacity(int connectCapacity) {
            mOptions.mConnectCapacity = connectCapacity;
            return this;
        }

        public Builder setWriteOrder(ByteOrder writeOrder) {
            mOptions.mWriteOrder = writeOrder;
            return this;
        }

        public Builder setReadOrder(ByteOrder readOrder) {
            mOptions.mReadOrder = readOrder;
            return this;
        }

        public Builder setReaderProtocol(IReaderProtocol readerProtocol) {
            mOptions.mReaderProtocol = readerProtocol;
            return this;
        }

        public Builder setWritePackageBytes(int writePackageBytes) {
            mOptions.mWritePackageBytes = writePackageBytes;
            return this;
        }

        public Builder setWritePackageQueueCapacity(int writePackageQueueCapacity) {
            mOptions.mWritePackageQueueCapacity = writePackageQueueCapacity;
            return this;
        }

        public Builder setReadPackageBytes(int readPackageBytes) {
            mOptions.mReadPackageBytes = readPackageBytes;
            return this;
        }

        public Builder setMaxReadDataMB(int maxReadDataMB) {
            mOptions.mMaxReadDataMB = maxReadDataMB;
            return this;
        }

        public Builder setServerSocketReuseAddress(boolean serverSocketReuseAddress) {
            mOptions.isServerSocketReuseAddress = serverSocketReuseAddress;
            return this;
        }

        public Builder setClientSocketReuseAddress(boolean clientSocketReuseAddress) {
            mOptions.isClientSocketReuseAddress = clientSocketReuseAddress;
            return this;
        }

        public Builder setClientSocketKeepAlive(boolean clientSocketKeepAlive) {
            mOptions.isClientSocketKeepAlive = clientSocketKeepAlive;
            return this;
        }

        public Builder setClientSocketTcpNoDelay(boolean clientSocketTcpNoDelay) {
            mOptions.isClientSocketTcpNoDelay = clientSocketTcpNoDelay;
            return this;
        }

        public OkServerOptions build() {
            validate(mOptions);
            return mOptions;
        }

        private static OkServerOptions copyOf(OkServerOptions source) {
            if (source == null) {
                return OkServerOptions.getDefault();
            }
            OkServerOptions copy = new OkServerOptions();
            copy.mReaderProtocol = source.mReaderProtocol;
            copy.mConnectCapacity = source.mConnectCapacity;
            copy.mMaxReadDataMB = source.mMaxReadDataMB;
            copy.mWritePackageBytes = source.mWritePackageBytes;
            copy.mWritePackageQueueCapacity = source.mWritePackageQueueCapacity;
            copy.mReadPackageBytes = source.mReadPackageBytes;
            copy.mReadOrder = source.mReadOrder;
            copy.mWriteOrder = source.mWriteOrder;
            copy.isServerSocketReuseAddress = source.isServerSocketReuseAddress;
            copy.isClientSocketReuseAddress = source.isClientSocketReuseAddress;
            copy.isClientSocketKeepAlive = source.isClientSocketKeepAlive;
            copy.isClientSocketTcpNoDelay = source.isClientSocketTcpNoDelay;
            return copy;
        }

        private static void validate(OkServerOptions options) {
            if (options == null) {
                throw new IllegalArgumentException("OkServerOptions can not be null");
            }
            if (options.mReaderProtocol == null) {
                throw new IllegalArgumentException("ReaderProtocol can not be null");
            }
            if (options.mWriteOrder == null) {
                throw new IllegalArgumentException("WriteByteOrder can not be null");
            }
            if (options.mReadOrder == null) {
                throw new IllegalArgumentException("ReadByteOrder can not be null");
            }
            if (options.mConnectCapacity <= 0) {
                throw new IllegalArgumentException("ConnectCapacity must be greater than 0");
            }
            if (options.mWritePackageBytes <= 0) {
                throw new IllegalArgumentException("WritePackageBytes must be greater than 0");
            }
            if (options.mWritePackageQueueCapacity <= 0) {
                throw new IllegalArgumentException("WritePackageQueueCapacity must be greater than 0");
            }
            if (options.mReadPackageBytes <= 0) {
                throw new IllegalArgumentException("ReadPackageBytes must be greater than 0");
            }
            if (options.mMaxReadDataMB <= 0) {
                throw new IllegalArgumentException("MaxReadDataMB must be greater than 0");
            }
        }
    }


    public int getConnectCapacity() {
        return mConnectCapacity;
    }

    public boolean isServerSocketReuseAddress() {
        return isServerSocketReuseAddress;
    }

    public boolean isClientSocketReuseAddress() {
        return isClientSocketReuseAddress;
    }

    public boolean isClientSocketKeepAlive() {
        return isClientSocketKeepAlive;
    }

    public boolean isClientSocketTcpNoDelay() {
        return isClientSocketTcpNoDelay;
    }

    @Override
    public ByteOrder getReadByteOrder() {
        return mReadOrder;
    }

    @Override
    public int getMaxReadDataMB() {
        return mMaxReadDataMB;
    }

    @Override
    public IReaderProtocol getReaderProtocol() {
        return mReaderProtocol;
    }

    @Override
    public ByteOrder getWriteByteOrder() {
        return mWriteOrder;
    }

    @Override
    public int getReadPackageBytes() {
        return mReadPackageBytes;
    }

    @Override
    public int getWritePackageBytes() {
        return mWritePackageBytes;
    }

    @Override
    public int getWritePackageQueueCapacity() {
        return mWritePackageQueueCapacity;
    }

    @Override
    public boolean isDebug() {
        return isDebug;
    }
}
