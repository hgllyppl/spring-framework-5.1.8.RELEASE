package com.spring.schedule;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;

/**
 * Created by xin on 2019/4/29.
 */
@ComponentScan("com.spring.schedule")
public class ScheduleMain {

    public static void main(String[] args) {
        new AnnotationConfigApplicationContext(ScheduleMain.class);
    }
}
