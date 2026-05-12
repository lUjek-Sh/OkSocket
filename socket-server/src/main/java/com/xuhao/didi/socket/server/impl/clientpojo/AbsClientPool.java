package com.xuhao.didi.socket.server.impl.clientpojo;

import com.xuhao.didi.socket.server.impl.OkServerOptions;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class AbsClientPool<K, V> {
    private final LinkedHashMap<K, V> mCache = new LinkedHashMap<>();
    private final int mCapacity;
    private final OkServerOptions.ClientPoolOverflowStrategy mOverflowStrategy;

    public AbsClientPool(int capacity, OkServerOptions.ClientPoolOverflowStrategy overflowStrategy) {
        mCapacity = capacity;
        mOverflowStrategy = overflowStrategy;
    }

    synchronized void set(K key, V value) {
        V old = mCache.get(key);
        if (old != null) {
            onCacheDuplicate(key, old);
        }
        if (mCache.containsKey(key)) {
            return;
        }

        if (mCapacity == mCache.size()) {
            if (mOverflowStrategy == OkServerOptions.ClientPoolOverflowStrategy.EVICT_OLDEST_CLIENT) {
                Map.Entry<K, V> oldest = getHead();
                if (oldest != null) {
                    mCache.remove(oldest.getKey());
                    onCacheEvicted(oldest.getKey(), oldest.getValue(), key, value);
                }
            } else {
                onCacheRejected(key, value);
                return;
            }
        }

        if (mCapacity == mCache.size()) {
            return;
        }
        mCache.put(key, value);
    }

    synchronized V get(K key) {
        return mCache.get(key);
    }

    synchronized void remove(K key) {
        mCache.remove(key);
        if (mCache.isEmpty()) {
            onCacheEmpty();
        }
    }

    synchronized void removeAll() {
        mCache.clear();
    }

    synchronized int size() {
        return mCache.size();
    }

    synchronized void echoRun(Echo echo) {
        if (echo == null) {
            return;
        }
        Iterator<Map.Entry<K, V>> iterator = mCache.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<K, V> node = iterator.next();
            echo.onEcho(node.getKey(), node.getValue());
        }
    }

    interface Echo<K, V> {
        void onEcho(K key, V value);
    }

    private Map.Entry<K, V> getHead() {
        Iterator<Map.Entry<K, V>> iterator = mCache.entrySet().iterator();
        return iterator.hasNext() ? iterator.next() : null;
    }

    abstract void onCacheRejected(K key, V newOne);

    abstract void onCacheEvicted(K key, V oldOne, K incomingKey, V incomingValue);

    abstract void onCacheDuplicate(K key, V oldOne);

    abstract void onCacheEmpty();
}
