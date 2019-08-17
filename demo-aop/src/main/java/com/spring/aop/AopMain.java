package com.spring.aop;

import com.spring.aop.bean.Student;
import org.springframework.aop.framework.DefaultAopProxyFactory;
import org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator;
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

    /**
     * TODO 切面初始化
     * @see AbstractAutoProxyCreator#postProcessBeforeInstantiation
     * @see AbstractAutoProxyCreator#postProcessAfterInitialization
     * TODO 使用何种代理方式
     * @see AbstractAutoProxyCreator#createProxy
     * @see DefaultAopProxyFactory#createAopProxy
     * TODO 循环依赖
     * @see AbstractAutoProxyCreator#getEarlyBeanReference
     * @see AbstractAutoProxyCreator#postProcessAfterInitialization
     */
    public static void main(String[] args) {
        ApplicationContext ctx = new AnnotationConfigApplicationContext(AopMain.class);
        Student student = ctx.getBean(Student.class);
        student.sayHello("z3");
    }
}
