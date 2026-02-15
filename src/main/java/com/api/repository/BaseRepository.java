package com.api.repository;

import com.api.config.datasource.DataSourceRegistry;
import com.api.dto.common.PageRequest;
import com.api.util.PaginationHelper;
import com.api.util.SqlLoader;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.util.List;
import java.util.Optional;

/**
 * Base repository providing common CRUD operations using JdbcTemplate.
 * Uses external SQL files for all queries.
 * 
 * Supports two modes:
 * 1. Direct JdbcTemplate injection (backwards compatible)
 * 2. DataSourceRegistry-based dynamic database selection by name
 *
 * @param <T>  Entity type
 * @param <ID> ID type
 */
public abstract class BaseRepository<T, ID> {

    protected final NamedParameterJdbcTemplate jdbcTemplate;
    protected final SqlLoader sqlLoader;
    protected final PaginationHelper paginationHelper;
    protected final String moduleName;
    protected final DataSourceRegistry dataSourceRegistry;
    protected final String databaseName;

    /**
     * Constructor for direct JdbcTemplate injection (backwards compatible).
     */
    protected BaseRepository(NamedParameterJdbcTemplate jdbcTemplate,
                            SqlLoader sqlLoader,
                            PaginationHelper paginationHelper,
                            String moduleName) {
        this.jdbcTemplate = jdbcTemplate;
        this.sqlLoader = sqlLoader;
        this.paginationHelper = paginationHelper;
        this.moduleName = moduleName;
        this.dataSourceRegistry = null;
        this.databaseName = null;
    }

    /**
     * Constructor for dynamic database selection by name.
     * Use this when you want to specify which database to connect to.
     * 
     * Example:
     * <pre>
     * public UserRepository(DataSourceRegistry registry, SqlLoader sqlLoader, PaginationHelper paginationHelper) {
     *     super(registry, "InventoryDB", sqlLoader, paginationHelper, "user");
     * }
     * </pre>
     */
    protected BaseRepository(DataSourceRegistry dataSourceRegistry,
                            String databaseName,
                            SqlLoader sqlLoader,
                            PaginationHelper paginationHelper,
                            String moduleName) {
        this.dataSourceRegistry = dataSourceRegistry;
        this.databaseName = databaseName;
        this.jdbcTemplate = dataSourceRegistry.getNamedParameterJdbcTemplate(databaseName);
        this.sqlLoader = sqlLoader;
        this.paginationHelper = paginationHelper;
        this.moduleName = moduleName;
    }

    /**
     * Get the database name this repository is connected to.
     * Returns null if using direct JdbcTemplate injection.
     */
    public String getDatabaseName() {
        return databaseName;
    }

    /**
     * Get a JdbcTemplate for a different database.
     * Useful for cross-database queries within a single repository.
     */
    protected NamedParameterJdbcTemplate getJdbcTemplateFor(String dbName) {
        if (dataSourceRegistry == null) {
            throw new IllegalStateException(
                "DataSourceRegistry not available. Use the registry-based constructor.");
        }
        return dataSourceRegistry.getNamedParameterJdbcTemplate(dbName);
    }

    /**
     * Get the RowMapper for this entity.
     */
    protected abstract RowMapper<T> getRowMapper();

    /**
     * Find entity by ID.
     */
    public Optional<T> findById(ID id) {
        String sql = sqlLoader.getSql(moduleName, "findById");
        MapSqlParameterSource params = new MapSqlParameterSource("id", id);
        
        List<T> results = jdbcTemplate.query(sql, params, getRowMapper());
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Find all entities with pagination.
     */
    public List<T> findAll(PageRequest pageRequest) {
        String baseSql = sqlLoader.getSql(moduleName, "findAll");
        String paginationClause = paginationHelper.buildPaginationClause(pageRequest);
        String sql = baseSql + paginationClause;
        
        return jdbcTemplate.query(sql, new MapSqlParameterSource(), getRowMapper());
    }

    /**
     * Count all entities.
     */
    public long count() {
        String sql = sqlLoader.getSql(moduleName, "count");
        Long count = jdbcTemplate.queryForObject(sql, new MapSqlParameterSource(), Long.class);
        return count != null ? count : 0;
    }

    /**
     * Delete entity by ID (soft delete).
     */
    public boolean deleteById(ID id) {
        String sql = sqlLoader.getSql(moduleName, "delete");
        MapSqlParameterSource params = new MapSqlParameterSource("id", id);
        int affected = jdbcTemplate.update(sql, params);
        return affected > 0;
    }

    /**
     * Check if entity exists by ID.
     */
    public boolean existsById(ID id) {
        String sql = sqlLoader.getSql(moduleName, "existsById");
        MapSqlParameterSource params = new MapSqlParameterSource("id", id);
        Long count = jdbcTemplate.queryForObject(sql, params, Long.class);
        return count != null && count > 0;
    }

    /**
     * Execute insert and return generated key.
     */
    protected Number executeInsert(String sql, MapSqlParameterSource params) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, params, keyHolder);
        return keyHolder.getKey();
    }

    /**
     * Execute update with optimistic locking.
     * Returns the number of affected rows (0 if version mismatch).
     */
    protected int executeUpdateWithLocking(String sql, MapSqlParameterSource params) {
        return jdbcTemplate.update(sql, params);
    }
}
