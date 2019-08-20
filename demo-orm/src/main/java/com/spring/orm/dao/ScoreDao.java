package com.spring.orm.dao;

import com.spring.orm.dao.entity.Score;
import com.spring.orm.dao.mapper.ScoreMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by Xin.L on 2017/10/27.
 */
@Component
public class ScoreDao {

    @Autowired
    private ScoreMapper scoreMapper;

    public Score queryScoreById(int id) {
        return scoreMapper.selectByPrimaryKey(id);
    }
}
