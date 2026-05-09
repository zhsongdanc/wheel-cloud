package com.wheel.cloud.seata.at.account;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan(basePackages = "com.wheel.cloud.seata.at.account",
            sqlSessionFactoryRef = "accountSqlSessionFactory",
            sqlSessionTemplateRef = "accountSqlSessionTemplate")
public class AccountMapperConfig {
}
