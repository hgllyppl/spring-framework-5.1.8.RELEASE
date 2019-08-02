package com.spring.ioc.bean;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * Created by xin on 2019/4/28.
 */
public class BeanLifeCycle implements InitializingBean, DisposableBean {

    @PostConstruct
    public void init() {
        System.out.println("BeanLifeCycle inited");
    }

    @PreDestroy
    public void close() {
        System.out.println("BeanLifeCycle closed");
    }

    @Override
    public void destroy() throws Exception {
        close();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        init();
    }
}
