# Senior Java Developer Journey - Implementation Details

This document provides the technical implementation details, testing steps, and observation guides for the completed milestones in the Senior Developer Journey.

## 1. Aspect-Oriented Programming (AOP)

**Goal:** Decouple cross-cutting concerns (logging execution time) from business logic.

### Implementation Details
1.  **Annotation**: Created a custom annotation `@MeasureTime` in `src/main/java/com/example/gemini_demo/aspect/MeasureTime.java`.
2.  **Aspect**: Created `PerformanceAspect` in `src/main/java/com/example/gemini_demo/aspect/PerformanceAspect.java`.
    *   Uses `@Around` advice to intercept methods annotated with `@MeasureTime`.
    *   Calculates execution time: `System.currentTimeMillis()` before and after `joinPoint.proceed()`.
    *   Logs the duration using SLF4J.
3.  **Usage**: Applied `@MeasureTime` to the `hello()` method in `HelloController.java`.

### How to Verify
1.  Run the application: `mvn spring-boot:run`.
2.  Access the endpoint: `http://localhost:8080/api/hello` (or via frontend port 3000).
3.  **Observe Logs**: Check the terminal where the application is running. You should see logs similar to:
    ```text
    INFO ... c.e.g.aspect.PerformanceAspect : String com.example.gemini_demo.HelloController.hello() executed in 302ms
    ```

---

## 2. Concurrency & Async Execution

**Goal:** Non-blocking operations using `CompletableFuture` and custom Thread Pools to handle high concurrency.

### Implementation Details
1.  **Enable Async**: Added `@EnableAsync` to the main application class `GeminiDemoApplication.java`.
2.  **Thread Pool Config**: Created `AsyncConfig.java` to define a custom `ThreadPoolTaskExecutor` bean named `taskExecutor`.
    *   **Core Pool Size**: 2
    *   **Max Pool Size**: 5
    *   **Queue Capacity**: 500
    *   **Thread Name Prefix**: `GeminiAsync-`
    *   **Rejection Policy**: `CallerRunsPolicy` (Backpressure mechanism).
3.  **Controller Update**:
    *   Annotated `hello()` with `@Async("taskExecutor")`.
    *   Changed return type to `CompletableFuture<String>`.

### How to Verify
1.  Run the application.
2.  Access the endpoint: `http://localhost:8080/api/hello`.
3.  **Observe Logs**: Look at the thread name in the logs (if configured to show thread names) or add a log statement inside the controller.
    *   Standard Tomcat thread: `[nio-8080-exec-1]`
    *   Async Thread: `[GeminiAsync-1]`
    *   This confirms the heavy lifting (sleep) is happening in your custom pool, freeing up the Tomcat thread.

---

## 3. Resilience Patterns (Circuit Breaker)

**Goal:** Implement Circuit Breakers to fail fast and recover gracefully when the backend is unstable.

### Implementation Details
1.  **Dependency**: Added `resilience4j-spring-boot3` to `pom.xml`.
2.  **Configuration**: Configured rules in `application.properties`:
    *   `slidingWindowSize=10`: Records last 10 calls.
    *   `failureRateThreshold=50`: Opens circuit if 50% fail.
    *   `waitDurationInOpenState=10s`: Stays open for 10 seconds.
    *   `permittedNumberOfCallsInHalfOpenState=3`: Allows 3 test calls to check recovery.
3.  **Controller Logic**:
    *   Added `@CircuitBreaker(name = "backendA", fallbackMethod = "fallbackHello")`.
    *   Implemented `fallbackHello(Throwable t)` to return a polite error message.
    *   Added random failure logic (`Math.random() > 0.5`) to simulate instability.
    *   Added monitoring endpoint `/api/monitor` to check circuit state.

### How to Verify
1.  Run the application.
2.  **Trigger Failures**: Refresh `http://localhost:8080/api/hello` multiple times rapidly.
    *   You will see a mix of "Hello from Spring Boot!" and "Fallback: Service is currently busy...".
3.  **Trip the Circuit**: Continue until you hit >50% failure rate in the last 10 calls.
4.  **Observe Open State**:
    *   Check `http://localhost:8080/api/monitor`. It should say `OPEN`.
    *   All subsequent calls to `/api/hello` will immediately return the Fallback message without waiting (Fail Fast).
5.  **Recovery**: Wait 10 seconds, then try again. The circuit will move to `HALF_OPEN` and eventually `CLOSED` if calls succeed.

---

## 4. Integration Testing with Testcontainers

**Goal:** Replace in-memory DBs with real Dockerized databases for reliable integration tests.

### Implementation Details
1.  **Dependencies**: Added `spring-boot-starter-data-jpa`, `postgresql`, `testcontainers`, and `h2` to `pom.xml`.
    *   `spring-boot-starter-data-jpa`: For database access.
    *   `postgresql`: The production database driver.
    *   `testcontainers`: To manage Docker containers in tests. @Testcontainers annotation is powerfull Java library allows to manager Docker containers from tests code.
    *   `h2`: As an in-memory database for tests that don't need a real database.
2.  **Integration Test**: Created `HelloControllerIntegrationTest.java`.
    *   `@Testcontainers`: Enables Testcontainers support.
    *   `@Container`: Defines a `PostgreSQLContainer` that will be started before the tests.
    *   `@DynamicPropertySource`: Dynamically sets the `spring.datasource.*` properties to point to the running Testcontainer as testCOntainer starts on some random ports.
    *   The test uses `MockMvc` to call the `/api/hello` endpoint and asserts the response, handling the asynchronous `CompletableFuture` and the random failure of the controller.
3.  **Unit/Component Tests**: The existing tests (`GeminiDemoApplicationTests` and `HelloControllerTest`) now use the H2 in-memory database, which is auto-configured by Spring Boot.

### How to Verify
1.  Run the tests: `mvnw test` (or `./mvnw.cmd test` on Windows).
2.  **Observe Logs**:
    *   You will see logs from Testcontainers starting the PostgreSQL Docker container.
    *   The tests will run, and you will see a "BUILD SUCCESS" message at the end.
    *   The `HelloControllerIntegrationTest` will connect to the PostgreSQL container, while the other tests will use the H2 in-memory database.

### Understanding the Test Logs

When you run the tests, you might notice two interesting things in the logs: the Spring Boot application starts multiple times, and two containers are created instead of just one. Here’s why that happens.

**1. Why does Spring Boot start three times?**

The Spring Test framework is optimized for speed. It caches and reuses the application context between tests, but only if their configuration is identical. If a test has a different configuration, a new context must be created, which appears as a full Spring Boot start.

Our three test classes each have a unique configuration, forcing three separate starts:

*   **Start 1: `GeminiDemoApplicationTests`**
    *   **Configuration**: H2 Database + Real Web Server (`@SpringBootTest(webEnvironment = RANDOM_PORT)`)
    *   A new context is created for this test.

*   **Start 2: `HelloControllerIntegrationTest`**
    *   **Configuration**: **PostgreSQL** Database + Real Web Server (`@SpringBootTest(webEnvironment = RANDOM_PORT)` + `@DynamicPropertySource`)
    *   This test uses `@DynamicPropertySource` to point to the PostgreSQL Testcontainer, so its database config is different from the first test, requiring a new context.

*   **Start 3: `HelloControllerTest`**
    *   **Configuration**: H2 Database + **Mock Web Server** (`@SpringBootTest(webEnvironment = MOCK)`)
    *   This test uses a mock web environment, which is different from the other two tests that use a real server, thus requiring a third context.

**2. Why are two containers created (PostgreSQL and Ryuk)?**

You will see logs for two containers spinning up.

*   **`postgres:16-alpine`**: This is the container you defined in `HelloControllerIntegrationTest`. It's a real PostgreSQL database instance used for running your high-fidelity integration test.

*   **`testcontainers/ryuk:x.x.x`**: This is a small, mandatory helper container that Testcontainers starts automatically. Its only job is to act as a "resource reaper." It monitors the test execution and ensures that any containers started by Testcontainers (like your PostgreSQL instance) are automatically stopped and removed when the tests are finished, even if the JVM terminates unexpectedly. This prevents orphaned containers from being left on your system.

---

## 4a. Database Integration with MyBatis

**Goal:** Add a proper DAO layer using MyBatis to fetch data from a database, confirming end-to-end execution.

### Implementation Details
1.  **Dependencies**: Added `mybatis-spring-boot-starter` and `spring-boot-starter-jdbc` to `pom.xml`.
2.  **Database Schema**: Created `src/main/resources/schema.sql` to define a `GREETINGS` table and `src/main/resources/data.sql` to populate it with sample messages.
3.  **Domain Model**: A simple `Greeting.java` Pojo was created in the `com.example.gemini_demo.model` package.
4.  **Mapper Interface (DAO)**:
    *   Created `GreetingMapper.java` in the `com.example.gemini_demo.mapper` package.
    *   Annotated with `@Mapper` to be picked up by Spring.
    *   Defined a `getRandomGreeting()` method with an `@Select` annotation containing the SQL to fetch a random greeting from the H2 database (`SELECT * FROM greetings ORDER BY RAND() LIMIT 1`).
5.  **Service Layer**: The `HelloService` was updated to be injected with the `GreetingMapper` and a new method `getGreetingFromDb()` was added to call the mapper.
6.  **Controller Layer**: A new, separate endpoint `/api/db-hello` was added to `HelloController` to expose the new functionality without touching the existing endpoints.
7.  **Testing**:
    *   A new integration test `HelloControllerDbIntegrationTest.java` was created.
    *   It uses `@SpringBootTest` and `MockMvc` to call the `/api/db-hello` endpoint.
    *   It asserts that the HTTP status is OK and the response body is one of the messages seeded in `data.sql`, confirming the entire flow from controller to database.

### How to Verify
1.  Run the application: `mvn spring-boot:run`.
2.  Access the new endpoint: `http://localhost:8080/api/db-hello`.
3.  **Observe Response**: Each time you refresh the page, you should see one of the three greetings from the database:
    *   `Hello from the database!`
    *   `Database says hi!`
    *   `Greetings from H2!`
4.  **Run Tests**: Execute `mvnw.cmd test`. All tests, including the new `HelloControllerDbIntegrationTest`, should pass, proving no existing functionality was broken.

---

## 5. Observability & Monitoring

**Goal:** Introduce logging, metrics, and tracing to the application.

### Implementation Details
1.  **Dependencies**: Added `spring-boot-starter-actuator`, `micrometer-registry-prometheus`, `micrometer-tracing-bridge-brave`, and `zipkin-reporter-brave` to `pom.xml`.
2.  **Configuration**: Updated `application.properties` to:
    *   Expose the `health` and `prometheus` actuator endpoints. All endpoints are now served on the main application port (`8080`).
    *   Set an application name (`gemini-demo`) for easier identification in monitoring tools.
    *   Configure the logging pattern to include `traceId` and `spanId` for correlated logging.
    *   Enable tracing and set the sampling probability to `1.0` to capture all traces during development.
3.  **Monitoring Infrastructure**:
    *   Created a `docker-compose.yml` file to run Prometheus and Zipkin.
    *   Created a `prometheus.yml` configuration file to scrape metrics from the Spring Boot application's `/actuator/prometheus` endpoint. The target is set to `host.docker.internal:8080` to allow Prometheus running in a Docker container to access the application running on the host machine.
4.  **Custom Metric**:
    *   Injected `MeterRegistry` into `HelloController`.
    *   Created a `Counter` named `api.db.hello.count` to track the number of calls to the `/api/db-hello` endpoint.
    *   Incremented the counter every time the `dbHello()` method is called.

### How to Verify
1.  **Start Monitoring Infrastructure**:
    *   Open a terminal in the project root and run `docker-compose up -d`. This will start Prometheus and Zipkin in the background.
2.  **Run the Application**:
    *   In a separate terminal, run the Spring Boot application: `mvnw spring-boot:run`.
3.  **Generate Metrics and Traces**:
    *   Access the application's endpoints a few times using your browser or `curl`:
        *   `http://localhost:8080/api/hello`
        *   `http://localhost:8080/api/db-hello`
        *   `http://localhost:8080/actuator/prometheus` (You can visit this to see the raw metrics your app is exposing)
4.  **Verify Metrics in Prometheus**:
    *   Open Prometheus in your browser: `http://localhost:9090`.
    *   In the "Expression" input box, start typing `api_db_hello_count_total` and click "Execute".
    *   **Result**: You should see a line with a value representing how many times you hit the `/api/db-hello` endpoint.
    *   **Troubleshooting**:
        *   If you see no targets in Prometheus (`Status` -> `Targets`), it means Prometheus can't connect to your app. The most common issue is the `host.docker.internal` address.
        *   **On Windows or Mac with Docker Desktop**: `host.docker.internal` should work automatically.
        *   **On Linux**: You may need to add `--add-host=host.docker.internal:host-gateway` to the `command` section of the prometheus service in your `docker-compose.yml`. Or, you can replace `host.docker.internal` with your machine's actual IP address in `prometheus.yml`.
5.  **Verify Traces in Zipkin**:
    *   Open Zipkin in your browser: `http://localhost:9411`.
    *   Click the "Find Traces" button (magnifying glass).
    *   **Result**: You should see traces for the requests you made. Click on one to see the detailed span, showing the time spent in the `HelloController` and `HelloService`.
    *   **Troubleshooting**:
        *   If you see no traces, ensure the application has started successfully and that you have made requests to the API endpoints.
        *   Check the Spring Boot application logs to ensure there are no errors related to Zipkin connectivity.
6.  **Verify Correlated Logs**:
    *   Examine the console output of your running Spring Boot application. You should now see logs prefixed with the application name, a `traceId`, and a `spanId`.
    ```text
    INFO [gemini-demo,63f3e3e3e3e3e3e3,63f3e3e3e3e3e3e3] 24416 --- [nio-8080-exec-2] c.e.g.HelloController: HelloController's hello() method was called
    ```
    *   This `traceId` should match the one you see in Zipkin for the same request, allowing you to correlate a specific log message to a specific distributed trace.

---

## 6. Database Schema Migration (Flyway)

**Goal:** Version-control schema changes so changes are applied consistently across environments and tracked in source control.

### Implementation Details
1.  **Dependency**: Added `spring-boot-starter-flyway` to `pom.xml`.
2.  **Migration Script**: Created `src/main/resources/db/migration/V1__init.sql`.
    *   Defines the `greetings` table (same as before) and adds a new `users` table.
    *   Seeds both tables with initial data.
3.  **Disabled Spring SQL Initialization**: Set `spring.sql.init.mode=never` so Flyway is the sole source of schema initialization.
4.  **Flyway state tracking**: Flyway maintains a table called `flyway_schema_history` in the database. This table records which migrations have been applied so Flyway can safely run only the missing ones on subsequent app starts.
5.  **Application Code**:
    *   Added `User` model and `UserMapper`.
    *   Added `/api/users` endpoint to return seeded users.
    *   Added an integration test to verify the Flyway migration and the seeded data.

### How to Verify
1.  Start the app: `mvn spring-boot:run`.
2.  Confirm Flyway ran by looking for log output like `Flyway` and `Successfully applied 1 migration`.
3.  Exercise the new endpoints:
    *   `curl http://localhost:8080/api/users` — should return JSON for the seeded users (`alice`, `bob`).
    *   `curl http://localhost:8080/api/migrations` — should return Flyway migration status (current version + applied migrations).
4.  Run the test suite: `./mvnw.cmd test` (Windows) or `./mvnw test` (Unix).

---

## 7. Caching Strategies (Caffeine)

**Goal:** Add fast application-level caching for expensive DB calls and allow explicit cache eviction.

### Implementation Details
1.  **Dependencies**:
    * Added `spring-boot-starter-cache` and `caffeine` to `pom.xml`.
2.  **Enable caching**:
    * Added `@EnableCaching` in `GeminiDemoApplication`.
3.  **Cache configuration**:
    * Added `CacheConfig` with `Caffeine.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).maximumSize(500).recordStats()`.
    * Configured caches `greetings` and `users`.
4.  **Service Cache**:
    * Added `@Cacheable("greetings")` on `getCachedGreetingFromDb()` and `@Cacheable("users")` on `getCachedUsers()`.
    * Added `@CacheEvict(value = {"greetings", "users"}, allEntries = true)` on `clearCache()`.
5.  **Controller Endpoints**:
    * Added `/api/cached-db-hello` to return cached greeting.
    * Added `/api/cached-users` to return cached users.
    * Added `/api/cache-clear` to evict caches.
6.  **Frontend**:
    * Added UI buttons in `frontend/src/App.js` for cached greeting and cache clear.

### How to Verify
1.  Start Redis (needed for hybrid cache):
    * `docker-compose up -d redis`
    * Or on Windows: `start_docker.bat` (stops with `stop_docker.bat`).
2.  Start app (with Redis L2 enabled): `./mvnw spring-boot:run -Dskip.frontend -Dapp.cache.redis.enabled=true` (Windows: `./mvnw.cmd spring-boot:run -Dskip.frontend -Dapp.cache.redis.enabled=true` or `start.bat`).
3.  Use the frontend comparator buttons:
    * `Fetch from DB` (no cache) should call `/api/db-hello` and return a fresh result each time.
    * `Fetch Cached Greeting` should call `/api/cached-db-hello`; first call warms L1 cache, subsequent calls should be faster and return the same cached value.
    * `Fetch Hybrid Cached Greeting` should call `/api/hybrid-cached-db-hello`; when Redis is enabled, this uses L1 (Caffeine) + L2 (Redis) and lets you compare performance across the three methods.
4.  Call `/api/cache-clear`; then call `/api/cached-db-hello` and `/api/hybrid-cached-db-hello` again to verify both reload from the DB (and repopulate caches).
5.  To force the hybrid path to go to Redis (L2) and then back into L1, clear only the L1 cache:
    * `/api/cache/l1-clear` — clears only the Caffeine (L1) cache for the hybrid greeting. The next call to `/api/hybrid-cached-db-hello` will load from Redis (if present) and repopulate L1.
6.  To inspect whether L1/L2 contain the cached value:
    * `/api/cache/inspect` — returns whether the hybrid key exists in L1 and L2 and (when present) its current value.
7.  Check `/api/cached-users` and `/api/cache-clear` similarly.
8.  Run tests: `./mvnw.cmd test`.

---

## 7a. Two-Tier Cache (Hybrid L1/L2)

**Goal:** Add a shared L2 cache (Redis) to complement the fast, in-memory L1 cache (Caffeine), providing resilience and better performance in a distributed environment.

### Implementation Details
1.  **Dependencies**: Added `spring-boot-starter-data-redis` to `pom.xml`.
2.  **Configuration**:
    *   Added `app.cache.redis.enabled=false` to `application.properties` to make the L2 cache opt-in.
    *   Added Redis connection details (`spring.redis.host`, `spring.redis.port`).
3.  **Cache Manager Logic (`CacheConfig.java`)**:
    *   The `cacheManager` bean now conditionally creates a `TwoLevelCacheManager` if `app.cache.redis.enabled` is true.
    *   If Redis is disabled or unavailable, it gracefully falls back to the `CaffeineCacheManager` alone.
4.  **Custom Cache Implementation**:
    *   `TwoLevelCacheManager`: A custom `CacheManager` that wraps the L1 (Caffeine) and L2 (Redis) managers.
    *   `TwoLevelCache`: A custom `Cache` implementation that orchestrates the two levels:
        *   **Read (`get`)**: Checks L1 -> L2 -> Promote from L2 to L1 if found.
        *   **Write (`put`)**: Writes to both L1 and L2.
        *   **Evict (`evict`/`clear`)**: Invalidates both L1 and L2.
5.  **Service Layer**: A new method `getHybridCachedGreetingFromDb` was added to `HelloService` and annotated with `@Cacheable("greetingsHybrid")` to use the new two-level cache.
6.  **Controller & Frontend**: Added a new endpoint `/api/hybrid-cached-db-hello` and a corresponding button in the UI to specifically test the hybrid cache behavior and compare its performance. Debug endpoints `/api/cache/l1-clear` and `/api/cache/inspect` were also added.

### Bug Fix: Inefficient `get` in `TwoLevelCache`
*   **Problem**: The original `get(key, valueLoader)` method was inefficiently calling the two-level `get(key)` again instead of checking L1 and L2 directly.
*   **Fix**: The method was rewritten to check L1, then L2, and only call the `valueLoader` if the value was absent from both, preventing redundant lookups and potential deserialization issues.

### How to Verify
The verification steps are the same as for Step 7, with a focus on the "Hybrid Cached Greeting" button and the cache inspection endpoint.
1.  **Start Redis**: `docker-compose up -d redis`
2.  **Start App with Redis**: `mvnw spring-boot:run -Dapp.cache.redis.enabled=true`
3.  **Observe Behavior**:
    *   First click on "Fetch Hybrid Cached Greeting" will be slow (loads from DB).
    *   Subsequent clicks will be fast (loads from L1 Caffeine cache).
    *   Click "Clear L1 Cache".
    *   Click "Fetch Hybrid Cached Greeting" again. It should be faster than the first call (loads from L2 Redis cache) but slower than the L1 cache hit.
    *   Click "Clear All Cache". The next call will be slow again (loads from DB).
4.  **Inspect Cache**: Use the `/api/cache/inspect` endpoint to see the state of the L1 and L2 caches at each step.
