package com.spring.aop.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Created by xin on 2019/4/28.
 */
@Aspect
@Component
@Order(0)
public class StaticAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(StaticAspect.class);

    public static final String TPL = "{}.{}";
    public static final String BEFORE = "before";
    public static final String AFTER = "after";
    public static final String AROUND = "around";

    @Pointcut("execution(public * com.spring.aop.bean.*.*(..))")
    public void staticPointcut() {
    }

    @Before("staticPointcut()")
    public void before() {
        LOGGER.info(TPL, StaticAspect.class.getSimpleName(), BEFORE);
    }

    @After("staticPointcut()")
    public void after() {
        LOGGER.info(TPL, StaticAspect.class.getSimpleName(), AFTER);
    }

    @Around("staticPointcut()")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        LOGGER.info(TPL, StaticAspect.class.getSimpleName(), AROUND);
        Object object = pjp.proceed();
        return object;
    }
}
