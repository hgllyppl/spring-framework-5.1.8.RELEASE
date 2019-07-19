package com.spring.ioc.bean;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by xin on 2019/4/28.
 */
public class CircularReferenceA {

    @Autowired
    private CircularReferenceB referenceB;
}
