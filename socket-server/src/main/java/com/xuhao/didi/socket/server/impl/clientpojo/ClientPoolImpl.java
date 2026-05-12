package com.xuhao.didi.socket.server.impl.clientpojo;

import com.xuhao.didi.core.iocore.interfaces.ISendable;
import com.xuhao.didi.socket.common.interfaces.common_interfacies.server.IClient;
import com.xuhao.didi.socket.common.interfaces.common_interfacies.server.IClientPool;
import com.xuhao.didi.socket.server.exceptions.CacheException;
import com.xuhao.didi.socket.server.impl.OkServerOptions;

public class ClientPoolImpl extends AbsClientPool<String, IClient> implements IClientPool<IClient, String> {

    public ClientPoolImpl(int capacity, OkServerOptions.ClientPoolOverflowStrategy overflowStrategy) {
        super(capacity, overflowStrategy);
    }

    @Override
    public void cache(IClient client) {
        super.set(client.getUniqueTag(), client);
    }

    @Override
    public IClient findByUniqueTag(String tag) {
        return get(tag);
    }

    public void unCache(IClient iClient) {
        remove(iClient.getUniqueTag());
    }

    public void unCache(String key) {
        remove(key);
    }

    @Override
    public int size() {
        return super.size();
    }

    @Override
    public void sendToAll(final ISendable sendable) {
        echoRun(new Echo<String, IClient>() {
            @Override
            public void onEcho(String key, IClient value) {
                value.send(sendable);
            }
        });
    }

    public void serverDown(){
        echoRun(new Echo<String, IClient>(){
            @Override
            public void onEcho(String key, IClient value) {
                value.disconnect();
            }
        });
        removeAll();
    }

    @Override
    void onCacheRejected(String key, IClient newOne) {
        newOne.disconnect(new CacheException("cache is full,new client was rejected"));
    }

    @Override
    void onCacheEvicted(String key, IClient oldOne, String incomingKey, IClient incomingValue) {
        oldOne.disconnect(new CacheException("cache is full,oldest client was evicted"));
    }

    @Override
    void onCacheDuplicate(String key, IClient oldOne) {
        oldOne.disconnect(new CacheException("there are cached in this server.it need removed before new cache"));
        unCache(oldOne);
    }

    @Override
    public void onCacheEmpty() {
        //do nothing
    }
}
