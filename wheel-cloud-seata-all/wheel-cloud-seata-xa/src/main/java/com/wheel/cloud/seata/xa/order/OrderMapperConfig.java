package com.wheel.cloud.seata.xa.order;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan(basePackages = "com.wheel.cloud.seata.xa.order",
        sqlSessionFactoryRef = "orderSqlSessionFactory",
        sqlSessionTemplateRef = "orderSqlSessionTemplate")
public class OrderMapperConfig {}
