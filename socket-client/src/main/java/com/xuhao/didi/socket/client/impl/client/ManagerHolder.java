package com.xuhao.didi.socket.client.impl.client;

import com.xuhao.didi.core.utils.SLog;
import com.xuhao.didi.socket.client.impl.client.abilities.IConnectionSwitchListener;
import com.xuhao.didi.socket.client.sdk.client.ConnectionInfo;
import com.xuhao.didi.socket.client.sdk.client.OkSocketOptions;
import com.xuhao.didi.socket.client.sdk.client.connection.IConnectionManager;
import com.xuhao.didi.socket.common.interfaces.common_interfacies.server.IServerManager;
import com.xuhao.didi.socket.common.interfaces.common_interfacies.server.IServerManagerPrivate;
import com.xuhao.didi.socket.common.interfaces.utils.SPIUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by xuhao on 2017/5/16.
 */
public class ManagerHolder {

    private final ConcurrentMap<ConnectionInfo, IConnectionManager> mConnectionManagerMap = new ConcurrentHashMap<>();

    private final ConcurrentMap<Integer, IServerManagerPrivate<?>> mServerManagerMap = new ConcurrentHashMap<>();

    private static class InstanceHolder {
        private static final ManagerHolder INSTANCE = new ManagerHolder();
    }

    public static ManagerHolder getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private ManagerHolder() {
        mConnectionManagerMap.clear();
    }

    public IServerManager getServer(int localPort) {
        IServerManagerPrivate<?> manager = mServerManagerMap.get(localPort);
        if (manager != null) {
            return manager;
        }

        IServerManagerPrivate<?> loadedManager = (IServerManagerPrivate<?>) SPIUtils.load(IServerManager.class);
        if (loadedManager == null) {
            String err = "Oksocket.Server() load error. Server plug-in are required!" +
                    " For details link to https://github.com/xuuhaoo/OkSocket";
            SLog.e(err);
            throw new IllegalStateException(err);
        }

        IServerManagerPrivate<?> cachedManager = mServerManagerMap.putIfAbsent(localPort, loadedManager);
        if (cachedManager == null) {
            loadedManager.initServerPrivate(localPort);
            return loadedManager;
        }
        return cachedManager;
    }

    public IConnectionManager getConnection(ConnectionInfo info) {
        IConnectionManager manager = mConnectionManagerMap.get(info);
        if (manager == null) {
            return getConnection(info, OkSocketOptions.getDefault());
        }
        OkSocketOptions options = manager.getOption();
        if (options == null || !options.isConnectionHolden()) {
            mConnectionManagerMap.remove(info, manager);
            return getConnection(info, OkSocketOptions.getDefault());
        }
        return getConnection(info, options);
    }

    public IConnectionManager getConnection(ConnectionInfo info, OkSocketOptions okOptions) {
        OkSocketOptions options = okOptions == null ? OkSocketOptions.getDefault() : okOptions;
        if (!options.isConnectionHolden()) {
            mConnectionManagerMap.remove(info);
            return createConnectionManager(info, options, false);
        }

        IConnectionManager manager = mConnectionManagerMap.get(info);
        if (manager != null) {
            manager.option(options);
            return manager;
        }

        AbsConnectionManager newManager = createConnectionManager(info, options, true);
        IConnectionManager cachedManager = mConnectionManagerMap.putIfAbsent(info, newManager);
        if (cachedManager != null) {
            cachedManager.option(options);
            return cachedManager;
        }
        return newManager;
    }

    private AbsConnectionManager createConnectionManager(ConnectionInfo info, OkSocketOptions okOptions, boolean cacheEnabled) {
        AbsConnectionManager manager = new ConnectionManagerImpl(info);
        manager.option(okOptions);
        manager.setOnConnectionSwitchListener(new IConnectionSwitchListener() {
            @Override
            public void onSwitchConnectionInfo(IConnectionManager manager, ConnectionInfo oldInfo,
                                               ConnectionInfo newInfo) {
                if (oldInfo != null) {
                    mConnectionManagerMap.remove(oldInfo, manager);
                }
                if (cacheEnabled && manager.getOption() != null && manager.getOption().isConnectionHolden()) {
                    mConnectionManagerMap.put(newInfo, manager);
                }
            }
        });
        return manager;
    }

    protected List<IConnectionManager> getList() {
        List<IConnectionManager> list = new ArrayList<>();
        for (Map.Entry<ConnectionInfo, IConnectionManager> entry : mConnectionManagerMap.entrySet()) {
            IConnectionManager manager = entry.getValue();
            OkSocketOptions options = manager.getOption();
            if (options == null || !options.isConnectionHolden()) {
                mConnectionManagerMap.remove(entry.getKey(), manager);
                continue;
            }
            list.add(manager);
        }
        return list;
    }

}
