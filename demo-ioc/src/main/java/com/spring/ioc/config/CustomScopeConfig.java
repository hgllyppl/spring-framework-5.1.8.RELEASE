package com.spring.ioc.config;

import com.spring.ioc.bean.TestScope;
import org.springframework.beans.factory.config.CustomScopeConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.spring.ioc.bean.TestScope.TEST_SCOPE;

/**
 * Created by xin on 2019/8/12.
 */
@Configuration
public class CustomScopeConfig {

    @Bean
    public TestScope testScope() {
        return new TestScope();
    }

    @Bean
    public CustomScopeConfigurer customScopeConfigurer(TestScope testScope) {
        CustomScopeConfigurer customScopeConfigurer = new CustomScopeConfigurer();
        customScopeConfigurer.addScope(TEST_SCOPE, testScope);
        return customScopeConfigurer;
    }
}
