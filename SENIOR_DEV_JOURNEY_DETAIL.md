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