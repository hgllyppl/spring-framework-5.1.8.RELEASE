package com.spring.ioc.config;

import com.spring.ioc.bean.Student;
import org.springframework.context.annotation.Bean;

import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_SINGLETON;

public interface BeanConfigInterface {

    @Bean
    default Student interfaceStudent() {
        return new Student()
                .setName("interfaceStudent")
                .setScope(SCOPE_SINGLETON);
    }
}