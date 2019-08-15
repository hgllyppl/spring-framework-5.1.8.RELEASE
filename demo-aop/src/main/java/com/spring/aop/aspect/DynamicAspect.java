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

import static com.spring.aop.aspect.StaticAspect.AFTER;
import static com.spring.aop.aspect.StaticAspect.AROUND;
import static com.spring.aop.aspect.StaticAspect.BEFORE;
import static com.spring.aop.aspect.StaticAspect.TPL;

/**
 * Created by xin on 2019/4/28.
 */
@Aspect
//@Component
@Order(1)
public class DynamicAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicAspect.class);

    @Pointcut("@annotation(com.spring.aop.aspect.DynamicAnn)")
    public void dynamicPointcut() {
    }

    @Before("dynamicPointcut()")
    public void before() {
        LOGGER.info(TPL, DynamicAspect.class.getSimpleName(), BEFORE);
    }

    @After("dynamicPointcut()")
    public void after() {
        LOGGER.info(TPL, DynamicAspect.class.getSimpleName(), AFTER);
    }

    @Around("dynamicPointcut()")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        LOGGER.info(TPL, DynamicAspect.class.getSimpleName(), AROUND);
        Object object = pjp.proceed();
        return object;
    }
}
