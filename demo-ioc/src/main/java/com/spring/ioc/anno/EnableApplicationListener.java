package com.spring.ioc.anno;

import com.spring.ioc.config.ApplicationListenerConfig;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by xin on 2019/8/1.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(ApplicationListenerConfig.class)
public @interface EnableApplicationListener {

    String condition() default "EnableApplicationListener test";
}
