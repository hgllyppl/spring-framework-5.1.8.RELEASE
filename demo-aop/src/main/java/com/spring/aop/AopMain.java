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
@EnableAspectJAutoProxy(exposeProxy = true, proxyTargetClass = true)
public class AopMain {

    // TODO 切面初始化
    // TODO 构建代理 - 如何决定代理类型
    // TODO 循环依赖
    public static void main(String[] args) {
        ApplicationContext ctx = new AnnotationConfigApplicationContext(AopMain.class);
        Student student = ctx.getBean(Student.class);
        student.sayHello("z3");
    }
}
