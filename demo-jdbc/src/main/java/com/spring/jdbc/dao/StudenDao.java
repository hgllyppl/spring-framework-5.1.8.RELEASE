package com.spring.jdbc.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by Xin.L on 2017/10/27.
 */
@Component
public class StudenDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Transactional
    public int addStudent(String name, String address, int sex, int age) {
        int effect = jdbcTemplate.update("INSERT INTO `student` (`name`, `address`, `sex`, `age`) VALUES (?, ?, ?, ?)", name, address, sex, age);
//        Integer.parseInt("test transaction");
        return effect;
    }
}
