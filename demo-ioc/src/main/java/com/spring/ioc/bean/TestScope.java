package com.spring.ioc.bean;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by xin on 2019/8/12.
 */
public class TestScope implements Scope, DisposableBean {

    public static final String TEST_SCOPE = "TestScope";

    private Map<String, Object> singleton = new ConcurrentHashMap<>();

    @Override
    public Object get(String name, ObjectFactory<?> objectFactory) {
        return singleton.put(name, objectFactory.getObject());
    }

    @Override
    public Object remove(String name) {
        return null;
    }

    @Override
    public void registerDestructionCallback(String name, Runnable callback) {
        System.out.println();
    }

    @Override
    public Object resolveContextualObject(String key) {
        return null;
    }

    @Override
    public String getConversationId() {
        return null;
    }

    @Override
    public void destroy() throws Exception {
        singleton.clear();
    }
}
