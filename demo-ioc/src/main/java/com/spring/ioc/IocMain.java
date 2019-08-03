package com.spring.ioc;

import com.spring.ioc.bean.StudentFactory;
import com.spring.ioc.bean.ViewProperty;
import com.spring.ioc.anno.EnableApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;

/**
 * Created by xin on 2019/4/22.
 */
@ComponentScan
@EnableApplicationListener
public class IocMain {

    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(IocMain.class);
        ctx.registerShutdownHook();
        System.out.println(ctx.getBean("viewProperty", ViewProperty.class));
        ctx.getBeanNamesForType(StudentFactory.class, true, false);
    }
}
