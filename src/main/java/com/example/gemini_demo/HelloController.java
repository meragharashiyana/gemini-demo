package com.example.gemini_demo;

import com.example.gemini_demo.config.TwoLevelCache;
import com.example.gemini_demo.service.HelloService;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.gemini_demo.aspect.MeasureTime;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    private static final Logger logger = LoggerFactory.getLogger(HelloController.class);

    private static final String HYBRID_CACHE_KEY = "getHybridCachedGreetingFromDb";

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final HelloService helloService;
    private final Counter dbHelloCounter;
    private final Flyway flyway;
    private final CacheManager cacheManager;
    private final ObjectProvider<RedisConnectionFactory> redisConnectionFactoryProvider;
    private final boolean redisEnabledProperty;

    public HelloController(
            CircuitBreakerRegistry circuitBreakerRegistry,
            HelloService helloService,
            MeterRegistry meterRegistry,
            Flyway flyway,
            CacheManager cacheManager,
            ObjectProvider<RedisConnectionFactory> redisConnectionFactoryProvider,
            @org.springframework.beans.factory.annotation.Value("${app.cache.redis.enabled:false}") boolean redisEnabledProperty
    ) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.helloService = helloService;
        this.dbHelloCounter = Counter.builder("api.db.hello.count")
                                     .description("Number of times the /api/db-hello endpoint has been called")
                                     .register(meterRegistry);
        this.flyway = flyway;
        this.cacheManager = cacheManager;
        this.redisConnectionFactoryProvider = redisConnectionFactoryProvider;
        this.redisEnabledProperty = redisEnabledProperty;
    }

    @GetMapping("/api/db-hello")
    public String dbHello() {
        dbHelloCounter.increment();
        return helloService.getGreetingFromDb();
    }

    @GetMapping("/api/users")
    public java.util.List<com.example.gemini_demo.model.User> users() {
        return helloService.getAllUsers();
    }

    @GetMapping("/api/migrations")
    public Map<String, Object> migrations() {
        MigrationInfo current = flyway.info().current();
        MigrationInfo[] applied = flyway.info().applied();

        Map<String, Object> result = new HashMap<>();
        if (current != null) {
            Map<String, Object> currentMap = new HashMap<>();
            currentMap.put("version", current.getVersion() != null ? current.getVersion().toString() : null);
            currentMap.put("description", current.getDescription());
            currentMap.put("state", current.getState().getDisplayName());
            result.put("current", currentMap);
        } else {
            result.put("current", null);
        }

        result.put("applied", Arrays.stream(applied).map(m -> Map.of(
                "version", m.getVersion() != null ? m.getVersion().toString() : null,
                "description", m.getDescription(),
                "state", m.getState().getDisplayName()
        )).toList());

        return result;
    }

    @GetMapping("/api/cached-db-hello")
    public String cachedDbHello() {
        return helloService.getCachedGreetingFromDb();
    }

    @GetMapping("/api/hybrid-cached-db-hello")
    public String hybridCachedDbHello() {
        return helloService.getHybridCachedGreetingFromDb();
    }

    @GetMapping("/api/cached-users")
    public java.util.List<com.example.gemini_demo.model.User> cachedUsers() {
        return helloService.getCachedUsers();
    }

    @GetMapping("/api/cache-clear")
    public String clearCache() {
        helloService.clearCache();
        return "Caches cleared";
    }

    @GetMapping("/api/cache/l1-clear")
    public String clearL1CacheOnly() {
        Cache hybrid = cacheManager.getCache("greetingsHybrid");
        Cache l1 = hybrid instanceof TwoLevelCache ? ((TwoLevelCache) hybrid).getL1() : hybrid;
        if (l1 != null) {
            l1.clear();
            return "L1 cache (Caffeine) cleared for greetingsHybrid";
        }
        return "L1 cache not available";
    }

    @GetMapping("/api/cache/inspect")
    public Map<String, Object> inspectHybridCache() {
        Cache hybrid = cacheManager.getCache("greetingsHybrid");
        Cache l1 = hybrid;
        Cache l2 = null;
        boolean redisEnabled = false;

        if (hybrid instanceof TwoLevelCache) {
            TwoLevelCache tlc = (TwoLevelCache) hybrid;
            l1 = tlc.getL1();
            l2 = tlc.getL2();
            redisEnabled = true;
        }

        boolean redisConnectionFactoryPresent = false;
        boolean redisReachable = false;
        String redisPing = null;
        RedisConnectionFactory redisConnectionFactory = redisConnectionFactoryProvider.getIfAvailable();
        if (redisConnectionFactory != null) {
            redisConnectionFactoryPresent = true;
            try (RedisConnection connection = redisConnectionFactory.getConnection()) {
                redisPing = connection.ping();
                redisReachable = "PONG".equalsIgnoreCase(redisPing);
            } catch (Exception e) {
                logger.warn("Failed to ping Redis", e);
            }
        }

        Object l1Value = l1 != null ? l1.get(HYBRID_CACHE_KEY, Object.class) : null;
        Object l2Value = l2 != null ? l2.get(HYBRID_CACHE_KEY, Object.class) : null;

        Map<String, Object> result = new HashMap<>();
        result.put("key", HYBRID_CACHE_KEY);
        result.put("l1Present", l1Value != null);
        result.put("l1Value", l1Value);
        result.put("l2Present", l2Value != null);
        result.put("l2Value", l2Value);
        result.put("redisEnabled", redisEnabled);
        result.put("redisEnabledProperty", redisEnabledProperty);
        result.put("redisConnectionFactoryPresent", redisConnectionFactoryPresent);
        result.put("redisReachable", redisReachable);
        result.put("redisPing", redisPing);
        return result;
    }

    @GetMapping("/api/hello")
    @MeasureTime // <--- The Aspect will now intercept this call
    @Async("taskExecutor") // <--- Run this in a separate thread from our custom pool
    @CircuitBreaker(name = "backendA", fallbackMethod = "fallbackHello")
    public CompletableFuture<String> hello() throws InterruptedException {
        logger.info("HelloController's hello() method was called");
        
        // Simulate a failure 50% of the time to test the Circuit Breaker
        if (Math.random() > 0.5) {
            logger.error("Simulating a random failure!");
            throw new RuntimeException("Something went wrong inside the backend!");
        }

        // Simulate a slow "Senior level" complex calculation
        Thread.sleep(300); 
        return CompletableFuture.completedFuture("Hello from Spring Boot!");
    }

    // This method runs if hello() fails or if the Circuit is OPEN
    public CompletableFuture<String> fallbackHello(Throwable t) {
        logger.warn("Fallback triggered due to: {}", t.getMessage());
        return CompletableFuture.completedFuture("Fallback: Service is currently busy, please try again later.");
    }

    @GetMapping("/api/monitor")
    public String getCircuitBreakerState() {
        return circuitBreakerRegistry.circuitBreaker("backendA").getState().toString();
    }
}
