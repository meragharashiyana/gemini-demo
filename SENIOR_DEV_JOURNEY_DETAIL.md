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