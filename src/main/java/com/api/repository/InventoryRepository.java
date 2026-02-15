package com.api.repository;

import com.api.config.datasource.DataSourceRegistry;
import com.api.dto.common.PageRequest;
import com.api.model.Product;
import com.api.util.PaginationHelper;
import com.api.util.SqlLoader;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Example repository demonstrating how to connect to a specific database by name.
 * 
 * This repository connects to the "InventoryDB" database instead of the primary database.
 * 
 * To use this pattern:
 * 1. Add the database to application.yml under app.datasources
 * 2. Pass the database name to the BaseRepository constructor
 * 
 * Example application.yml:
 * <pre>
 * app:
 *   datasources:
 *     InventoryDB:
 *       url: jdbc:postgresql://localhost:5432/inventory
 *       username: postgres
 *       password: secret
 *       driver-class-name: org.postgresql.Driver
 * </pre>
 */
// @Repository  // Uncomment when InventoryDB is configured
public class InventoryRepository extends BaseRepository<Product, Long> {

    private static final String MODULE_NAME = "product";
    private static final String DATABASE_NAME = "InventoryDB";

    /**
     * Constructor that connects to a specific database by name.
     */
    public InventoryRepository(
            DataSourceRegistry dataSourceRegistry,
            SqlLoader sqlLoader,
            PaginationHelper paginationHelper) {
        // Specify the database name instead of using the primary
        super(dataSourceRegistry, DATABASE_NAME, sqlLoader, paginationHelper, MODULE_NAME);
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
            return product;
        };
    }

    /**
     * Example: Query the InventoryDB for low stock products.
     */
    public List<Product> findLowStockProducts(int threshold, PageRequest pageRequest) {
        String sql = "SELECT * FROM products WHERE quantity < :threshold AND deleted = false";
        String paginationClause = paginationHelper.buildPaginationClause(pageRequest);
        
        MapSqlParameterSource params = new MapSqlParameterSource("threshold", threshold);
        return jdbcTemplate.query(sql + paginationClause, params, getRowMapper());
    }

    /**
     * Example: Cross-database query - get product from InventoryDB and enrich with data from another DB.
     * Shows how to use getJdbcTemplateFor() to query a different database.
     */
    public Product getProductWithCrossDbData(Long productId, String otherDbName) {
        // Query InventoryDB (this repository's default database)
        Product product = findById(productId).orElse(null);
        
        if (product != null && dataSourceRegistry != null) {
            // Query another database for additional data
            var otherJdbc = getJdbcTemplateFor(otherDbName);
            // Use otherJdbc to get additional data from the other database
            // Example: otherJdbc.query("SELECT ...", params, rowMapper);
        }
        
        return product;
    }
}
