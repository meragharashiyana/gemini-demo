package com.example.gemini_demo.config;

import java.util.concurrent.Callable;
import org.springframework.cache.Cache;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.lang.Nullable;

/**
 * A cache implementation that composes a fast L1 cache and a durable L2 cache.
 * <p>
 * Reads will attempt L1 first and then L2. When the value is found in L2, it is promoted into L1.
 * Writes are applied to both caches.
 */
public class TwoLevelCache implements Cache {

    private final String name;
    private final Cache l1;
    private final Cache l2;

    public TwoLevelCache(String name, Cache l1, Cache l2) {
        this.name = name;
        this.l1 = l1;
        this.l2 = l2;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object getNativeCache() {
        return l1.getNativeCache();
    }

    public Cache getL1() {
        return l1;
    }

    public Cache getL2() {
        return l2;
    }

    @Override
    @Nullable
    public ValueWrapper get(Object key) {
        ValueWrapper value = l1.get(key);
        if (value != null) {
            return value;
        }
        value = l2.get(key);
        if (value != null) {
            // Promote into L1 for faster subsequent reads
            l1.put(key, value.get());
        }
        return value;
    }

    @Override
    @Nullable
    public <T> T get(Object key, @Nullable Class<T> type) {
        T value = l1.get(key, type);
        if (value != null) {
            return value;
        }
        value = l2.get(key, type);
        if (value != null) {
            l1.put(key, value);
        }
        return value;
    }

    @Override
    @Nullable
    public <T> T get(Object key, Callable<T> valueLoader) {
        // Attempt L1 first
        ValueWrapper wrapper = l1.get(key);
        if (wrapper != null) {
            return (T) wrapper.get();
        }
        // Attempt L2
        wrapper = l2.get(key);
        if (wrapper != null) {
            // Promote to L1
            l1.put(key, wrapper.get());
            return (T) wrapper.get();
        }
        // Value not in either cache, compute it and store in both
        try {
            T value = valueLoader.call();
            put(key, value);
            return value;
        } catch (Exception ex) {
            throw new ValueRetrievalException(key, valueLoader, ex);
        }
    }

    @Override
    public void put(Object key, @Nullable Object value) {
        l1.put(key, value);
        l2.put(key, value);
    }

    @Override
    @Nullable
    public ValueWrapper putIfAbsent(Object key, @Nullable Object value) {
        ValueWrapper existing = get(key);
        if (existing != null) {
            return existing;
        }
        put(key, value);
        return null;
    }

    @Override
    public void evict(Object key) {
        l1.evict(key);
        l2.evict(key);
    }

    @Override
    public void clear() {
        l1.clear();
        l2.clear();
    }
}
