package com.wheel.cloud.hystrix.spring;

import com.wheel.cloud.hystrix.config.CircuitBreakerManager;
import com.wheel.cloud.hystrix.property.CommandProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CommandProperty.class)
@ConditionalOnClass(CircuitBreakerManager.class)
public class HystrixPropertiesConfiguration {
}
