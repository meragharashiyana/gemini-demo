package com.example.gemini_demo;

import com.example.gemini_demo.service.HelloService;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.gemini_demo.aspect.MeasureTime;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    private static final Logger logger = LoggerFactory.getLogger(HelloController.class);

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final HelloService helloService;
    private final Counter dbHelloCounter;

    public HelloController(CircuitBreakerRegistry circuitBreakerRegistry, HelloService helloService, MeterRegistry meterRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.helloService = helloService;
        this.dbHelloCounter = Counter.builder("api.db.hello.count")
                                     .description("Number of times the /api/db-hello endpoint has been called")
                                     .register(meterRegistry);
    }

    @GetMapping("/api/db-hello")
    public String dbHello() {
        dbHelloCounter.increment();
        return helloService.getGreetingFromDb();
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
