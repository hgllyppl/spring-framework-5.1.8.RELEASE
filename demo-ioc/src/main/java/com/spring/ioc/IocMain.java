package com.spring.ioc;

import com.spring.ioc.anno.EnableApplicationListener;
import com.spring.ioc.anno.SpringApplicationCondition;
import com.spring.ioc.bean.StudentFactory;
import com.spring.ioc.config.BeanConfig;
import com.spring.ioc.config.DeferredImportSelectorConfig;
import com.spring.ioc.config.ImportBeanDefConfig;
import com.spring.ioc.config.ImportSelectorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.AbstractApplicationContext;

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

    private static final Logger LOGGER = LoggerFactory.getLogger(IocMain.class);

    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(IocMain.class);
        ctx.registerShutdownHook();
        // 获取工厂bean
        ctx.getBean(StudentFactory.class);
        ctx.getBean("&studentFactory");
        // 获取 bean
        ctx.getBean("studentFactory");
        // lookup
        BeanConfig beanConfig = ctx.getBean(BeanConfig.class);
        LOGGER.info(beanConfig.getStudent().toString());
        LOGGER.info(beanConfig.getStudent().toString());
        /**
         * 带 @Lazy 的 bean 和 普通 bean 并无什么大的不同
         * 仅仅只是在 ApplicationContext.finishBeanFactoryInitialization 时没有完成初始化
         * @see AbstractApplicationContext#finishBeanFactoryInitialization
         * @see DefaultListableBeanFactory#preInstantiateSingletons
         */
        LOGGER.info(ctx.getBean("studentSingleton").toString());
    }
}
