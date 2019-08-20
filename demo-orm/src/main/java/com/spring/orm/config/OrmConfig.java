package com.spring.orm.config;

import org.apache.ibatis.session.AutoMappingBehavior;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.tomcat.jdbc.pool.PoolConfiguration;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;

import javax.sql.DataSource;

/**
 * Created by xin on 2019/4/28.
 */
@Configuration
public class OrmConfig {

    @Bean
    public PropertySourcesPlaceholderConfigurer placeholderConfigurer() {
        YamlPropertiesFactoryBean ymlFactory = new YamlPropertiesFactoryBean();
        ymlFactory.setResources(new ClassPathResource("application.yml"));
        PropertySourcesPlaceholderConfigurer sources = new PropertySourcesPlaceholderConfigurer();
        sources.setProperties(ymlFactory.getObject());
        return sources;
    }

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
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource,
                                               @Value("${mybatis.auto-mapping-behavior}") String autoMappingBehavior,
                                               @Value("${mybatis.map-underscore-to-camel-case}") boolean mapUnderscoreToCamelCase,
                                               @Value("${mybatis.default-statement-timeout}") int defaultStatementTimeout) throws Exception {
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        configuration.setAutoMappingBehavior("full".equalsIgnoreCase(autoMappingBehavior) ? AutoMappingBehavior.FULL : AutoMappingBehavior.PARTIAL);
        configuration.setMapUnderscoreToCamelCase(mapUnderscoreToCamelCase);
        configuration.setDefaultStatementTimeout(defaultStatementTimeout);

        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setConfiguration(configuration);
        return factory.getObject();
    }
}
