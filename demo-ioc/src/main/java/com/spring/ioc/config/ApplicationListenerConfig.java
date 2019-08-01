package com.spring.ioc.config;

import com.spring.ioc.anno.EnableApplicationListener;
import com.spring.ioc.bean.StartListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;

import java.util.Map;

/**
 * Created by xin on 2019/8/1.
 * {@link Configuration} 是必须的注解, 才能拿到 {@link EnableApplicationListener} 上的参数
 * @see ConfigurationClassPostProcessor#ImportAwareBeanPostProcessor
 */
@Configuration
public class ApplicationListenerConfig implements ImportAware {

    @Bean
    public StartListener startListener() {
        return new StartListener();
    }

    @Override
    public void setImportMetadata(AnnotationMetadata importMetadata) {
        Map<String, Object> map = importMetadata.getAnnotationAttributes(EnableApplicationListener.class.getName());
        AnnotationAttributes attrs = AnnotationAttributes.fromMap(map);
        System.out.println(attrs.getString("condition"));
    }
}
