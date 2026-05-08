package com.wheel.cloud.seata.at.order;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan(basePackages = "com.wheel.cloud.seata.at.order",
            sqlSessionFactoryRef = "orderSqlSessionFactory",
            sqlSessionTemplateRef = "orderSqlSessionTemplate")
public class OrderMapperConfig {
}
