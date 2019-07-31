package com.spring.ioc.bean;

import org.springframework.context.SmartLifecycle;

/**
 * Created by xin on 2019/7/30.
 */
public class CustomSmartLifeCycle implements SmartLifecycle {

    private volatile boolean running = false;

    @Override
    public void start() {
        running = true;
        System.out.println("CustomSmartLifeCycle start");
    }

    @Override
    public void stop() {
        running = false;
        System.out.println("CustomSmartLifeCycle stop");
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
