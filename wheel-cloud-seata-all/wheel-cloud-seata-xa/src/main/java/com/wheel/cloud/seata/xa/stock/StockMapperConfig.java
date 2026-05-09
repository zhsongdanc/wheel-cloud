package com.wheel.cloud.seata.xa.stock;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan(basePackages = "com.wheel.cloud.seata.xa.stock",
        sqlSessionFactoryRef = "stockSqlSessionFactory",
        sqlSessionTemplateRef = "stockSqlSessionTemplate")
public class StockMapperConfig {}
