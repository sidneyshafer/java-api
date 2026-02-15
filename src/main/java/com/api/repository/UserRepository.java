package com.api.repository;

import com.api.dto.common.PageRequest;
import com.api.model.User;
import com.api.util.PaginationHelper;
import com.api.util.SqlLoader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for User entity operations.
 */
@Repository
public class UserRepository extends BaseRepository<User, Long> {

    private static final String MODULE_NAME = "user";

    public UserRepository(
            @Qualifier("primaryNamedParameterJdbcTemplate") NamedParameterJdbcTemplate jdbcTemplate,
            SqlLoader sqlLoader,
            PaginationHelper paginationHelper) {
        super(jdbcTemplate, sqlLoader, paginationHelper, MODULE_NAME);
    }

    @Override
    protected RowMapper<User> getRowMapper() {
        return (rs, rowNum) -> {
            User user = new User();
            user.setId(rs.getLong("id"));
            user.setEmail(rs.getString("email"));
            user.setFirstName(rs.getString("first_name"));
            user.setLastName(rs.getString("last_name"));
            user.setPhone(rs.getString("phone"));
            user.setStatus(rs.getString("status"));
            user.setRole(rs.getString("role"));
            user.setCreatedAt(rs.getTimestamp("created_at") != null ? 
                    rs.getTimestamp("created_at").toLocalDateTime() : null);
            user.setUpdatedAt(rs.getTimestamp("updated_at") != null ? 
                    rs.getTimestamp("updated_at").toLocalDateTime() : null);
            user.setDeleted(rs.getBoolean("deleted"));
            user.setVersion(rs.getInt("version"));
            return user;
        };
    }

    /**
     * Create a new user.
     */
    public User create(User user) {
        String sql = sqlLoader.getSql(MODULE_NAME, "create");
        
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("email", user.getEmail())
                .addValue("firstName", user.getFirstName())
                .addValue("lastName", user.getLastName())
                .addValue("phone", user.getPhone())
                .addValue("status", user.getStatus() != null ? user.getStatus() : "ACTIVE")
                .addValue("role", user.getRole() != null ? user.getRole() : "USER")
                .addValue("createdAt", LocalDateTime.now())
                .addValue("updatedAt", LocalDateTime.now())
                .addValue("createdBy", user.getCreatedBy())
                .addValue("version", 1);

        Number id = executeInsert(sql, params);
        user.setId(id.longValue());
        user.setVersion(1);
        return user;
    }

    /**
     * Update an existing user with optimistic locking.
     */
    public Optional<User> update(User user) {
        String sql = sqlLoader.getSql(MODULE_NAME, "update");
        
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", user.getId())
                .addValue("email", user.getEmail())
                .addValue("firstName", user.getFirstName())
                .addValue("lastName", user.getLastName())
                .addValue("phone", user.getPhone())
                .addValue("status", user.getStatus())
                .addValue("role", user.getRole())
                .addValue("updatedAt", LocalDateTime.now())
                .addValue("updatedBy", user.getUpdatedBy())
                .addValue("version", user.getVersion())
                .addValue("newVersion", user.getVersion() + 1);

        int affected = executeUpdateWithLocking(sql, params);
        
        if (affected == 0) {
            return Optional.empty(); // Optimistic lock failure
        }
        
        return findById(user.getId());
    }

    /**
     * Find user by email.
     */
    public Optional<User> findByEmail(String email) {
        String sql = sqlLoader.getSql(MODULE_NAME, "findByEmail");
        MapSqlParameterSource params = new MapSqlParameterSource("email", email);
        
        List<User> results = jdbcTemplate.query(sql, params, getRowMapper());
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Find users by status with pagination.
     */
    public List<User> findByStatus(String status, PageRequest pageRequest) {
        String baseSql = sqlLoader.getSql(MODULE_NAME, "findByStatus");
        String paginationClause = paginationHelper.buildPaginationClause(pageRequest);
        String sql = baseSql + paginationClause;
        
        MapSqlParameterSource params = new MapSqlParameterSource("status", status);
        return jdbcTemplate.query(sql, params, getRowMapper());
    }

    /**
     * Count users by status.
     */
    public long countByStatus(String status) {
        String sql = sqlLoader.getSql(MODULE_NAME, "countByStatus");
        MapSqlParameterSource params = new MapSqlParameterSource("status", status);
        Long count = jdbcTemplate.queryForObject(sql, params, Long.class);
        return count != null ? count : 0;
    }

    /**
     * Check if email exists.
     */
    public boolean existsByEmail(String email) {
        String sql = sqlLoader.getSql(MODULE_NAME, "existsByEmail");
        MapSqlParameterSource params = new MapSqlParameterSource("email", email);
        Long count = jdbcTemplate.queryForObject(sql, params, Long.class);
        return count != null && count > 0;
    }

    /**
     * Search users by name.
     */
    public List<User> searchByName(String searchTerm, PageRequest pageRequest) {
        String baseSql = sqlLoader.getSql(MODULE_NAME, "searchByName");
        String paginationClause = paginationHelper.buildPaginationClause(pageRequest);
        String sql = baseSql + paginationClause;
        
        MapSqlParameterSource params = new MapSqlParameterSource("searchTerm", "%" + searchTerm + "%");
        return jdbcTemplate.query(sql, params, getRowMapper());
    }
}
