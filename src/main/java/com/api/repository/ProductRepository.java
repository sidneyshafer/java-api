package com.api.repository;

import com.api.config.datasource.DataSourceRegistry;
import com.api.dto.common.PageRequest;
import com.api.model.Product;
import com.api.util.PaginationHelper;
import com.api.util.SqlLoader;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Product entity operations.
 * Connects to the primary database by default.
 */
@Repository
public class ProductRepository extends BaseRepository<Product, Long> {

    private static final String MODULE_NAME = "product";

    public ProductRepository(
            DataSourceRegistry dataSourceRegistry,
            SqlLoader sqlLoader,
            PaginationHelper paginationHelper) {
        super(dataSourceRegistry, dataSourceRegistry.getPrimaryDataSourceName(),
              sqlLoader, paginationHelper, MODULE_NAME);
    }

    @Override
    protected RowMapper<Product> getRowMapper() {
        return (rs, rowNum) -> {
            Product product = new Product();
            product.setId(rs.getLong("id"));
            product.setSku(rs.getString("sku"));
            product.setName(rs.getString("name"));
            product.setDescription(rs.getString("description"));
            product.setPrice(rs.getBigDecimal("price"));
            product.setQuantity(rs.getInt("quantity"));
            product.setCategory(rs.getString("category"));
            product.setStatus(rs.getString("status"));
            product.setCreatedAt(rs.getTimestamp("created_at") != null ?
                    rs.getTimestamp("created_at").toLocalDateTime() : null);
            product.setUpdatedAt(rs.getTimestamp("updated_at") != null ?
                    rs.getTimestamp("updated_at").toLocalDateTime() : null);
            product.setDeleted(rs.getBoolean("deleted"));
            product.setVersion(rs.getInt("version"));
            return product;
        };
    }

    /**
     * Create a new product.
     */
    public Product create(Product product) {
        String sql = sqlLoader.getSql(MODULE_NAME, "create");

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("sku", product.getSku())
                .addValue("name", product.getName())
                .addValue("description", product.getDescription())
                .addValue("price", product.getPrice())
                .addValue("quantity", product.getQuantity() != null ? product.getQuantity() : 0)
                .addValue("category", product.getCategory())
                .addValue("status", product.getStatus() != null ? product.getStatus() : "ACTIVE")
                .addValue("createdAt", LocalDateTime.now())
                .addValue("updatedAt", LocalDateTime.now())
                .addValue("createdBy", product.getCreatedBy())
                .addValue("version", 1);

        Number id = executeInsert(sql, params);
        product.setId(id.longValue());
        product.setVersion(1);
        return product;
    }

    /**
     * Update an existing product with optimistic locking.
     */
    public Optional<Product> update(Product product) {
        String sql = sqlLoader.getSql(MODULE_NAME, "update");

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", product.getId())
                .addValue("sku", product.getSku())
                .addValue("name", product.getName())
                .addValue("description", product.getDescription())
                .addValue("price", product.getPrice())
                .addValue("quantity", product.getQuantity())
                .addValue("category", product.getCategory())
                .addValue("status", product.getStatus())
                .addValue("updatedAt", LocalDateTime.now())
                .addValue("updatedBy", product.getUpdatedBy())
                .addValue("version", product.getVersion())
                .addValue("newVersion", product.getVersion() + 1);

        int affected = executeUpdateWithLocking(sql, params);

        if (affected == 0) {
            return Optional.empty();
        }

        return findById(product.getId());
    }

    /**
     * Find product by SKU.
     */
    public Optional<Product> findBySku(String sku) {
        String sql = sqlLoader.getSql(MODULE_NAME, "findBySku");
        MapSqlParameterSource params = new MapSqlParameterSource("sku", sku);

        List<Product> results = jdbcTemplate.query(sql, params, getRowMapper());
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Find products by category with pagination.
     */
    public List<Product> findByCategory(String category, PageRequest pageRequest) {
        String baseSql = sqlLoader.getSql(MODULE_NAME, "findByCategory");
        String paginationClause = paginationHelper.buildPaginationClause(pageRequest);
        String sql = baseSql + paginationClause;

        MapSqlParameterSource params = new MapSqlParameterSource("category", category);
        return jdbcTemplate.query(sql, params, getRowMapper());
    }

    /**
     * Count products by category.
     */
    public long countByCategory(String category) {
        String sql = sqlLoader.getSql(MODULE_NAME, "countByCategory");
        MapSqlParameterSource params = new MapSqlParameterSource("category", category);
        Long count = jdbcTemplate.queryForObject(sql, params, Long.class);
        return count != null ? count : 0;
    }

    /**
     * Check if SKU exists.
     */
    public boolean existsBySku(String sku) {
        String sql = sqlLoader.getSql(MODULE_NAME, "existsBySku");
        MapSqlParameterSource params = new MapSqlParameterSource("sku", sku);
        Long count = jdbcTemplate.queryForObject(sql, params, Long.class);
        return count != null && count > 0;
    }

    /**
     * Search products by name.
     */
    public List<Product> searchByName(String searchTerm, PageRequest pageRequest) {
        String baseSql = sqlLoader.getSql(MODULE_NAME, "searchByName");
        String paginationClause = paginationHelper.buildPaginationClause(pageRequest);
        String sql = baseSql + paginationClause;

        MapSqlParameterSource params = new MapSqlParameterSource("searchTerm", "%" + searchTerm + "%");
        return jdbcTemplate.query(sql, params, getRowMapper());
    }

    /**
     * Update product quantity (for inventory management).
     */
    public boolean updateQuantity(Long id, int quantityChange, int currentVersion) {
        String sql = sqlLoader.getSql(MODULE_NAME, "updateQuantity");

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("quantityChange", quantityChange)
                .addValue("updatedAt", LocalDateTime.now())
                .addValue("version", currentVersion)
                .addValue("newVersion", currentVersion + 1);

        int affected = jdbcTemplate.update(sql, params);
        return affected > 0;
    }
}
