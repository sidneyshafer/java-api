package com.api.config.datasource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

/**
 * Configuration that exposes the primary datasource as Spring beans 
 * for backwards compatibility with @Qualifier annotations.
 */
@Configuration
@EnableTransactionManagement
@DependsOn("dataSourceRegistry")
public class DynamicDataSourceConfig {

    private final DataSourceRegistry registry;

    public DynamicDataSourceConfig(DataSourceRegistry registry) {
        this.registry = registry;
    }

    @Primary
    @Bean(name = "primaryDataSource")
    public DataSource primaryDataSource() {
        return registry.getPrimaryDataSource();
    }

    @Primary
    @Bean(name = "primaryJdbcTemplate")
    public JdbcTemplate primaryJdbcTemplate() {
        return registry.getPrimaryJdbcTemplate();
    }

    @Primary
    @Bean(name = "primaryNamedParameterJdbcTemplate")
    public NamedParameterJdbcTemplate primaryNamedParameterJdbcTemplate() {
        return registry.getPrimaryNamedParameterJdbcTemplate();
    }

    @Primary
    @Bean(name = "primaryTransactionManager")
    public PlatformTransactionManager primaryTransactionManager() {
        return registry.getPrimaryTransactionManager();
    }
}
