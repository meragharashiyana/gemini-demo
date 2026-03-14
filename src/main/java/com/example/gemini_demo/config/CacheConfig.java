package com.example.gemini_demo.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@Configuration
public class CacheConfig {

    private static final Logger logger = LoggerFactory.getLogger(CacheConfig.class);

    @Bean
    public Caffeine<Object, Object> caffeineConfig() {
        return Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(500)
                .recordStats();
    }

    @Bean
    public CacheManager cacheManager(
            Caffeine<Object, Object> caffeine,
            ObjectProvider<RedisConnectionFactory> redisConnectionFactory,
            @Value("${app.cache.redis.enabled:false}") boolean redisEnabled
    ) {
        // Include the hybrid cache so that it participates in L1 caching.
        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager("greetings", "users", "greetingsHybrid");
        caffeineCacheManager.setCaffeine(caffeine);

        if (!redisEnabled) {
            return caffeineCacheManager;
        }

        RedisConnectionFactory factory = redisConnectionFactory.getIfAvailable();
        if (factory == null) {
            logger.warn("Redis cache is enabled but no RedisConnectionFactory is available. Falling back to Caffeine only.");
            return caffeineCacheManager;
        }

        try {
            RedisCacheManager redisCacheManager = RedisCacheManager.builder(factory).build();
            logger.info("Enabled Redis as second-level cache (L2)");
            return new TwoLevelCacheManager(caffeineCacheManager, redisCacheManager);
        } catch (Exception e) {
            logger.warn("Failed to initialize Redis cache manager, falling back to Caffeine only", e);
            return caffeineCacheManager;
        }
    }
}
