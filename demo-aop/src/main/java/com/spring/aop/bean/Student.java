package com.spring.aop.bean;

import com.spring.aop.aspect.DynamicAnn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * Created by xin on 2019/4/28.
 */
@Component
public class Student {

    private static final Logger LOGGER = LoggerFactory.getLogger(Student.class);

    @DynamicAnn
    public String sayHello(String name) {
        LOGGER.info("hello, i'm a student, my name is {}", name);
        return String.valueOf(new Random().nextInt());
    }
}
