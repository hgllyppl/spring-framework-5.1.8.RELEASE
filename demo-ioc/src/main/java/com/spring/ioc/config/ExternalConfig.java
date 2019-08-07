package com.spring.ioc.config;

import com.spring.ioc.bean.Student;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_SINGLETON;

/**
 * Created by xin on 2019/8/6.
 */
@Configuration
public class ExternalConfig {

    @Bean
    @Scope(SCOPE_SINGLETON)
    public Student externalStudent() {
        return new Student()
                .setName("externalStudent")
                .setScope(SCOPE_SINGLETON);
    }

    class InnerConfig {
        @Bean
        @Scope(SCOPE_SINGLETON)
        public Student innerStudent() {
            return new Student()
                    .setName("innerStudent")
                    .setScope(SCOPE_SINGLETON);
        }
    }
}
