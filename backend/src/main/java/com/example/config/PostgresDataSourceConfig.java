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
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

@Configuration
@MapperScan(basePackages = "com.example.mapper.postgres", sqlSessionTemplateRef = "postgresSqlSessionTemplate")
public class PostgresDataSourceConfig {

    @Bean("postgresDataSourceProperties")
    @ConfigurationProperties("spring.datasource.postgres")
    public DataSourceProperties postgresDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "postgresDataSource")
    public DataSource postgresDataSource(@Qualifier("postgresDataSourceProperties") DataSourceProperties dsp) {
        return dsp.initializeDataSourceBuilder()
                .type(com.zaxxer.hikari.HikariDataSource.class)
                .build();
    }

    @Bean(name = "postgresSqlSessionFactory")
    public SqlSessionFactory postgresSqlSessionFactory(@Qualifier("postgresDataSource") DataSource ds) throws Exception {
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(ds);

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = new Resource[0];
        try {
            resources = resolver.getResources("classpath*:mapper/postgres/**/*.xml");
        } catch (IOException e) {
            System.out.println("PostgresDataSource: no postgres mapper files found under classpath*:mappers/postgres/**/*.xml");
        }
        bean.setMapperLocations(resources);

        return bean.getObject();
    }

    @Bean(name = "postgresTransactionManager")
    public DataSourceTransactionManager postgresTx(@Qualifier("postgresDataSource") DataSource ds) {
        return new DataSourceTransactionManager(ds);
    }

    @Bean(name = "postgresSqlSessionTemplate")
    public SqlSessionTemplate postgresSqlSessionTemplate(@Qualifier("postgresSqlSessionFactory") SqlSessionFactory sf) {
        return new SqlSessionTemplate(sf);
    }
}
