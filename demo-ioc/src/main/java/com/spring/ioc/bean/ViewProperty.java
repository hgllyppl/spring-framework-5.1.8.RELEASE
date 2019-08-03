package com.spring.ioc.bean;

import com.alibaba.fastjson.JSON;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Created by xin on 2019/7/11.
 */
@Component
public class ViewProperty {

    @Value("${yml.a}")
    private String ymlA;

    @Value("${prop.a}")
    private String propA;

    public String getPropA() {
        return propA;
    }

    public String getYmlA() {
        return ymlA;
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }
}
