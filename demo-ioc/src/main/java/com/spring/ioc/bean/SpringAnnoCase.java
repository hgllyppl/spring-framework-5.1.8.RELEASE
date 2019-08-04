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

    /**
     * array 和 list 的注入要注意, beanFactory 并不是优先注入名字相匹配的 bean
     * 而是优先查找跟数组类型或泛型相符合的 bean, 如果找得到就将其组合成 array 和 list 返回
     * 找不到才去找跟名字相符合的 bean
     */
    @Autowired
    private String[] injectArray;

    @Autowired @Qualifier("injectList")
    private List<String> injectList;

    /**
     * map 注入注意, key 必须是 string, val 注入的是任意类型的集合
     */
    @Autowired
    private Map<String, Object> injectMap;

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
