package com.api.config.datasource;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for managing multiple database connections by name.
 * 
 * Usage:
 * <pre>
 * // Get JdbcTemplate for a specific database
 * NamedParameterJdbcTemplate jdbc = dataSourceRegistry.getNamedParameterJdbcTemplate("InventoryDB");
 * 
 * // Check if a database is registered
 * boolean exists = dataSourceRegistry.hasDataSource("OrdersDB");
 * 
 * // Get all registered database names
 * Set&lt;String&gt; names = dataSourceRegistry.getDataSourceNames();
 * </pre>
 */
@Component
public class DataSourceRegistry {

    private static final Logger log = LoggerFactory.getLogger(DataSourceRegistry.class);

    private final DataSourceProperties properties;
    
    private final Map<String, HikariDataSource> dataSources = new ConcurrentHashMap<>();
    private final Map<String, JdbcTemplate> jdbcTemplates = new ConcurrentHashMap<>();
    private final Map<String, NamedParameterJdbcTemplate> namedParameterJdbcTemplates = new ConcurrentHashMap<>();
    private final Map<String, PlatformTransactionManager> transactionManagers = new ConcurrentHashMap<>();
    
    private String primaryDataSourceName;

    public DataSourceRegistry(DataSourceProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void initialize() {
        log.info("Initializing DataSource Registry...");
        
        Map<String, DataSourceProperties.DatabaseConfig> configs = properties.getDatasources();
        
        if (configs == null || configs.isEmpty()) {
            log.warn("No datasources configured. Add datasources under 'app.datasources' in application.yml");
            return;
        }

        for (Map.Entry<String, DataSourceProperties.DatabaseConfig> entry : configs.entrySet()) {
            String name = entry.getKey();
            DataSourceProperties.DatabaseConfig config = entry.getValue();
            
            registerDataSource(name, config);
            
            if (config.isPrimary()) {
                if (primaryDataSourceName != null) {
                    log.warn("Multiple primary datasources defined. Using '{}' as primary.", name);
                }
                primaryDataSourceName = name;
            }
        }

        // If no primary is set, use the first one
        if (primaryDataSourceName == null && !dataSources.isEmpty()) {
            primaryDataSourceName = dataSources.keySet().iterator().next();
            log.info("No primary datasource specified. Using '{}' as default primary.", primaryDataSourceName);
        }

        log.info("DataSource Registry initialized with {} datasource(s): {}", 
                dataSources.size(), dataSources.keySet());
    }

    /**
     * Register a new datasource with the given name and configuration.
     */
    public void registerDataSource(String name, DataSourceProperties.DatabaseConfig config) {
        log.info("Registering datasource: {}", name);
        
        HikariDataSource dataSource = createHikariDataSource(name, config);
        dataSources.put(name, dataSource);
        
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplates.put(name, jdbcTemplate);
        
        NamedParameterJdbcTemplate namedJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        namedParameterJdbcTemplates.put(name, namedJdbcTemplate);
        
        DataSourceTransactionManager txManager = new DataSourceTransactionManager(dataSource);
        transactionManagers.put(name, txManager);
        
        log.info("Datasource '{}' registered successfully", name);
    }

    /**
     * Dynamically add a new datasource at runtime.
     */
    public void addDataSource(String name, String url, String username, String password, String driverClassName) {
        if (dataSources.containsKey(name)) {
            throw new IllegalArgumentException("Datasource '" + name + "' already exists");
        }
        
        DataSourceProperties.DatabaseConfig config = new DataSourceProperties.DatabaseConfig();
        config.setUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driverClassName);
        
        registerDataSource(name, config);
    }

    /**
     * Remove a datasource by name.
     */
    public void removeDataSource(String name) {
        if (name.equals(primaryDataSourceName)) {
            throw new IllegalStateException("Cannot remove primary datasource");
        }
        
        HikariDataSource ds = dataSources.remove(name);
        if (ds != null) {
            ds.close();
            jdbcTemplates.remove(name);
            namedParameterJdbcTemplates.remove(name);
            transactionManagers.remove(name);
            log.info("Datasource '{}' removed", name);
        }
    }

    private HikariDataSource createHikariDataSource(String name, DataSourceProperties.DatabaseConfig config) {
        HikariDataSource ds = new HikariDataSource();
        
        ds.setJdbcUrl(config.getUrl());
        ds.setUsername(config.getUsername());
        ds.setPassword(config.getPassword());
        
        if (config.getDriverClassName() != null && !config.getDriverClassName().isEmpty()) {
            ds.setDriverClassName(config.getDriverClassName());
        }
        
        DataSourceProperties.HikariConfig hikari = config.getHikari();
        ds.setPoolName(hikari.getPoolName() != null ? hikari.getPoolName() : name + "Pool");
        ds.setMaximumPoolSize(hikari.getMaximumPoolSize());
        ds.setMinimumIdle(hikari.getMinimumIdle());
        ds.setIdleTimeout(hikari.getIdleTimeout());
        ds.setConnectionTimeout(hikari.getConnectionTimeout());
        ds.setMaxLifetime(hikari.getMaxLifetime());
        ds.setLeakDetectionThreshold(hikari.getLeakDetectionThreshold());
        
        return ds;
    }

    // ========== Getters ==========

    /**
     * Get DataSource by name.
     */
    public DataSource getDataSource(String name) {
        DataSource ds = dataSources.get(name);
        if (ds == null) {
            throw new IllegalArgumentException("No datasource found with name: " + name);
        }
        return ds;
    }

    /**
     * Get JdbcTemplate by database name.
     */
    public JdbcTemplate getJdbcTemplate(String name) {
        JdbcTemplate template = jdbcTemplates.get(name);
        if (template == null) {
            throw new IllegalArgumentException("No JdbcTemplate found for datasource: " + name);
        }
        return template;
    }

    /**
     * Get NamedParameterJdbcTemplate by database name.
     */
    public NamedParameterJdbcTemplate getNamedParameterJdbcTemplate(String name) {
        NamedParameterJdbcTemplate template = namedParameterJdbcTemplates.get(name);
        if (template == null) {
            throw new IllegalArgumentException("No NamedParameterJdbcTemplate found for datasource: " + name);
        }
        return template;
    }

    /**
     * Get TransactionManager by database name.
     */
    public PlatformTransactionManager getTransactionManager(String name) {
        PlatformTransactionManager txManager = transactionManagers.get(name);
        if (txManager == null) {
            throw new IllegalArgumentException("No TransactionManager found for datasource: " + name);
        }
        return txManager;
    }

    /**
     * Get the primary DataSource.
     */
    public DataSource getPrimaryDataSource() {
        if (primaryDataSourceName == null) {
            throw new IllegalStateException("No primary datasource configured");
        }
        return getDataSource(primaryDataSourceName);
    }

    /**
     * Get the primary JdbcTemplate.
     */
    public JdbcTemplate getPrimaryJdbcTemplate() {
        if (primaryDataSourceName == null) {
            throw new IllegalStateException("No primary datasource configured");
        }
        return getJdbcTemplate(primaryDataSourceName);
    }

    /**
     * Get the primary NamedParameterJdbcTemplate.
     */
    public NamedParameterJdbcTemplate getPrimaryNamedParameterJdbcTemplate() {
        if (primaryDataSourceName == null) {
            throw new IllegalStateException("No primary datasource configured");
        }
        return getNamedParameterJdbcTemplate(primaryDataSourceName);
    }

    /**
     * Get the primary TransactionManager.
     */
    public PlatformTransactionManager getPrimaryTransactionManager() {
        if (primaryDataSourceName == null) {
            throw new IllegalStateException("No primary datasource configured");
        }
        return getTransactionManager(primaryDataSourceName);
    }

    /**
     * Check if a datasource exists.
     */
    public boolean hasDataSource(String name) {
        return dataSources.containsKey(name);
    }

    /**
     * Get all registered datasource names.
     */
    public Set<String> getDataSourceNames() {
        return Collections.unmodifiableSet(dataSources.keySet());
    }

    /**
     * Get the name of the primary datasource.
     */
    public String getPrimaryDataSourceName() {
        return primaryDataSourceName;
    }

    /**
     * Get a read-only view of all DataSources.
     */
    public Map<String, DataSource> getAllDataSources() {
        return Collections.unmodifiableMap(dataSources);
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down DataSource Registry...");
        for (Map.Entry<String, HikariDataSource> entry : dataSources.entrySet()) {
            try {
                entry.getValue().close();
                log.info("Closed datasource: {}", entry.getKey());
            } catch (Exception e) {
                log.error("Error closing datasource '{}': {}", entry.getKey(), e.getMessage());
            }
        }
        dataSources.clear();
        jdbcTemplates.clear();
        namedParameterJdbcTemplates.clear();
        transactionManagers.clear();
    }
}
