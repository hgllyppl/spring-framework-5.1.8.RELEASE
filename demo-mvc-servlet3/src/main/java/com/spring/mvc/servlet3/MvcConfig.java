package com.spring.mvc.servlet3;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;

/**
 * Created by xin on 2019/4/30.
 */
@EnableWebMvc
@ComponentScan("com.spring.mvc.servlet3")
public class MvcConfig implements WebApplicationInitializer {

    @Override
    public void onStartup(ServletContext servletCxt) {
        AnnotationConfigWebApplicationContext ctx = new AnnotationConfigWebApplicationContext();
        ctx.register(MvcConfig.class);
        // add servlet
        DispatcherServlet servlet = new DispatcherServlet(ctx);
        ServletRegistration.Dynamic registration = servletCxt.addServlet("dispatcherServlet", servlet);
        registration.setLoadOnStartup(1);
        registration.addMapping("/");
    }
}
