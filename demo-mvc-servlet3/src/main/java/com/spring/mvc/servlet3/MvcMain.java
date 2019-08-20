package com.spring.mvc.servlet3;

import com.google.common.collect.Sets;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.springframework.web.SpringServletContainerInitializer;

import javax.servlet.ServletException;
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
        // addContext
        Context context = tomcat.addContext(tomcat.getHost(), "", createTempDir("context-base").getAbsolutePath());
        context.addServletContainerInitializer(new SpringServletContainerInitializer(), Sets.newHashSet(MvcConfig.class));
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
}
