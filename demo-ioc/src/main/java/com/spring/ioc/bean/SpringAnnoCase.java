package com.spring.ioc.bean;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created by xin on 2019/8/1.
 * @see AutowiredAnnotationBeanPostProcessor
 */
public class SpringAnnoCase {

    @Value("${yml.a}")
    private String ymlA;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private String[] injectArray;

    @Autowired
    private Map<String, Object> injectMap;

    @Autowired @Qualifier("injectList")
    private List<String> injectList;

    @Autowired
    private Optional<Object> optional;

    @Autowired
    private ObjectFactory<Object> objectFactory;

    @Autowired
    private ViewProperty viewProperty;

    @Autowired
    public void setViewProperty(ApplicationContext applicationContext, ViewProperty viewProperty) {
        this.applicationContext = applicationContext;
        this.viewProperty = viewProperty;
    }
}
