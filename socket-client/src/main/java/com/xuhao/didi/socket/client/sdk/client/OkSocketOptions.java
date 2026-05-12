package com.xuhao.didi.socket.client.sdk.client;

import com.xuhao.didi.core.iocore.interfaces.IIOCoreOptions;
import com.xuhao.didi.core.protocol.IReaderProtocol;
import com.xuhao.didi.socket.client.impl.client.action.ActionDispatcher;
import com.xuhao.didi.socket.client.sdk.client.connection.AbsReconnectionManager;
import com.xuhao.didi.socket.client.sdk.client.connection.DefaultReconnectManager;
import com.xuhao.didi.socket.client.sdk.client.connection.abilities.IConfiguration;
import com.xuhao.didi.socket.common.interfaces.default_protocol.DefaultNormalReaderProtocol;

import java.nio.ByteOrder;

/**
 * OkSocket参数配置类<br>
 * Created by xuhao on 2017/5/16.
 */
public class OkSocketOptions implements IIOCoreOptions {
    /**
     * 框架是否是调试模式
     */
    private static boolean isDebug;
    /**
     * Socket通讯模式
     * <p>
     * 请注意:<br>
     * 阻塞式仅支持冷切换(断开后切换)<br>
     * 非阻塞式可以热切换<br>
     * </p>
     */
    private IOThreadMode mIOThreadMode;
    /**
     * 连接是否管理保存<br>
     * <p>
     * true:连接将会保存在管理器中,进行性能优化和断线重连<br>
     * false:不会保存在管理器中,对于已经保存的会进行删除,将不进行性能优化和断线重连.
     * </p>
     */
    private boolean isConnectionHolden;
    /**
     * 写入Socket管道中给服务器的字节序
     */
    private ByteOrder mWriteOrder;
    /**
     * 从Socket管道中读取字节序时的字节序
     */
    private ByteOrder mReadByteOrder;
    /**
     * Socket通讯中,业务层定义的数据包包头格式
     */
    private IReaderProtocol mReaderProtocol;
    /**
     * 发送给服务器时单个数据包的总长度
     */
    private int mWritePackageBytes;
    /**
     * 从服务器读取时单次读取的缓存字节长度,数值越大,读取效率越高.但是相应的系统消耗将越大
     */
    private int mReadPackageBytes;
    /**
     * 脉搏频率单位是毫秒
     */
    private long mPulseFrequency;
    /**
     * 脉搏丢失次数<br>
     * 大于或等于丢失次数时将断开该通道的连接<br>
     * 抛出{@link com.xuhao.didi.socket.client.impl.exceptions.DogDeadException}
     */
    private int mPulseFeedLoseTimes;
    /**
     * 连接超时时间(秒)
     */
    private int mConnectTimeoutSecond;
    private long mReconnectDelayMillis;
    private long mReconnectMaxDelayMillis;
    private double mReconnectDelayScale;
    private double mReconnectJitterRatio;
    private int mReconnectBackupSwitchThreshold;
    /**
     * 最大读取数据的兆数(MB)<br>
     * 防止服务器返回数据体过大的数据导致前端内存溢出.
     */
    private int mMaxReadDataMB;
    /**
     * 重新连接管理器
     */
    private AbsReconnectionManager mReconnectionManager;
    /**
     * 安全套接字层配置
     */
    private OkSocketSSLConfig mSSLConfig;
    /**
     * 套接字工厂
     */
    private OkSocketFactory mOkSocketFactory;
    private int mWritePackageQueueCapacity;
    private boolean isSocketReuseAddress;
    private boolean isSocketKeepAlive;
    private boolean isSocketTcpNoDelay;
    /**
     * 从独立线程进行回调.
     */
    private boolean isCallbackInIndependentThread;
    /**
     * 将分发放到handler中,外部需要传入HandlerToken并且调用Handler.post(runnable);
     */
    private ThreadModeToken mCallbackThreadModeToken;

    private OkSocketOptions() {
    }

    public static void setIsDebug(boolean isDebug) {
        OkSocketOptions.isDebug = isDebug;
    }

    public static abstract class ThreadModeToken {
        public abstract void handleCallbackEvent(ActionDispatcher.ActionRunnable runnable);
    }

    public static class Builder {
        private OkSocketOptions mOptions;

        public Builder() {
            this(OkSocketOptions.getDefault());
        }

        public Builder(IConfiguration configuration) {
            this(configuration == null ? null : configuration.getOption());
        }

        public Builder(OkSocketOptions okOptions) {
            mOptions = copyOf(okOptions);
        }

        /**
         * Socket通讯模式
         * <p>
         * 请注意:<br>
         * 阻塞式仅支持冷切换(断开后切换)<br>
         * 非阻塞式可以热切换<br>
         * </p>
         *
         * @param IOThreadMode {@link IOThreadMode}
         */
        public Builder setIOThreadMode(IOThreadMode IOThreadMode) {
            mOptions.mIOThreadMode = IOThreadMode;
            return this;
        }

        /**
         * 最大读取数据的兆数(MB)<br>
         * 防止服务器返回数据体过大的数据导致前端内存溢出<br>
         *
         * @param maxReadDataMB 兆字节为单位
         */
        public Builder setMaxReadDataMB(int maxReadDataMB) {
            mOptions.mMaxReadDataMB = maxReadDataMB;
            return this;
        }

        /**
         * 安全套接字层配置<br>
         *
         * @param SSLConfig {@link OkSocketSSLConfig}
         */
        public Builder setSSLConfig(OkSocketSSLConfig SSLConfig) {
            mOptions.mSSLConfig = SSLConfig;
            return this;
        }

        /**
         * Socket通讯中,业务层定义的数据包包头格式<br>
         * 默认的为{@link DefaultNormalReaderProtocol}<br>
         *
         * @param readerProtocol {@link IReaderProtocol} 通讯头协议
         */
        public Builder setReaderProtocol(IReaderProtocol readerProtocol) {
            mOptions.mReaderProtocol = readerProtocol;
            return this;
        }

        /**
         * 设置脉搏间隔频率<br>
         * 单位是毫秒<br>
         *
         * @param pulseFrequency 间隔毫秒数
         */

        public Builder setPulseFrequency(long pulseFrequency) {
            mOptions.mPulseFrequency = pulseFrequency;
            return this;
        }

        /**
         * 连接是否管理保存<br>
         * <p>
         * true:连接将会保存在管理器中,进行性能优化和断线重连<br>
         * false:不会保存在管理器中,对于已经保存的会进行删除,将不进行性能优化和断线重连.
         * </p>
         * 默认是 true
         *
         * @param connectionHolden true 讲此次链接交由OkSocket进行缓存管理,false 则不进行缓存管理.
         */
        public Builder setConnectionHolden(boolean connectionHolden) {
            mOptions.isConnectionHolden = connectionHolden;
            return this;
        }

        /**
         * 脉搏丢失次数<br>
         * 大于或等于丢失次数时将断开该通道的连接<br>
         * 抛出{@link com.xuhao.didi.socket.client.impl.exceptions.DogDeadException}<br>
         * 默认是5次
         *
         * @param pulseFeedLoseTimes 丢失心跳ACK的次数,例如5,当丢失3次时,自动断开.
         */
        public Builder setPulseFeedLoseTimes(int pulseFeedLoseTimes) {
            mOptions.mPulseFeedLoseTimes = pulseFeedLoseTimes;
            return this;
        }

        /**
         * 设置输出Socket管道中给服务器的字节序<br>
         * 默认是:大端字节序<br>
         *
         * @param writeOrder {@link ByteOrder} 字节序
         * @deprecated 请使用 {@link Builder#setWriteByteOrder(ByteOrder)}
         */
        public Builder setWriteOrder(ByteOrder writeOrder) {
            setWriteByteOrder(writeOrder);
            return this;
        }


        /**
         * 设置输出Socket管道中给服务器的字节序<br>
         * 默认是:大端字节序<br>
         *
         * @param writeOrder {@link ByteOrder} 字节序
         */
        public Builder setWriteByteOrder(ByteOrder writeOrder) {
            mOptions.mWriteOrder = writeOrder;
            return this;
        }

        /**
         * 设置输入Socket管道中读取时的字节序<br>
         * 默认是:大端字节序<br>
         *
         * @param readByteOrder {@link ByteOrder} 字节序
         */
        public Builder setReadByteOrder(ByteOrder readByteOrder) {
            mOptions.mReadByteOrder = readByteOrder;
            return this;
        }

        /**
         * 发送给服务器时单个数据包的总长度
         *
         * @param writePackageBytes 单个数据包的总大小
         */
        public Builder setWritePackageBytes(int writePackageBytes) {
            mOptions.mWritePackageBytes = writePackageBytes;
            return this;
        }

        public Builder setWritePackageQueueCapacity(int writePackageQueueCapacity) {
            mOptions.mWritePackageQueueCapacity = writePackageQueueCapacity;
            return this;
        }

        /**
         * 从服务器读取时单个数据包的总长度
         *
         * @param readPackageBytes 单个数据包的总大小
         */
        public Builder setReadPackageBytes(int readPackageBytes) {
            mOptions.mReadPackageBytes = readPackageBytes;
            return this;
        }

        /**
         * 设置连接超时时间,该超时时间是链路上从开始连接到连接上的时间
         *
         * @param connectTimeoutSecond 超时秒数,注意单位是秒
         * @return
         */
        public Builder setConnectTimeoutSecond(int connectTimeoutSecond) {
            mOptions.mConnectTimeoutSecond = connectTimeoutSecond;
            return this;
        }

        public Builder setReconnectDelayMillis(long reconnectDelayMillis) {
            mOptions.mReconnectDelayMillis = reconnectDelayMillis;
            return this;
        }

        public Builder setReconnectMaxDelayMillis(long reconnectMaxDelayMillis) {
            mOptions.mReconnectMaxDelayMillis = reconnectMaxDelayMillis;
            return this;
        }

        public Builder setReconnectDelayScale(double reconnectDelayScale) {
            mOptions.mReconnectDelayScale = reconnectDelayScale;
            return this;
        }

        public Builder setReconnectJitterRatio(double reconnectJitterRatio) {
            mOptions.mReconnectJitterRatio = reconnectJitterRatio;
            return this;
        }

        public Builder setReconnectBackupSwitchThreshold(int reconnectBackupSwitchThreshold) {
            mOptions.mReconnectBackupSwitchThreshold = reconnectBackupSwitchThreshold;
            return this;
        }

        /**
         * 设置断线重连的连接管理器<br>
         * 默认的连接管理器为{@link DefaultReconnectManager}<br>
         * 如果不需要断线重连请设置该参数为{@link com.xuhao.didi.socket.client.sdk.client.connection.NoneReconnect}
         *
         * @param reconnectionManager 断线重连管理器{@link AbsReconnectionManager}
         * @return
         */
        public Builder setReconnectionManager(
                AbsReconnectionManager reconnectionManager) {
            mOptions.mReconnectionManager = reconnectionManager;
            return this;
        }

        /**
         * 设置Socket工厂类,用于提供一个可以连接的Socket.
         * 可以是加密Socket,也可以是未加密的socket.
         *
         * @param factory socket工厂方法
         * @return
         */
        public Builder setSocketFactory(OkSocketFactory factory) {
            mOptions.mOkSocketFactory = factory;
            return this;
        }

        public Builder setSocketReuseAddress(boolean socketReuseAddress) {
            mOptions.isSocketReuseAddress = socketReuseAddress;
            return this;
        }

        public Builder setSocketKeepAlive(boolean socketKeepAlive) {
            mOptions.isSocketKeepAlive = socketKeepAlive;
            return this;
        }

        public Builder setSocketTcpNoDelay(boolean socketTcpNoDelay) {
            mOptions.isSocketTcpNoDelay = socketTcpNoDelay;
            return this;
        }

        /**
         * 设置回调在线程中,不是在UI线程中.
         *
         * @param threadModeToken 针对android设计,可以使回调在android的主线程中,
         *                        需要自己实现handleCallbackEvent方法.在方法中使用Handler.post(runnable)进行回调
         * @return
         */
        public Builder setCallbackThreadModeToken(ThreadModeToken threadModeToken) {
            mOptions.mCallbackThreadModeToken = threadModeToken;
            return this;
        }

        public OkSocketOptions build() {
            validate(mOptions);
            return mOptions;
        }

        private static OkSocketOptions copyOf(OkSocketOptions source) {
            if (source == null) {
                return OkSocketOptions.getDefault();
            }
            OkSocketOptions copy = new OkSocketOptions();
            copy.mIOThreadMode = source.mIOThreadMode;
            copy.isConnectionHolden = source.isConnectionHolden;
            copy.mWriteOrder = source.mWriteOrder;
            copy.mReadByteOrder = source.mReadByteOrder;
            copy.mReaderProtocol = source.mReaderProtocol;
            copy.mWritePackageBytes = source.mWritePackageBytes;
            copy.mWritePackageQueueCapacity = source.mWritePackageQueueCapacity;
            copy.mReadPackageBytes = source.mReadPackageBytes;
            copy.mPulseFrequency = source.mPulseFrequency;
            copy.mPulseFeedLoseTimes = source.mPulseFeedLoseTimes;
            copy.mConnectTimeoutSecond = source.mConnectTimeoutSecond;
            copy.mReconnectDelayMillis = source.mReconnectDelayMillis;
            copy.mReconnectMaxDelayMillis = source.mReconnectMaxDelayMillis;
            copy.mReconnectDelayScale = source.mReconnectDelayScale;
            copy.mReconnectJitterRatio = source.mReconnectJitterRatio;
            copy.mReconnectBackupSwitchThreshold = source.mReconnectBackupSwitchThreshold;
            copy.mMaxReadDataMB = source.mMaxReadDataMB;
            copy.mReconnectionManager = source.mReconnectionManager;
            copy.mSSLConfig = source.mSSLConfig;
            copy.mOkSocketFactory = source.mOkSocketFactory;
            copy.isSocketReuseAddress = source.isSocketReuseAddress;
            copy.isSocketKeepAlive = source.isSocketKeepAlive;
            copy.isSocketTcpNoDelay = source.isSocketTcpNoDelay;
            copy.isCallbackInIndependentThread = source.isCallbackInIndependentThread;
            copy.mCallbackThreadModeToken = source.mCallbackThreadModeToken;
            return copy;
        }

        private static void validate(OkSocketOptions options) {
            if (options == null) {
                throw new IllegalArgumentException("OkSocketOptions can not be null");
            }
            if (options.mIOThreadMode == null) {
                throw new IllegalArgumentException("IOThreadMode can not be null");
            }
            if (options.mReaderProtocol == null) {
                throw new IllegalArgumentException("ReaderProtocol can not be null");
            }
            if (options.mWriteOrder == null) {
                throw new IllegalArgumentException("WriteByteOrder can not be null");
            }
            if (options.mReadByteOrder == null) {
                throw new IllegalArgumentException("ReadByteOrder can not be null");
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
            if (options.mConnectTimeoutSecond < 0) {
                throw new IllegalArgumentException("ConnectTimeoutSecond can not be negative");
            }
            if (options.mReconnectDelayMillis <= 0) {
                throw new IllegalArgumentException("ReconnectDelayMillis must be greater than 0");
            }
            if (options.mReconnectMaxDelayMillis <= 0) {
                throw new IllegalArgumentException("ReconnectMaxDelayMillis must be greater than 0");
            }
            if (options.mReconnectMaxDelayMillis < options.mReconnectDelayMillis) {
                throw new IllegalArgumentException("ReconnectMaxDelayMillis must be greater than or equal to ReconnectDelayMillis");
            }
            if (options.mReconnectDelayScale < 1.0d) {
                throw new IllegalArgumentException("ReconnectDelayScale must be greater than or equal to 1.0");
            }
            if (options.mReconnectJitterRatio < 0.0d || options.mReconnectJitterRatio > 1.0d) {
                throw new IllegalArgumentException("ReconnectJitterRatio must be between 0 and 1");
            }
            if (options.mReconnectBackupSwitchThreshold < 0) {
                throw new IllegalArgumentException("ReconnectBackupSwitchThreshold can not be negative");
            }
            if (options.mMaxReadDataMB <= 0) {
                throw new IllegalArgumentException("MaxReadDataMB must be greater than 0");
            }
            if (options.mPulseFrequency < 0) {
                throw new IllegalArgumentException("PulseFrequency can not be negative");
            }
            if (options.mPulseFeedLoseTimes == 0 || options.mPulseFeedLoseTimes < -1) {
                throw new IllegalArgumentException("PulseFeedLoseTimes must be -1 or greater than 0");
            }
        }
    }

    public IOThreadMode getIOThreadMode() {
        return mIOThreadMode;
    }

    public long getPulseFrequency() {
        return mPulseFrequency;
    }

    public OkSocketSSLConfig getSSLConfig() {
        return mSSLConfig;
    }

    public OkSocketFactory getOkSocketFactory() {
        return mOkSocketFactory;
    }

    public boolean isSocketReuseAddress() {
        return isSocketReuseAddress;
    }

    public boolean isSocketKeepAlive() {
        return isSocketKeepAlive;
    }

    public boolean isSocketTcpNoDelay() {
        return isSocketTcpNoDelay;
    }

    public int getConnectTimeoutSecond() {
        return mConnectTimeoutSecond;
    }

    public long getReconnectDelayMillis() {
        return mReconnectDelayMillis;
    }

    public long getReconnectMaxDelayMillis() {
        return mReconnectMaxDelayMillis;
    }

    public double getReconnectDelayScale() {
        return mReconnectDelayScale;
    }

    public double getReconnectJitterRatio() {
        return mReconnectJitterRatio;
    }

    public int getReconnectBackupSwitchThreshold() {
        return mReconnectBackupSwitchThreshold;
    }

    public boolean isConnectionHolden() {
        return isConnectionHolden;
    }

    public int getPulseFeedLoseTimes() {
        return mPulseFeedLoseTimes;
    }

    public AbsReconnectionManager getReconnectionManager() {
        return mReconnectionManager;
    }

    public boolean isDebug() {
        return isDebug;
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
    public int getReadPackageBytes() {
        return mReadPackageBytes;
    }

    @Override
    public ByteOrder getWriteByteOrder() {
        return mWriteOrder;
    }

    @Override
    public IReaderProtocol getReaderProtocol() {
        return mReaderProtocol;
    }

    @Override
    public int getMaxReadDataMB() {
        return mMaxReadDataMB;
    }

    @Override
    public ByteOrder getReadByteOrder() {
        return mReadByteOrder;
    }

    public ThreadModeToken getCallbackThreadModeToken() {
        return mCallbackThreadModeToken;
    }

    public boolean isCallbackInIndependentThread() {
        return isCallbackInIndependentThread;
    }

    public static OkSocketOptions getDefault() {
        OkSocketOptions okOptions = new OkSocketOptions();
        okOptions.mPulseFrequency = 5 * 1000;
        okOptions.mIOThreadMode = IOThreadMode.DUPLEX;
        okOptions.mReaderProtocol = new DefaultNormalReaderProtocol();
        okOptions.mMaxReadDataMB = 5;
        okOptions.mConnectTimeoutSecond = 3;
        okOptions.mReconnectDelayMillis = 10 * 1000L;
        okOptions.mReconnectMaxDelayMillis = 10 * 1000L;
        okOptions.mReconnectDelayScale = 1.0d;
        okOptions.mReconnectJitterRatio = 0.0d;
        okOptions.mReconnectBackupSwitchThreshold = 12;
        okOptions.mWritePackageBytes = 100;
        okOptions.mWritePackageQueueCapacity = 256;
        okOptions.mReadPackageBytes = 50;
        okOptions.mReadByteOrder = ByteOrder.BIG_ENDIAN;
        okOptions.mWriteOrder = ByteOrder.BIG_ENDIAN;
        okOptions.isConnectionHolden = true;
        okOptions.mPulseFeedLoseTimes = 5;
        okOptions.mReconnectionManager = new DefaultReconnectManager();
        okOptions.mSSLConfig = null;
        okOptions.mOkSocketFactory = null;
        okOptions.isSocketReuseAddress = true;
        okOptions.isSocketKeepAlive = true;
        okOptions.isSocketTcpNoDelay = true;
        okOptions.isCallbackInIndependentThread = true;
        okOptions.mCallbackThreadModeToken = null;
        return okOptions;
    }

    /**
     * 线程模式
     */
    public enum IOThreadMode {
        /**
         * 单工通讯
         */
        SIMPLEX,
        /**
         * 双工通讯
         */
        DUPLEX;
    }
}
