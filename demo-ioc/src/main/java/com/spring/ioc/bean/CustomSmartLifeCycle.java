package com.spring.ioc.bean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

/**
 * Created by xin on 2019/7/30.
 */
public class CustomSmartLifeCycle implements SmartLifecycle {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomSmartLifeCycle.class);

    private volatile boolean running = false;

    @Override
    public void start() {
        running = true;
        LOGGER.info("start");
    }

    @Override
    public void stop() {
        running = false;
        LOGGER.info("stop");
    }

    @Override
    public void stop(Runnable callback) {
        new Thread(() -> {
            stop();
            callback.run();
        }).start();
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
