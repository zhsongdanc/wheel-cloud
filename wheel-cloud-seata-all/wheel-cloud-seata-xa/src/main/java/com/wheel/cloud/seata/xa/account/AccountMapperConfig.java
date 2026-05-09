package com.wheel.cloud.seata.xa.account;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan(basePackages = "com.wheel.cloud.seata.xa.account",
        sqlSessionFactoryRef = "accountSqlSessionFactory",
        sqlSessionTemplateRef = "accountSqlSessionTemplate")
public class AccountMapperConfig {}
