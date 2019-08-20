package com.spring.mvc.servlet3;

import com.google.common.collect.Maps;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalTime;
import java.util.Map;

/**
 * Created by xin on 2019/4/29.
 */
@RestController
@RequestMapping("/mvc")
public class MvcController {

    @RequestMapping("hello")
    public Object hello(String name) {
        Map<String, Object> map = Maps.newHashMap();
        map.put("time", LocalTime.now().toString());
        map.put("ack", "hello, " + name);
        return map;
    }
}
