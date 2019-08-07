package com.spring.ioc.config;

import com.spring.ioc.bean.Student;
import org.springframework.context.annotation.Bean;

import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_SINGLETON;

/**
 * Created by xin on 2019/8/6.
 */
public class BeanConfigSuperClass {

    @Bean
    public Student superClassStudent() {
        return new Student()
                .setName("superClassStudent")
                .setScope(SCOPE_SINGLETON);
    }
}
