package com.spring.ioc.config;

import com.google.common.collect.Lists;
import com.spring.ioc.bean.BeanLifeCycle;
import com.spring.ioc.bean.CircularReferenceA;
import com.spring.ioc.bean.CircularReferenceB;
import com.spring.ioc.bean.CustomSmartLifeCycle;
import com.spring.ioc.bean.JavaxAnnoCase;
import com.spring.ioc.bean.SpringAnnoCase;
import com.spring.ioc.bean.Student;
import com.spring.ioc.bean.StudentFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;

import java.util.List;

import static com.spring.ioc.bean.TestScope.TEST_SCOPE;
import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;
import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_SINGLETON;

/**
 * Created by xin on 2019/4/22.
 */
@Configuration
public class BeanConfig extends BeanConfigSuperClass implements BeanConfigInterface {

    private ApplicationContext applicationContext;

    public BeanConfig(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    //-------------------------作用域---------------------------------------
    @Bean
    @Lazy
    @Scope(SCOPE_SINGLETON)
    public Student studentSingleton() {
        return new Student()
                .setName("zhang3")
                .setScope(SCOPE_SINGLETON);
    }

    @Bean
    @Scope(SCOPE_PROTOTYPE)
    public Student studentPrototype() {
        return new Student()
                .setName("li4")
                .setScope(SCOPE_PROTOTYPE);
    }

    @Bean
    @Scope(TEST_SCOPE)
    public Student studentTestScope() {
        return new Student()
                .setName("xiangxiang")
                .setScope(TEST_SCOPE);
    }

    //-------------------------工厂bean---------------------------------------
    @Bean
    public <T> StudentFactory studentFactory(List<T> list) {
        return new StudentFactory();
    }

    //-------------------------循环引用---------------------------------------
    @Bean
    public CircularReferenceA circularReferenceA() {
        return new CircularReferenceA();
    }

    @Bean
    public CircularReferenceB circularReferenceB() {
        return new CircularReferenceB();
    }

    //-------------------------bean生命周期---------------------------------------
    @Bean(initMethod = "init", destroyMethod = "destroy")
    public BeanLifeCycle beanLifeCycle() {
        return new BeanLifeCycle();
    }

    //-------------------------SmartLifeCycle---------------------------------------
    @Bean
    public CustomSmartLifeCycle customSmartLifeCycle() {
        return new CustomSmartLifeCycle();
    }

    //-------------------------同名bean---------------------------------------
    @Bean
    public Student student() {
        return new Student().setName("qiangqiang");
    }

    @Bean
    public Student student(@Autowired(required = false) String name) {
        return new Student().setName(name);
    }

    //-------------------------autowire anno case---------------------------------------
    @Bean
    public JavaxAnnoCase javaxAnnoCase() {
        return new JavaxAnnoCase();
    }

    @Bean
    @DependsOn("javaxAnnoCase")
    public SpringAnnoCase springAnnoCase() {
        return new SpringAnnoCase();
    }

    @Bean
    public String[] injectArray() {
        return new String[] {"1"};
    }

    @Bean @Primary
    public List<String> injectList() {
        return Lists.newArrayList("1");
    }

    //-------------------------lookup case---------------------------------------
    @Lookup("studentPrototype")
    public Student getStudent() {
        return null;
    }
}
