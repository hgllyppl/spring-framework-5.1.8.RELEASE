package com.spring.orm;

import com.spring.orm.dao.ScoreDao;
import com.spring.orm.dao.StudenDao;
import com.spring.orm.dao.entity.Score;
import com.spring.orm.dao.entity.Student;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;

/**
 * Created by xin on 2019/4/22.
 */
@ComponentScan("com.spring.orm")
@MapperScan("com.spring.orm")
public class OrmMain {

    public static void main(String[] args) {
        ApplicationContext ctx = new AnnotationConfigApplicationContext(OrmMain.class);
        ScoreDao scoreDao = ctx.getBean(ScoreDao.class);
        Score score = scoreDao.queryScoreById(0);
        System.out.println(score);
        StudenDao studenDao = ctx.getBean(StudenDao.class);
        Student student = new Student();
        student.setName("l4");
        student.setAddress("addr-orm");
        student.setAge(1);
        student.setSex(18);
        studenDao.addStudent(student);
    }
}
