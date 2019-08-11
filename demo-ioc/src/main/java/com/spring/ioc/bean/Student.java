package com.spring.ioc.bean;

import com.alibaba.fastjson.JSON;
import org.springframework.util.ObjectUtils;

/**
 * Created by xin on 2019/4/24.
 */
public class Student {

    private String id = ObjectUtils.getIdentityHexString(this);
    private String name;
    private String scope;

    public Student() {
    }

    public Student(String id, String name, String scope) {
        this.id = id;
        this.name = name;
        this.scope = scope;
    }

    public String getId() {
        return id;
    }

    public Student setId(String id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public Student setName(String name) {
        this.name = name;
        return this;
    }

    public String getScope() {
        return scope;
    }

    public Student setScope(String scope) {
        this.scope = scope;
        return this;
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }
}
