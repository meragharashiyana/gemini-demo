package com.example.gemini_demo.config;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.util.Assert;

/**
 * A {@link CacheManager} that composes a fast L1 cache (e.g., Caffeine) and a durable L2 cache (e.g., Redis).
 * <p>
 * On cache reads, it first checks L1; if missing it checks L2 and then promotes the value into L1.
 * On cache writes, it updates both L1 and L2. On evict/clear, it applies to both layers.
 */
public class TwoLevelCacheManager implements CacheManager {

    private final CacheManager l1CacheManager;
    private final CacheManager l2CacheManager;

    public TwoLevelCacheManager(CacheManager l1CacheManager, CacheManager l2CacheManager) {
        Assert.notNull(l1CacheManager, "L1 CacheManager must not be null");
        Assert.notNull(l2CacheManager, "L2 CacheManager must not be null");
        this.l1CacheManager = l1CacheManager;
        this.l2CacheManager = l2CacheManager;
    }

    @Override
    public Cache getCache(String name) {
        Cache l1 = l1CacheManager.getCache(name);
        Cache l2 = l2CacheManager.getCache(name);

        if (l1 == null && l2 == null) {
            return null;
        }
        if (l1 == null) {
            return l2;
        }
        if (l2 == null) {
            return l1;
        }
        return new TwoLevelCache(name, l1, l2);
    }

    @Override
    public Collection<String> getCacheNames() {
        Set<String> names = new LinkedHashSet<>();
        names.addAll(l1CacheManager.getCacheNames());
        names.addAll(l2CacheManager.getCacheNames());
        return names;
    }
}
