package com.api.config.datasource;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for dynamic datasources.
 * Allows defining multiple named database connections.
 * 
 * Example configuration:
 * <pre>
 * app:
 *   datasources:
 *     InventoryDB:
 *       url: jdbc:postgresql://localhost:5432/inventory
 *       username: postgres
 *       password: secret
 *     OrdersDB:
 *       url: jdbc:postgresql://localhost:5432/orders
 *       username: postgres
 *       password: secret
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "app")
public class DataSourceProperties {

    private Map<String, DatabaseConfig> datasources = new HashMap<>();

    public Map<String, DatabaseConfig> getDatasources() {
        return datasources;
    }

    public void setDatasources(Map<String, DatabaseConfig> datasources) {
        this.datasources = datasources;
    }

    /**
     * Configuration for a single database connection.
     */
    public static class DatabaseConfig {
        private String url;
        private String username;
        private String password;
        private String driverClassName;
        private boolean primary = false;
        private HikariConfig hikari = new HikariConfig();

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getDriverClassName() {
            return driverClassName;
        }

        public void setDriverClassName(String driverClassName) {
            this.driverClassName = driverClassName;
        }

        public boolean isPrimary() {
            return primary;
        }

        public void setPrimary(boolean primary) {
            this.primary = primary;
        }

        public HikariConfig getHikari() {
            return hikari;
        }

        public void setHikari(HikariConfig hikari) {
            this.hikari = hikari;
        }
    }

    /**
     * HikariCP connection pool configuration.
     */
    public static class HikariConfig {
        private String poolName;
        private int maximumPoolSize = 10;
        private int minimumIdle = 5;
        private long idleTimeout = 300000;
        private long connectionTimeout = 20000;
        private long maxLifetime = 1200000;
        private long leakDetectionThreshold = 60000;

        public String getPoolName() {
            return poolName;
        }

        public void setPoolName(String poolName) {
            this.poolName = poolName;
        }

        public int getMaximumPoolSize() {
            return maximumPoolSize;
        }

        public void setMaximumPoolSize(int maximumPoolSize) {
            this.maximumPoolSize = maximumPoolSize;
        }

        public int getMinimumIdle() {
            return minimumIdle;
        }

        public void setMinimumIdle(int minimumIdle) {
            this.minimumIdle = minimumIdle;
        }

        public long getIdleTimeout() {
            return idleTimeout;
        }

        public void setIdleTimeout(long idleTimeout) {
            this.idleTimeout = idleTimeout;
        }

        public long getConnectionTimeout() {
            return connectionTimeout;
        }

        public void setConnectionTimeout(long connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
        }

        public long getMaxLifetime() {
            return maxLifetime;
        }

        public void setMaxLifetime(long maxLifetime) {
            this.maxLifetime = maxLifetime;
        }

        public long getLeakDetectionThreshold() {
            return leakDetectionThreshold;
        }

        public void setLeakDetectionThreshold(long leakDetectionThreshold) {
            this.leakDetectionThreshold = leakDetectionThreshold;
        }
    }
}
