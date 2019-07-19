package com.spring.ioc;

import com.spring.ioc.bean.StudentFactory;
import com.spring.ioc.bean.ViewProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;

/**
 * Created by xin on 2019/4/22.
 */
@ComponentScan
public class IocMain {

    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(IocMain.class);
        System.out.println(ctx.getBean(ViewProperty.class));
        ctx.registerShutdownHook();
        ctx.getBeanNamesForType(StudentFactory.class, true, false);
    }
}
