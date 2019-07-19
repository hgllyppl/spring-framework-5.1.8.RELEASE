package com.spring.ioc.bean;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.stereotype.Component;

/**
 * Created by xin on 2019/4/24.
 */
@Component("StudentFactory")
public class StudentFactory implements FactoryBean<Student> {

    @Override
    public Student getObject() throws Exception {
        return new Student()
                .setName("wang5");
    }

    @Override
    public Class<?> getObjectType() {
        return Student.class;
    }

    @Override
    public boolean isSingleton() {
        return false;
    }
}
