package io.kidsfirst.core.utils;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
@ConditionalOnExpression("${application.aspect.enabled:true}")
public class ExecutionTimeTrackerAdvice {
    @Around("@annotation(io.kidsfirst.core.utils.Timed)")
    public Object executionTime(ProceedingJoinPoint point) throws Throwable {
        val startTime = System.currentTimeMillis();
        Object object = point.proceed();
        val endTime = System.currentTimeMillis();
        log.info("Class Name: " + point.getSignature().getDeclaringTypeName() + ". Method Name: " + point.getSignature().getName() + ". Time taken for Execution is : " + (endTime-startTime) +"ms");
        return object;
    }
}
