package com.xuhao.didi.socket.client.sdk.client;

import java.io.Serializable;
import java.util.Objects;

/**
 * 连接信息服务类
 * Created by xuhao on 2017/5/16.
 */
public final class ConnectionInfo implements Serializable, Cloneable {
    private static final long serialVersionUID = -1298081264440685718L;
    /**
     * IPV4地址
     */
    private final String mIp;
    /**
     * 连接服务器端口号
     */
    private final int mPort;
    /**
     * 当此IP地址Ping不通时的备用IP
     */
    private ConnectionInfo mBackupInfo;

    public ConnectionInfo(String ip, int port) {
        this.mIp = normalizeIp(ip);
        this.mPort = validatePort(port);
    }

    /**
     * 获取传入的IP地址
     *
     * @return ip地址
     */
    public String getIp() {
        return mIp;
    }

    /**
     * 获取传入的端口号
     *
     * @return 端口号
     */
    public int getPort() {
        return mPort;
    }

    /**
     * 获取备用的Ip和端口号
     *
     * @return 备用的端口号和IP地址
     */
    public ConnectionInfo getBackupInfo() {
        return mBackupInfo;
    }

    /**
     * 设置备用的IP和端口号,可以不设置
     *
     * @param backupInfo 备用的IP和端口号信息
     */
    public void setBackupInfo(ConnectionInfo backupInfo) {
        if (backupInfo != null) {
            ensureNoCycle(backupInfo);
        }
        mBackupInfo = backupInfo;
    }

    @Override
    public ConnectionInfo clone() {
        ConnectionInfo connectionInfo = new ConnectionInfo(mIp, mPort);
        if (mBackupInfo != null) {
            connectionInfo.setBackupInfo(mBackupInfo.clone());
        }
        return connectionInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (!(o instanceof ConnectionInfo)) { return false; }

        ConnectionInfo connectInfo = (ConnectionInfo) o;

        if (mPort != connectInfo.mPort) { return false; }
        return Objects.equals(mIp, connectInfo.mIp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mIp, mPort);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("ConnectionInfo{");
        builder.append("ip='").append(mIp).append('\'');
        builder.append(", port=").append(mPort);
        if (mBackupInfo != null) {
            builder.append(", backup=")
                    .append(mBackupInfo.mIp)
                    .append(':')
                    .append(mBackupInfo.mPort);
        }
        builder.append('}');
        return builder.toString();
    }

    private static String normalizeIp(String ip) {
        if (ip == null) {
            throw new IllegalArgumentException("ip can not be null");
        }
        String normalizedIp = ip.trim();
        if (normalizedIp.length() == 0) {
            throw new IllegalArgumentException("ip can not be empty");
        }
        return normalizedIp;
    }

    private static int validatePort(int port) {
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("port must be between 0 and 65535");
        }
        return port;
    }

    private void ensureNoCycle(ConnectionInfo backupInfo) {
        ConnectionInfo current = backupInfo;
        while (current != null) {
            if (current == this) {
                throw new IllegalArgumentException("backupInfo can not create a cycle");
            }
            current = current.mBackupInfo;
        }
    }
}
