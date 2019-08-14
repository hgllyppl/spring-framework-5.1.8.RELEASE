package com.spring.aop;

import com.spring.aop.bean.Student;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Created by xin on 2019/4/22.
 */
@ComponentScan
@EnableAspectJAutoProxy
public class AopMain {

    // TODO 如何确定代理类型
    // TODO 如何完成代理

    // TODO 动/静态切点
    // TODO 如何实例化(反射、objenesis)

    // TODO 循环依赖
    // TODO SmartInstantiationAwareBeanPostProcessor
    public static void main(String[] args) {
        ApplicationContext ctx = new AnnotationConfigApplicationContext(AopMain.class);
        Student student = ctx.getBean(Student.class);
        student.sayHello();
    }
}
