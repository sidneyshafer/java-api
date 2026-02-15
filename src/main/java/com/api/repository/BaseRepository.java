package com.api.repository;

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
 * @param <T>  Entity type
 * @param <ID> ID type
 */
public abstract class BaseRepository<T, ID> {

    protected final NamedParameterJdbcTemplate jdbcTemplate;
    protected final SqlLoader sqlLoader;
    protected final PaginationHelper paginationHelper;
    protected final String moduleName;

    protected BaseRepository(NamedParameterJdbcTemplate jdbcTemplate,
                            SqlLoader sqlLoader,
                            PaginationHelper paginationHelper,
                            String moduleName) {
        this.jdbcTemplate = jdbcTemplate;
        this.sqlLoader = sqlLoader;
        this.paginationHelper = paginationHelper;
        this.moduleName = moduleName;
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
