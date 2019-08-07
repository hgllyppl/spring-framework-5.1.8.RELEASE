package com.spring.ioc.anno;

import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Created by xin on 2019/8/6.
 */
public class SpringApplicationCondition implements ConfigurationCondition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        try {
            Class.forName("com.spring.ioc.anno.SpringApplicationCondition");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }

    }

    @Override
    public ConfigurationPhase getConfigurationPhase() {
        return ConfigurationPhase.PARSE_CONFIGURATION;
    }
}
