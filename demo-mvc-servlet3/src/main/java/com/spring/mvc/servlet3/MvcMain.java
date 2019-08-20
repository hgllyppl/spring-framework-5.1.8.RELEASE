package com.spring.mvc.servlet3;

import com.google.common.collect.Sets;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.springframework.web.SpringServletContainerInitializer;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import java.io.File;
import java.io.IOException;

/**
 * Created by xin on 2019/4/30.
 */
public class MvcMain {

    public static void main(String[] args) throws LifecycleException, IOException, ServletException {
        // 创建 tomcat
        Tomcat tomcat = new Tomcat();
        tomcat.setBaseDir(createTempDir("tomcat-base").getAbsolutePath());
        tomcat.setPort(8080);
        // addWebapp 会自动侦测 ServletContainerInitializer
//        tomcat.addWebapp("", createTempDir("context-base").getAbsolutePath());
        // addContext 不会自动侦测 ServletContainerInitializer
        Context context = tomcat.addContext(tomcat.getHost(), "", createTempDir("context-base").getAbsolutePath());
        context.addServletContainerInitializer(new SpringServletContainerInitializer(), Sets.newHashSet(CustomWebAppInit.class));
        // 启动 tomcat
        tomcat.start();
        tomcat.getServer().await();
    }

    public static File createTempDir(String prefix) throws IOException {
        File temp = File.createTempFile(prefix + ".", "");
        if (temp.exists()) {
            temp.delete();
        }
        temp.mkdir();
        temp.deleteOnExit();
        return temp;
    }

    public static class ExtendsSpringWebAppInit extends AbstractAnnotationConfigDispatcherServletInitializer {
        @Override
        protected Class<?>[] getRootConfigClasses() {
            return new Class<?>[] {};
        }

        @Override
        protected Class<?>[] getServletConfigClasses() {
            return new Class<?>[] {MvcConfig.class};
        }

        @Override
        protected String[] getServletMappings() {
            return new String[]{"/"};
        }
    }

    public static class CustomWebAppInit implements WebApplicationInitializer {
        @Override
        public void onStartup(ServletContext servletCxt) {
            AnnotationConfigWebApplicationContext ctx = new AnnotationConfigWebApplicationContext();
            ctx.register(MvcConfig.class);
//            ctx.refresh();
            // add servlet
            DispatcherServlet servlet = new DispatcherServlet(ctx);
            ServletRegistration.Dynamic registration = servletCxt.addServlet("dispatcherServlet", servlet);
            registration.setLoadOnStartup(1);
            registration.addMapping("/");
        }
    }
}
