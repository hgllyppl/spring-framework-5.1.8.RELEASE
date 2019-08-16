package com.spring.aop.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
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
//@Scope(ConfigurableListableBeanFactory.SCOPE_PROTOTYPE)
@Order(0)
public class StaticAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(StaticAspect.class);
    private static final String CLAZZ_NAME = StaticAspect.class.getSimpleName();

    public static final String TPL = "{}.{}";

    public static final String AFTER_THROWING = "afterThrowing";
    public static final String AFTER_RETURNING = "afterReturning";
    public static final String AFTER = "after";
    public static final String AROUND = "around";
    public static final String BEFORE = "before";

    @Pointcut("execution(public * com.spring.aop.bean.*.*(..))")
    public void staticPointcut() {
    }

    @AfterThrowing(value = "staticPointcut()", throwing = "ex")
    public void afterThrowing(Exception ex) {
        LOGGER.info(TPL, CLAZZ_NAME, AFTER_THROWING);
    }

    @AfterReturning(value = "staticPointcut()", returning = "returnVal")
    public void afterReturning(String returnVal) {
        LOGGER.info(TPL, CLAZZ_NAME, AFTER_RETURNING);
    }

    @After("staticPointcut()")
    public void after() {
        LOGGER.info(TPL, CLAZZ_NAME, AFTER);
    }

    @Around(value = "staticPointcut() && args(name)", argNames = "pjp, name")
    public Object around(ProceedingJoinPoint pjp, String name) throws Throwable {
        LOGGER.info(TPL, CLAZZ_NAME, AROUND);
        Object object = pjp.proceed();
        return object;
    }

    @Before("staticPointcut()")
    public void before() {
        LOGGER.info(TPL, CLAZZ_NAME, BEFORE);
    }
}
