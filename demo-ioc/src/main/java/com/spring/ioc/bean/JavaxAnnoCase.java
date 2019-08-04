package com.spring.ioc.bean;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.CommonAnnotationBeanPostProcessor;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

/**
 * Created by xin on 2019/8/1.
 * @see CommonAnnotationBeanPostProcessor
 */
public class JavaxAnnoCase {

    // 还可以动态替换资源名称, NBility
    @Resource(name = "${viewProperty}")
    private ViewProperty viewProperty;

    @Resource
    public JavaxAnnoCase setViewProperty(ViewProperty viewProperty) {
        this.viewProperty = viewProperty;
        return this;
    }

    /**
     * 此方法在 bean 属性注入之前被调用
     * 如果需要在 bean 属性注入之后进行初始化操作, 应该实现 {@link InitializingBean#afterPropertiesSet()}
     */
    @PostConstruct
    public void init() {
        System.out.println("JavaxAnnoCase inited");
    }

    /**
     * 此方法无 @PostConstruct 的禁忌
     * 但此方法也是在 {@link DisposableBean#destroy()} 之前被调用
     */
    @PreDestroy
    public void close() {
        System.out.println("JavaxAnnoCase closed");
    }
}
