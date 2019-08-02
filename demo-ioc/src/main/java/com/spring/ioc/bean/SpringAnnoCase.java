package com.spring.ioc.bean;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;

/**
 * Created by xin on 2019/8/1.
 * @see AutowiredAnnotationBeanPostProcessor
 */
public class SpringAnnoCase {

    @Autowired
    private ViewProperty viewProperty;

    @Autowired
    public SpringAnnoCase setViewProperty(ViewProperty viewProperty) {
        this.viewProperty = viewProperty;
        return this;
    }
}
