package com.example.config;

import java.io.IOException;
import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

@Configuration
@MapperScan(basePackages = "com.example.mapper.mysql", sqlSessionTemplateRef = "primarySqlSessionTemplate")
public class PrimaryDataSourceConfig {

    @Bean("primaryDataSourceProperties")
    @ConfigurationProperties("spring.datasource.primary")
    public DataSourceProperties primaryDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "primaryDataSource")
    @Primary
    public DataSource primaryDataSource(@Qualifier("primaryDataSourceProperties") DataSourceProperties dsp) {
        return dsp.initializeDataSourceBuilder()
                .type(com.zaxxer.hikari.HikariDataSource.class)
                .build();
    }

    @Bean(name = "primarySqlSessionFactory")
    @Primary
    public SqlSessionFactory primarySqlSessionFactory(@Qualifier("primaryDataSource") DataSource ds) throws Exception {
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(ds);

        // 使用 classpath* + 通配符，且对找不到文件做容错
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = new Resource[0];
        try {
            resources = resolver.getResources("classpath*:mapper/mysql/**/*.xml");
        } catch (IOException e) {
            System.out.println("PrimaryDataSource: no mysql mapper files found under classpath*:mappers/mysql/**/*.xml");
        }
        bean.setMapperLocations(resources);

        return bean.getObject();
    }

    @Bean(name = "primaryTransactionManager")
    @Primary
    public DataSourceTransactionManager primaryTx(@Qualifier("primaryDataSource") DataSource ds) {
        return new DataSourceTransactionManager(ds);
    }

    @Bean(name = "primarySqlSessionTemplate")
    @Primary
    public SqlSessionTemplate primarySqlSessionTemplate(@Qualifier("primarySqlSessionFactory") SqlSessionFactory sf) {
        return new SqlSessionTemplate(sf);
    }
}
