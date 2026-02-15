package com.api.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility for loading SQL queries from external .sql files.
 * 
 * SQL files should be organized as:
 * /sql/{module}/{operation}.sql
 * 
 * Example:
 * /sql/user/create.sql
 * /sql/user/findById.sql
 * /sql/user/findAll.sql
 * /sql/user/update.sql
 * /sql/user/delete.sql
 */
@Component
public class SqlLoader {

    private static final Logger log = LoggerFactory.getLogger(SqlLoader.class);
    
    @Value("${app.sql.base-path:classpath:sql/}")
    private String sqlBasePath;
    
    private final Map<String, String> sqlCache = new ConcurrentHashMap<>();
    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    @PostConstruct
    public void init() {
        log.info("SQL Loader initialized with base path: {}", sqlBasePath);
        preloadSqlFiles();
    }

    /**
     * Preload all SQL files at startup for better performance.
     */
    private void preloadSqlFiles() {
        try {
            Resource[] resources = resolver.getResources(sqlBasePath + "**/*.sql");
            for (Resource resource : resources) {
                String path = extractRelativePath(resource);
                if (path != null) {
                    String sql = loadSqlContent(resource);
                    sqlCache.put(path, sql);
                    log.debug("Preloaded SQL: {}", path);
                }
            }
            log.info("Preloaded {} SQL files", sqlCache.size());
        } catch (IOException e) {
            log.warn("Could not preload SQL files: {}", e.getMessage());
        }
    }

    /**
     * Get SQL query by module and operation.
     * 
     * @param module The module name (e.g., "user", "product")
     * @param operation The operation name (e.g., "create", "findById")
     * @return The SQL query string
     */
    @Cacheable(value = "sqlQueries", key = "#module + '/' + #operation")
    public String getSql(String module, String operation) {
        String key = module + "/" + operation + ".sql";
        
        // Check cache first
        if (sqlCache.containsKey(key)) {
            return sqlCache.get(key);
        }
        
        // Load from file
        String sql = loadSqlFromFile(module, operation);
        sqlCache.put(key, sql);
        return sql;
    }

    /**
     * Get SQL query with a custom filename.
     * 
     * @param path Full relative path to SQL file (e.g., "user/findByEmail.sql")
     * @return The SQL query string
     */
    public String getSqlByPath(String path) {
        if (!path.endsWith(".sql")) {
            path = path + ".sql";
        }
        
        if (sqlCache.containsKey(path)) {
            return sqlCache.get(path);
        }
        
        try {
            Resource resource = resolver.getResource(sqlBasePath + path);
            String sql = loadSqlContent(resource);
            sqlCache.put(path, sql);
            return sql;
        } catch (Exception e) {
            throw new SqlLoadException("Failed to load SQL file: " + path, e);
        }
    }

    /**
     * Load SQL from file.
     */
    private String loadSqlFromFile(String module, String operation) {
        String path = String.format("%s%s/%s.sql", sqlBasePath, module, operation);
        try {
            Resource resource = resolver.getResource(path);
            return loadSqlContent(resource);
        } catch (Exception e) {
            throw new SqlLoadException(
                    String.format("Failed to load SQL file for module: %s, operation: %s", 
                            module, operation), e);
        }
    }

    /**
     * Load SQL content from resource.
     */
    private String loadSqlContent(Resource resource) {
        try (InputStream inputStream = resource.getInputStream()) {
            String content = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
            return normalizeSql(content);
        } catch (IOException e) {
            throw new SqlLoadException("Failed to read SQL file: " + resource.getFilename(), e);
        }
    }

    /**
     * Normalize SQL content by removing comments and extra whitespace.
     */
    private String normalizeSql(String sql) {
        // Remove single-line comments
        sql = sql.replaceAll("--.*$", "");
        // Remove multi-line comments
        sql = sql.replaceAll("/\\*[^*]*\\*+(?:[^/*][^*]*\\*+)*/", "");
        // Normalize whitespace
        sql = sql.replaceAll("\\s+", " ").trim();
        return sql;
    }

    /**
     * Extract relative path from resource.
     */
    private String extractRelativePath(Resource resource) {
        try {
            String uri = resource.getURI().toString();
            int sqlIndex = uri.lastIndexOf("/sql/");
            if (sqlIndex != -1) {
                return uri.substring(sqlIndex + 5); // +5 to skip "/sql/"
            }
            return resource.getFilename();
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Reload all SQL files (useful for development).
     */
    public void reloadAll() {
        sqlCache.clear();
        preloadSqlFiles();
        log.info("Reloaded all SQL files");
    }

    /**
     * Check if a SQL query exists.
     */
    public boolean exists(String module, String operation) {
        String key = module + "/" + operation + ".sql";
        if (sqlCache.containsKey(key)) {
            return true;
        }
        try {
            Resource resource = resolver.getResource(
                    String.format("%s%s/%s.sql", sqlBasePath, module, operation));
            return resource.exists();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Custom exception for SQL loading errors.
     */
    public static class SqlLoadException extends RuntimeException {
        public SqlLoadException(String message, Throwable cause) {
            super(message, cause);
        }

        public SqlLoadException(String message) {
            super(message);
        }
    }
}
