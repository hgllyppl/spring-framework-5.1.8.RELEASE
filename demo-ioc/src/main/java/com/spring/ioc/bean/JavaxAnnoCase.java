package com.spring.ioc.bean;

import org.springframework.context.annotation.CommonAnnotationBeanPostProcessor;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

/**
 * Created by xin on 2019/8/1.
 * @see CommonAnnotationBeanPostProcessor
 */
public class JavaxAnnoCase {

    @Resource
    private ViewProperty viewProperty;

    @Resource
    public JavaxAnnoCase setViewProperty(ViewProperty viewProperty) {
        this.viewProperty = viewProperty;
        return this;
    }

    @PostConstruct
    public void init() {
        System.out.println("JavaxAnnoCase inited");
    }

    @PreDestroy
    public void close() {
        System.out.println("JavaxAnnoCase closed");
    }
}
