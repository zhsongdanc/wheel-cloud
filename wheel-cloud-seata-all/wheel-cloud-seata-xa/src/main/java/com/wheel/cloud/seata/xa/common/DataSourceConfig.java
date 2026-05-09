package com.wheel.cloud.seata.xa.common;

import com.alibaba.druid.pool.DruidDataSource;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import io.seata.rm.datasource.xa.DataSourceProxyXA;
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
 * XA 模式数据源配置
 *
 * 和 AT 的唯一区别：DataSourceProxy → DataSourceProxyXA
 *
 * XA 原理：
 *   - TC 协调所有 RM，每个 RM 在 prepare 阶段将数据库置于"已准备提交"状态（XA Prepare）
 *   - TC 收到所有 RM 的 prepare OK 后，再统一发 XA Commit
 *   - 任一 RM prepare 失败，TC 发 XA Rollback
 *   - 数据库层面保证原子性，不需要 undo_log，但 prepare 阶段持有数据库锁，性能差
 */
@Configuration
public class DataSourceConfig {

    @Bean("orderDruidDs")
    @Primary
    @ConfigurationProperties("spring.datasource.order")
    public DruidDataSource orderDruidDs() { return new DruidDataSource(); }

    @Bean("orderDataSource")
    @Primary
    public DataSourceProxyXA orderDataSource(@Qualifier("orderDruidDs") DruidDataSource ds) {
        return new DataSourceProxyXA(ds);
    }

    @Bean("orderSqlSessionFactory")
    @Primary
    public SqlSessionFactory orderSqlSessionFactory(@Qualifier("orderDataSource") DataSource ds) throws Exception {
        MybatisSqlSessionFactoryBean fb = new MybatisSqlSessionFactoryBean();
        fb.setDataSource(ds);
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
    public SqlSessionTemplate orderSqlSessionTemplate(@Qualifier("orderSqlSessionFactory") SqlSessionFactory sf) {
        return new SqlSessionTemplate(sf);
    }

    // ---- stock ----

    @Bean("stockDruidDs")
    @ConfigurationProperties("spring.datasource.stock")
    public DruidDataSource stockDruidDs() { return new DruidDataSource(); }

    @Bean("stockDataSource")
    public DataSourceProxyXA stockDataSource(@Qualifier("stockDruidDs") DruidDataSource ds) {
        return new DataSourceProxyXA(ds);
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
    public SqlSessionTemplate stockSqlSessionTemplate(@Qualifier("stockSqlSessionFactory") SqlSessionFactory sf) {
        return new SqlSessionTemplate(sf);
    }

    // ---- account ----

    @Bean("accountDruidDs")
    @ConfigurationProperties("spring.datasource.account")
    public DruidDataSource accountDruidDs() { return new DruidDataSource(); }

    @Bean("accountDataSource")
    public DataSourceProxyXA accountDataSource(@Qualifier("accountDruidDs") DruidDataSource ds) {
        return new DataSourceProxyXA(ds);
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
    public SqlSessionTemplate accountSqlSessionTemplate(@Qualifier("accountSqlSessionFactory") SqlSessionFactory sf) {
        return new SqlSessionTemplate(sf);
    }
}
