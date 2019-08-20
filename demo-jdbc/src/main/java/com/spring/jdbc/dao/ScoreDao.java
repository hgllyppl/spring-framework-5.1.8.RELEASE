package com.spring.jdbc.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

/**
 * Created by Xin.L on 2017/10/27.
 */
@Component
public class ScoreDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public Map<String, Object> queryScoreById(int id) {
        List<Map<String, Object>> list = jdbcTemplate.queryForList("SELECT * FROM score WHERE id=?", id);
        return CollectionUtils.isEmpty(list) ? null : list.get(0);
    }
}
