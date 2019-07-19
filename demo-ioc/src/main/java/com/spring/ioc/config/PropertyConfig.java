package com.spring.ioc.config;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;

/**
 * Created by xin on 2019/4/29.
 */
@Configuration
@PropertySource("classpath:application.properties")
public class PropertyConfig {

    @Bean
    public PropertySourcesPlaceholderConfigurer placeholderConfigurer() {
        YamlPropertiesFactoryBean ymlFactory = new YamlPropertiesFactoryBean();
        ymlFactory.setResources(new ClassPathResource("application.yml"));
        PropertySourcesPlaceholderConfigurer sources = new PropertySourcesPlaceholderConfigurer();
        sources.setProperties(ymlFactory.getObject());
        return sources;
    }
}
