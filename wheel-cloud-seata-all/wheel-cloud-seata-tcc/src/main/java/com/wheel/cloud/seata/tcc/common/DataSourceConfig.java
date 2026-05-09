package com.wheel.cloud.seata.tcc.common;

import com.alibaba.druid.pool.DruidDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

/**
 * TCC 模式不需要 DataSourceProxy，使用原生数据源。
 * TCC 的补偿逻辑完全由业务代码（Confirm/Cancel 方法）手动实现。
 */
@Configuration
public class DataSourceConfig {

    @Bean("orderDruidDs")
    @Primary
    @ConfigurationProperties("spring.datasource.order")
    public DruidDataSource orderDruidDs() { return new DruidDataSource(); }

    @Bean("stockDruidDs")
    @ConfigurationProperties("spring.datasource.stock")
    public DruidDataSource stockDruidDs() { return new DruidDataSource(); }

    @Bean("accountDruidDs")
    @ConfigurationProperties("spring.datasource.account")
    public DruidDataSource accountDruidDs() { return new DruidDataSource(); }

    @Bean("orderJdbc")
    @Primary
    public JdbcTemplate orderJdbc(@Qualifier("orderDruidDs") DruidDataSource ds) {
        return new JdbcTemplate(ds);
    }

    @Bean("stockJdbc")
    public JdbcTemplate stockJdbc(@Qualifier("stockDruidDs") DruidDataSource ds) {
        return new JdbcTemplate(ds);
    }

    @Bean("accountJdbc")
    public JdbcTemplate accountJdbc(@Qualifier("accountDruidDs") DruidDataSource ds) {
        return new JdbcTemplate(ds);
    }

    @Bean("orderTxManager")
    @Primary
    public DataSourceTransactionManager orderTxManager(@Qualifier("orderDruidDs") DruidDataSource ds) {
        return new DataSourceTransactionManager(ds);
    }

    @Bean("stockTxManager")
    public DataSourceTransactionManager stockTxManager(@Qualifier("stockDruidDs") DruidDataSource ds) {
        return new DataSourceTransactionManager(ds);
    }

    @Bean("accountTxManager")
    public DataSourceTransactionManager accountTxManager(@Qualifier("accountDruidDs") DruidDataSource ds) {
        return new DataSourceTransactionManager(ds);
    }
}
