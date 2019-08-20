package com.spring.ioc.bean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

/**
 * Created by xin on 2019/7/18.
 */
public class StartListener implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartListener.class);

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        LOGGER.info("ContextRefreshedEvent");
    }

    @EventListener
    public String eventListener(ContextRefreshedEvent event) {
        LOGGER.info("ContextRefreshedEvent");
        return "success";
    }

    @EventListener
    public void handleEventResult(String msg) {
        LOGGER.info(msg);
    }
}
