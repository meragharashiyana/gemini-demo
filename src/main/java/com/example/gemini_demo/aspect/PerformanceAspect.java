package com.example.gemini_demo.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect     // Tells Spring: "This class contains AOP logic"
@Component  // Tells Spring: "Please manage this class (create an instance of it)"
public class PerformanceAspect {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceAspect.class);

    // @Around: This is the most powerful advice. It lets us run code BEFORE and AFTER the method.
    // The expression inside says: "Target any method annotated with @MeasureTime"
    @Around("@annotation(com.example.gemini_demo.aspect.MeasureTime)")
    public Object measureExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        
         // 1. BEFORE: We start the timer
        long start = System.currentTimeMillis();
        
        // 2. PROCEED: This is the most important line.
        // It tells Spring: "Go ahead and run the actual method (hello()) now."
        Object proceed = joinPoint.proceed(); // Execute the actual method
        
        // 3. AFTER: The method is done. We stop the timer.
        long executionTime = System.currentTimeMillis() - start;
        
        // 4. Log the result
        logger.info("{} executed in {}ms", joinPoint.getSignature(), executionTime);
        
        // 5. Return the result to the original caller (the browser/frontend)
        return proceed;
    }
}