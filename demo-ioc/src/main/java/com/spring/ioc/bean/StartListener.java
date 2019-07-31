package com.spring.ioc.bean;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

/**
 * Created by xin on 2019/7/18.
 */
public class StartListener implements ApplicationListener<ContextRefreshedEvent> {
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        System.out.println("StartListener ContextRefreshedEvent");
    }

    @EventListener
    public void eventListener(ContextRefreshedEvent event) {
        System.out.println("StartListener ContextRefreshedEvent");
    }
}
