package com.spring.jdbc;

import com.spring.jdbc.dao.ScoreDao;
import com.spring.jdbc.dao.StudenDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import java.util.Map;

/**
 * Created by xin on 2019/4/22.
 */
@ComponentScan("com.spring.jdbc")
public class JdbcMain {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcMain.class);

    public static void main(String[] args) {
        ApplicationContext ctx = new AnnotationConfigApplicationContext(JdbcMain.class);
        ScoreDao scoreDao = ctx.getBean(ScoreDao.class);
        Map<String, Object> score = scoreDao.queryScoreById(0);
        LOGGER.info(score == null ? null : score.toString());
        StudenDao studenDao = ctx.getBean(StudenDao.class);
        studenDao.addStudent("z3", "addr-jdbc", 0, 18);
    }
}
