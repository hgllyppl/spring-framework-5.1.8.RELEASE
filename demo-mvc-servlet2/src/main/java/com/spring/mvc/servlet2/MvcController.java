package com.spring.mvc.servlet2;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by xin on 2019/4/29.
 */
@RestController
@RequestMapping("/mvc")
public class MvcController {

    @RequestMapping("hello")
    public String hello() {
        return "hello, mvc!";
    }
}
