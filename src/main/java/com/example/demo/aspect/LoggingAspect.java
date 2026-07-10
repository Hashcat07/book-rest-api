package com.example.demo.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class LoggingAspect {

    @Pointcut("within(com.example.demo.controller..*)"+"|| within(com.example.demo.service..*)"+"|| within(com.example.demo.exception..*)")
    public void aspectMethods(){}

    @Before("aspectMethods()")
    public void logBefore(JoinPoint joinPoint){
        log.info("Before method {}",joinPoint);
    }

    @AfterThrowing(pointcut = "aspectMethods()", throwing = "ex")
    public void logAfterThrowing(JoinPoint joinPoint,Throwable ex){
        log.error("Method {} threw {}: {}",joinPoint.getSignature().toShortString(),ex.getClass().getSimpleName(),ex.getMessage());
    }

    @AfterReturning(pointcut = "aspectMethods()", returning = "result")
    void logAfterReturning(JoinPoint joinPoint,Object result){
        log.info("<< {} returned={}",
                joinPoint.getSignature().toShortString(), result);
    }
    @Around("aspectMethods()")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long elapsed = System.currentTimeMillis() - start;
        log.info("~~ {} executed in {}ms",
                joinPoint.getSignature().toShortString(), elapsed);
        return result;
    }
}
