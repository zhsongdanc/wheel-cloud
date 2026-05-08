package com.wheel.cloud.seata.at.common;

import com.alibaba.druid.pool.DruidDataSource;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import io.seata.rm.datasource.DataSourceProxy;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;

/**
 * 三数据源 + Seata DataSourceProxy + 各自的 SqlSessionFactory。
 *
 * 每个业务库对应独立的 MapperScan，指向各自的 SqlSessionFactory，
 * 这样 MyBatis Plus 能正确路由 SQL 到对应库。
 * DataSourceProxy 是 Seata AT 模式的核心：它拦截 SQL、记录 undo_log，
 * 实现全局事务的反向补偿。
 */
@Configuration
public class DataSourceConfig {

    // ===================== order 数据源 =====================

    @Bean("orderDruidDs")
    @ConfigurationProperties("spring.datasource.order")
    public DruidDataSource orderDruidDs() {
        return new DruidDataSource();
    }

    @Bean("orderDataSource")
    @Primary
    public DataSourceProxy orderDataSource(@Qualifier("orderDruidDs") DruidDataSource ds) {
        return new DataSourceProxy(ds);
    }

    @Bean("orderSqlSessionFactory")
    @Primary
    public SqlSessionFactory orderSqlSessionFactory(@Qualifier("orderDataSource") DataSource ds) throws Exception {
        MybatisSqlSessionFactoryBean fb = new MybatisSqlSessionFactoryBean();
        fb.setDataSource(ds);
        fb.setMapperLocations();
        com.baomidou.mybatisplus.core.MybatisConfiguration cfg = new com.baomidou.mybatisplus.core.MybatisConfiguration();
        cfg.setMapUnderscoreToCamelCase(true);
        fb.setConfiguration(cfg);
        return fb.getObject();
    }

    @Bean("orderTxManager")
    @Primary
    public DataSourceTransactionManager orderTxManager(@Qualifier("orderDataSource") DataSource ds) {
        return new DataSourceTransactionManager(ds);
    }

    @Bean("orderSqlSessionTemplate")
    @Primary
    public SqlSessionTemplate orderSqlSessionTemplate(
            @Qualifier("orderSqlSessionFactory") SqlSessionFactory sf) {
        return new SqlSessionTemplate(sf);
    }

    // ===================== stock 数据源 =====================

    @Bean("stockDruidDs")
    @ConfigurationProperties("spring.datasource.stock")
    public DruidDataSource stockDruidDs() {
        return new DruidDataSource();
    }

    @Bean("stockDataSource")
    public DataSourceProxy stockDataSource(@Qualifier("stockDruidDs") DruidDataSource ds) {
        return new DataSourceProxy(ds);
    }

    @Bean("stockSqlSessionFactory")
    public SqlSessionFactory stockSqlSessionFactory(@Qualifier("stockDataSource") DataSource ds) throws Exception {
        MybatisSqlSessionFactoryBean fb = new MybatisSqlSessionFactoryBean();
        fb.setDataSource(ds);
        com.baomidou.mybatisplus.core.MybatisConfiguration cfg = new com.baomidou.mybatisplus.core.MybatisConfiguration();
        cfg.setMapUnderscoreToCamelCase(true);
        fb.setConfiguration(cfg);
        return fb.getObject();
    }

    @Bean("stockTxManager")
    public DataSourceTransactionManager stockTxManager(@Qualifier("stockDataSource") DataSource ds) {
        return new DataSourceTransactionManager(ds);
    }

    @Bean("stockSqlSessionTemplate")
    public SqlSessionTemplate stockSqlSessionTemplate(
            @Qualifier("stockSqlSessionFactory") SqlSessionFactory sf) {
        return new SqlSessionTemplate(sf);
    }

    // ===================== account 数据源 =====================

    @Bean("accountDruidDs")
    @ConfigurationProperties("spring.datasource.account")
    public DruidDataSource accountDruidDs() {
        return new DruidDataSource();
    }

    @Bean("accountDataSource")
    public DataSourceProxy accountDataSource(@Qualifier("accountDruidDs") DruidDataSource ds) {
        return new DataSourceProxy(ds);
    }

    @Bean("accountSqlSessionFactory")
    public SqlSessionFactory accountSqlSessionFactory(@Qualifier("accountDataSource") DataSource ds) throws Exception {
        MybatisSqlSessionFactoryBean fb = new MybatisSqlSessionFactoryBean();
        fb.setDataSource(ds);
        com.baomidou.mybatisplus.core.MybatisConfiguration cfg = new com.baomidou.mybatisplus.core.MybatisConfiguration();
        cfg.setMapUnderscoreToCamelCase(true);
        fb.setConfiguration(cfg);
        return fb.getObject();
    }

    @Bean("accountTxManager")
    public DataSourceTransactionManager accountTxManager(@Qualifier("accountDataSource") DataSource ds) {
        return new DataSourceTransactionManager(ds);
    }

    @Bean("accountSqlSessionTemplate")
    public SqlSessionTemplate accountSqlSessionTemplate(
            @Qualifier("accountSqlSessionFactory") SqlSessionFactory sf) {
        return new SqlSessionTemplate(sf);
    }
}
