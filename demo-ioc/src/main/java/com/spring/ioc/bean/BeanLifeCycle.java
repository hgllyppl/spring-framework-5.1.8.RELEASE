package com.spring.ioc.bean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * Created by xin on 2019/4/28.
 */
public class BeanLifeCycle implements InitializingBean, DisposableBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(BeanLifeCycle.class);

    @PostConstruct
    public void init() {
        LOGGER.info("inited");
    }

    @PreDestroy
    public void close() {
        LOGGER.info("closed");
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
