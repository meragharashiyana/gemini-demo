# Senior Developer Interview Questions

This document contains a collection of advanced interview questions and detailed answers related to the engineering practices implemented in this project.

---

## Step 7a. Two-Tier Cache (Hybrid L1/L2)

### 1. Can you explain the Two-Tier (L1/L2) Caching architecture you implemented?

**Interviewer:** "I see you've implemented a two-tier cache. Can you walk me through the design? What was the motivation, and how does it work?"

**My Response:**

Absolutely. The motivation for a two-tier cache was to get the best of both worlds: the raw speed of an in-memory cache and the consistency of a shared, distributed cache.

A simple, single-level cache using Caffeine (which we call **L1** or Level 1) is incredibly fast because it's just a `ConcurrentHashMap` within the application's own memory. However, in a distributed system with multiple instances of the application, each instance has its own separate L1 cache. This leads to a few problems:
1.  **Data Inconsistency:** One instance might have stale data that another instance has already updated.
2.  **Inefficiency:** The same data has to be fetched from the database and stored in the cache of every single instance, which is redundant.
3.  **Cold Starts:** When a new instance starts up, its cache is completely empty, leading to poor initial performance.

To solve this, we introduced a **L2** (Level 2) cache using Redis. Redis is an external, network-based cache that is shared by all application instances. This gives us a single source of cached truth.

Our final architecture is a hybrid or "two-tier" model that works like this:

**For a Cache Read (e.g., a `get` operation):**

1.  The application first checks the **L1 (Caffeine) cache**. This is the fastest possible path. If the data is there (an L1 hit), we return it immediately.
2.  If the data is not in L1 (an L1 miss), we then check the **L2 (Redis) cache**.
3.  If we get an L2 hit, we've found the data. But before returning it, we **promote** it by saving it into the L1 cache. This ensures that the *next* request for that same data will be a super-fast L1 hit.
4.  If it's an L2 miss as well, we finally go to the source of truth—the database.
5.  After fetching the data from the database, we populate both the L2 and L1 caches so that future requests for that data can be served from the cache.

**For a Cache Write or Eviction:**

The operation is applied to **both caches simultaneously** to keep them in sync. A `put` writes to L1 and L2, and an `evict` removes the entry from both.

### 2. How did you implement this in Spring Boot?

**Interviewer:** "That's a good high-level overview. Can you dive into the technical implementation? What key classes or Spring concepts did you use?"

**My Response:**

To implement this, we leveraged Spring's caching abstraction but extended it with a few custom components to handle the two-tier logic.

1.  **Conditional Configuration (`CacheConfig.java`):** The core of the integration is in our `@Configuration`. We created a single `CacheManager` bean, but its behavior changes based on a property in `application.properties` (`app.cache.redis.enabled`). If this property is false, the bean is just a standard `CaffeineCacheManager`. If it's true, it becomes our custom `TwoLevelCacheManager`. This provides flexibility to run with or without Redis.

2.  **Custom `CacheManager` (`TwoLevelCacheManager`):** Spring's `@Cacheable` annotation works with a single `CacheManager` bean. Since we have two underlying cache technologies (Caffeine and Redis), we needed a way to compose them. Our `TwoLevelCacheManager` implements Spring's `CacheManager` interface. Its primary job is to intercept requests for a specific cache (e.g., "greetingsHybrid") and wrap the underlying L1 (Caffeine) and L2 (Redis) cache objects into our custom `TwoLevelCache` object.

3.  **Custom `Cache` (`TwoLevelCache`):** This class is where the actual two-level logic lives. It implements Spring's `Cache` interface and holds references to the real L1 and L2 cache objects. We overrode key methods like `get`, `put`, and `evict`:
    *   The `get(key)` method implements the read-through logic I described: check L1, then check L2, then promote.
    *   The `put(key, value)` method simply calls `put` on both the L1 and L2 caches.

This approach allows us to seamlessly plug our custom two-tier logic into Spring's standard caching mechanism. From the service layer's perspective, it's as simple as adding `@Cacheable("greetingsHybrid")`. The framework, guided by our custom manager, handles the rest.

### 3. What are the trade-offs or challenges with this approach?

**Interviewer:** "This sounds powerful, but nothing comes for free. What are the potential downsides or challenges you need to be mindful of?"

**My Response:**

That's a great point. There are several important trade-offs:

1.  **Increased Complexity:** The most obvious trade-off is complexity. The code is more involved than a simple `@Cacheable`, and it adds another piece of infrastructure (Redis) that needs to be maintained, monitored, and scaled.

2.  **Serialization:** Anything stored in the L2 Redis cache must be `Serializable`. This forces us to be disciplined about our cacheable objects. While simple data types are fine, complex domain objects can introduce subtle serialization bugs or versioning issues if the class structure changes.

3.  **Cache Invalidation:** This is one of the hardest problems in Computer Science. Our current implementation uses a combination of Time-To-Live (TTL) and a manual REST endpoint (`/api/cache-clear`) to evict caches. This is effective but basic. In a more sophisticated system, a critical improvement would be to use Redis's Pub/Sub feature to broadcast invalidation messages. When data is updated in the database, a message could be published to a channel. All application instances would subscribe to this channel and, upon receiving a message, would know to evict that specific key from their local L1 cache. This prevents serving stale data from L1 after an L2 update.
