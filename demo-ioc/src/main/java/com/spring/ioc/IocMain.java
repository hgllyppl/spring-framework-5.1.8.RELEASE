package com.spring.ioc;

import com.spring.ioc.anno.EnableApplicationListener;
import com.spring.ioc.anno.SpringApplicationCondition;
import com.spring.ioc.bean.StudentFactory;
import com.spring.ioc.bean.ViewProperty;
import com.spring.ioc.config.DeferredImportSelectorConfig;
import com.spring.ioc.config.ImportBeanDefConfig;
import com.spring.ioc.config.ImportSelectorConfig;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;

/**
 * Created by xin on 2019/4/22.
 */
@ComponentScan
@EnableApplicationListener
@Import({ImportBeanDefConfig.class, ImportSelectorConfig.class, DeferredImportSelectorConfig.class})
@ImportResource("classpath:import.xml")
@PropertySource("classpath:application.properties")
@Conditional(SpringApplicationCondition.class)
public class IocMain {

    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(IocMain.class);
        ctx.registerShutdownHook();
        System.out.println(ctx.getBean("viewProperty", ViewProperty.class));
        ctx.getBeanNamesForType(StudentFactory.class, true, false);
    }
}
