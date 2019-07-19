package com.spring.ioc.bean;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Created by xin on 2019/7/11.
 */
@Component
public class ViewProperty {

    @Value("yml.a")
    private String ymlA;

    @Value("prop.a")
    private String propA;

}
