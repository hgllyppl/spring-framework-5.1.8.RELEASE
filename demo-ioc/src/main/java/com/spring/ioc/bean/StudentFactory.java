package com.spring.ioc.bean;

import org.springframework.beans.factory.SmartFactoryBean;
import org.springframework.stereotype.Component;

/**
 * Created by xin on 2019/4/24.
 */
@Component
public class StudentFactory implements SmartFactoryBean<Student> {

    private Student wang5 = new Student().setName("wang5");

    @Override
    public Student getObject() throws Exception {
        return wang5;
    }

    @Override
    public Class<?> getObjectType() {
        return Student.class;
    }

    @Override
    public boolean isEagerInit() {
        return true;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public boolean isPrototype() {
        return false;
    }
}
