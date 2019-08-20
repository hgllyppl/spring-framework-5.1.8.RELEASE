package com.spring.jdbc.config;

import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.util.Properties;

/**
 * Created by xin on 2019/8/20.
 */
//@Configuration
public class DeclareTransactionConfig {

    @Bean
    public TransactionInterceptor declareTxAdvice() {
        Properties properties = new Properties();
        properties.setProperty("add*", "PROPAGATION_REQUIRED");
        properties.setProperty("insert*", "PROPAGATION_REQUIRED");
        properties.setProperty("update*", "PROPAGATION_REQUIRED");
        properties.setProperty("delete*", "PROPAGATION_REQUIRED");
        properties.setProperty("query*", "PROPAGATION_REQUIRED, readOnly");
        properties.setProperty("select*", "PROPAGATION_REQUIRED, readOnly");
        properties.setProperty("find*", "PROPAGATION_REQUIRED, readOnly");
        TransactionInterceptor transactionInterceptor = new TransactionInterceptor();
        transactionInterceptor.setTransactionAttributes(properties);
        return transactionInterceptor;
    }

    @Bean
    public AspectJExpressionPointcut declareTxPointcut() {
        AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
        pointcut.setExpression("execution(public * com.spring.jdbc.dao.*.*(..))");
        return pointcut;
    }

    @Bean
    public DefaultPointcutAdvisor declareTxAdvisor() {
        DefaultPointcutAdvisor advisor = new DefaultPointcutAdvisor(declareTxPointcut(), declareTxAdvice());
        return advisor;
    }
}
