package com.spring.jdbc.config;

import org.apache.tomcat.jdbc.pool.PoolConfiguration;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

/**
 * Created by xin on 2019/4/28.
 */
@Configuration
@PropertySource("classpath:application.properties")
@EnableTransactionManagement
public class DataSourceConfig {

    @Bean
    public PoolConfiguration poolConfiguration(@Value("${datasource.url}") String url,
                                               @Value("${datasource.driver}") String driver,
                                               @Value("${datasource.username}") String username,
                                               @Value("${datasource.password}") String password) {
        PoolConfiguration poolConf = new PoolProperties();
        poolConf.setUrl(url);
        poolConf.setDriverClassName(driver);
        poolConf.setUsername(username);
        poolConf.setPassword(password);
        return poolConf;
    }

    @Bean
    public DataSource dataSource(PoolConfiguration poolConfiguration) {
        return new org.apache.tomcat.jdbc.pool.DataSource(poolConfiguration);
    }

    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
