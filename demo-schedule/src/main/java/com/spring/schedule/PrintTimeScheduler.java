package com.spring.schedule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Created by xin on 2018/8/30.
 */
@Component
@EnableScheduling
public class PrintTimeScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrintTimeScheduler.class);

    @Scheduled(cron = "1 * * * * ?")
    public void interval() {
        LOGGER.info("interval");
    }
}
