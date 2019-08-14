package com.spring.aop.bean;

import com.spring.aop.aspect.DynamicAnn;
import com.spring.aop.aspect.DynamicAspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Created by xin on 2019/4/28.
 */
@Component
public class Student {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicAspect.class);

    @DynamicAnn
    public void sayHello() {
        LOGGER.info("hello, i'm a student");
    }
}
