package com.spring.orm.dao;

import com.spring.orm.dao.entity.Student;
import com.spring.orm.dao.mapper.StudentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by Xin.L on 2017/10/27.
 */
@Component
public class StudenDao {

    @Autowired
    private StudentMapper studentMapper;

    @Transactional
    public int addStudent(Student student) {
        int effect = studentMapper.insert(student);
        return effect;
    }
}
