package com.api.repository;

import com.api.config.datasource.DataSourceRegistry;
import com.api.dto.common.PageRequest;
import com.api.model.Order;
import com.api.model.OrderItem;
import com.api.util.PaginationHelper;
import com.api.util.SqlLoader;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Order entity operations.
 * Connects to the primary database by default.
 */
@Repository
public class OrderRepository extends BaseRepository<Order, Long> {

    private static final String MODULE_NAME = "order";

    public OrderRepository(
            DataSourceRegistry dataSourceRegistry,
            SqlLoader sqlLoader,
            PaginationHelper paginationHelper) {
        super(dataSourceRegistry, dataSourceRegistry.getPrimaryDataSourceName(),
              sqlLoader, paginationHelper, MODULE_NAME);
    }

    @Override
    protected RowMapper<Order> getRowMapper() {
        return (rs, rowNum) -> {
            Order order = new Order();
            order.setId(rs.getLong("id"));
            order.setOrderNumber(rs.getString("order_number"));
            order.setUserId(rs.getLong("user_id"));
            order.setTotalAmount(rs.getBigDecimal("total_amount"));
            order.setStatus(rs.getString("status"));
            order.setShippingAddress(rs.getString("shipping_address"));
            order.setBillingAddress(rs.getString("billing_address"));
            order.setOrderDate(rs.getTimestamp("order_date") != null ?
                    rs.getTimestamp("order_date").toLocalDateTime() : null);
            order.setCreatedAt(rs.getTimestamp("created_at") != null ?
                    rs.getTimestamp("created_at").toLocalDateTime() : null);
            order.setUpdatedAt(rs.getTimestamp("updated_at") != null ?
                    rs.getTimestamp("updated_at").toLocalDateTime() : null);
            order.setDeleted(rs.getBoolean("deleted"));
            order.setVersion(rs.getInt("version"));
            return order;
        };
    }

    private RowMapper<OrderItem> getOrderItemRowMapper() {
        return (rs, rowNum) -> OrderItem.builder()
                .id(rs.getLong("id"))
                .orderId(rs.getLong("order_id"))
                .productId(rs.getLong("product_id"))
                .quantity(rs.getInt("quantity"))
                .unitPrice(rs.getBigDecimal("unit_price"))
                .totalPrice(rs.getBigDecimal("total_price"))
                .build();
    }

    /**
     * Create a new order.
     */
    public Order create(Order order) {
        String sql = sqlLoader.getSql(MODULE_NAME, "create");
        
        String orderNumber = generateOrderNumber();
        LocalDateTime now = LocalDateTime.now();

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("orderNumber", orderNumber)
                .addValue("userId", order.getUserId())
                .addValue("totalAmount", order.getTotalAmount())
                .addValue("status", "PENDING")
                .addValue("shippingAddress", order.getShippingAddress())
                .addValue("billingAddress", order.getBillingAddress())
                .addValue("orderDate", now)
                .addValue("createdAt", now)
                .addValue("updatedAt", now)
                .addValue("createdBy", order.getCreatedBy())
                .addValue("version", 1);

        Number id = executeInsert(sql, params);
        order.setId(id.longValue());
        order.setOrderNumber(orderNumber);
        order.setOrderDate(now);
        order.setVersion(1);
        
        return order;
    }

    /**
     * Create order items.
     */
    public void createOrderItems(List<OrderItem> items) {
        String sql = sqlLoader.getSql(MODULE_NAME, "createItem");
        
        for (OrderItem item : items) {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("orderId", item.getOrderId())
                    .addValue("productId", item.getProductId())
                    .addValue("quantity", item.getQuantity())
                    .addValue("unitPrice", item.getUnitPrice())
                    .addValue("totalPrice", item.getTotalPrice());
            
            Number id = executeInsert(sql, params);
            item.setId(id.longValue());
        }
    }

    /**
     * Update an existing order with optimistic locking.
     */
    public Optional<Order> update(Order order) {
        String sql = sqlLoader.getSql(MODULE_NAME, "update");

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", order.getId())
                .addValue("status", order.getStatus())
                .addValue("shippingAddress", order.getShippingAddress())
                .addValue("billingAddress", order.getBillingAddress())
                .addValue("updatedAt", LocalDateTime.now())
                .addValue("updatedBy", order.getUpdatedBy())
                .addValue("version", order.getVersion())
                .addValue("newVersion", order.getVersion() + 1);

        int affected = executeUpdateWithLocking(sql, params);

        if (affected == 0) {
            return Optional.empty();
        }

        return findById(order.getId());
    }

    /**
     * Find order with items.
     */
    public Optional<Order> findByIdWithItems(Long id) {
        Optional<Order> orderOpt = findById(id);
        
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            List<OrderItem> items = findOrderItems(id);
            order.setItems(items);
            return Optional.of(order);
        }
        
        return Optional.empty();
    }

    /**
     * Find order items.
     */
    public List<OrderItem> findOrderItems(Long orderId) {
        String sql = sqlLoader.getSql(MODULE_NAME, "findItems");
        MapSqlParameterSource params = new MapSqlParameterSource("orderId", orderId);
        return jdbcTemplate.query(sql, params, getOrderItemRowMapper());
    }

    /**
     * Find orders by user with pagination.
     */
    public List<Order> findByUserId(Long userId, PageRequest pageRequest) {
        String baseSql = sqlLoader.getSql(MODULE_NAME, "findByUserId");
        String paginationClause = paginationHelper.buildPaginationClause(pageRequest);
        String sql = baseSql + paginationClause;

        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        return jdbcTemplate.query(sql, params, getRowMapper());
    }

    /**
     * Count orders by user.
     */
    public long countByUserId(Long userId) {
        String sql = sqlLoader.getSql(MODULE_NAME, "countByUserId");
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        Long count = jdbcTemplate.queryForObject(sql, params, Long.class);
        return count != null ? count : 0;
    }

    /**
     * Find orders by status with pagination.
     */
    public List<Order> findByStatus(String status, PageRequest pageRequest) {
        String baseSql = sqlLoader.getSql(MODULE_NAME, "findByStatus");
        String paginationClause = paginationHelper.buildPaginationClause(pageRequest);
        String sql = baseSql + paginationClause;

        MapSqlParameterSource params = new MapSqlParameterSource("status", status);
        return jdbcTemplate.query(sql, params, getRowMapper());
    }

    /**
     * Find order by order number.
     */
    public Optional<Order> findByOrderNumber(String orderNumber) {
        String sql = sqlLoader.getSql(MODULE_NAME, "findByOrderNumber");
        MapSqlParameterSource params = new MapSqlParameterSource("orderNumber", orderNumber);

        List<Order> results = jdbcTemplate.query(sql, params, getRowMapper());
        if (!results.isEmpty()) {
            Order order = results.get(0);
            order.setItems(findOrderItems(order.getId()));
            return Optional.of(order);
        }
        return Optional.empty();
    }

    /**
     * Update order status.
     */
    public boolean updateStatus(Long id, String status, int currentVersion) {
        String sql = sqlLoader.getSql(MODULE_NAME, "updateStatus");

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("status", status)
                .addValue("updatedAt", LocalDateTime.now())
                .addValue("version", currentVersion)
                .addValue("newVersion", currentVersion + 1);

        int affected = jdbcTemplate.update(sql, params);
        return affected > 0;
    }

    private String generateOrderNumber() {
        return "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
