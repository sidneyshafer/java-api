package com.api.service.internal;

import com.api.dto.common.PageRequest;
import com.api.dto.common.PageResponse;
import com.api.dto.order.CreateOrderRequest;
import com.api.dto.order.OrderResponse;
import com.api.dto.order.UpdateOrderRequest;
import com.api.exception.BusinessException;
import com.api.exception.OptimisticLockException;
import com.api.exception.ResourceNotFoundException;
import com.api.model.Order;
import com.api.model.OrderItem;
import com.api.model.Product;
import com.api.repository.OrderRepository;
import com.api.repository.ProductRepository;
import com.api.repository.UserRepository;
import com.api.util.PaginationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Internal service for order business logic.
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final PaginationHelper paginationHelper;

    public OrderService(OrderRepository orderRepository,
                       UserRepository userRepository,
                       ProductRepository productRepository,
                       PaginationHelper paginationHelper) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.paginationHelper = paginationHelper;
    }

    /**
     * Create a new order.
     */
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Creating order for user: {}", request.getUserId());

        // Validate user exists
        if (!userRepository.existsById(request.getUserId())) {
            throw new ResourceNotFoundException("User not found with ID: " + request.getUserId());
        }

        // Validate products and calculate total
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (CreateOrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product not found with ID: " + itemRequest.getProductId()));

            // Check inventory
            if (product.getQuantity() < itemRequest.getQuantity()) {
                throw new BusinessException(
                        "Insufficient inventory for product: " + product.getName() +
                        ". Available: " + product.getQuantity() + ", Requested: " + itemRequest.getQuantity());
            }

            BigDecimal itemTotal = product.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);

            OrderItem orderItem = OrderItem.builder()
                    .productId(product.getId())
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(product.getPrice())
                    .totalPrice(itemTotal)
                    .build();
            orderItems.add(orderItem);
        }

        // Create order
        Order order = Order.builder()
                .userId(request.getUserId())
                .totalAmount(totalAmount)
                .shippingAddress(request.getShippingAddress())
                .billingAddress(request.getBillingAddress())
                .status("PENDING")
                .build();

        Order savedOrder = orderRepository.create(order);

        // Create order items and update inventory
        for (int i = 0; i < orderItems.size(); i++) {
            OrderItem item = orderItems.get(i);
            item.setOrderId(savedOrder.getId());
            
            // Decrease product inventory
            productRepository.updateQuantity(item.getProductId(), -item.getQuantity(),
                    productRepository.findById(item.getProductId()).get().getVersion());
        }
        orderRepository.createOrderItems(orderItems);

        savedOrder.setItems(orderItems);
        log.info("Order created with ID: {} and order number: {}", savedOrder.getId(), savedOrder.getOrderNumber());

        return OrderResponse.fromEntity(savedOrder);
    }

    /**
     * Get order by ID.
     */
    @Cacheable(value = "orders", key = "#id")
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        log.debug("Fetching order with ID: {}", id);

        Order order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + id));

        return OrderResponse.fromEntity(order);
    }

    /**
     * Get order by order number.
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrderByOrderNumber(String orderNumber) {
        log.debug("Fetching order with order number: {}", orderNumber);

        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with order number: " + orderNumber));

        return OrderResponse.fromEntity(order);
    }

    /**
     * Get all orders with pagination.
     */
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getAllOrders(PageRequest pageRequest) {
        log.debug("Fetching all orders with pagination: {}", pageRequest);

        PageRequest normalized = paginationHelper.normalize(pageRequest);
        List<Order> orders = orderRepository.findAll(normalized);
        long totalCount = orderRepository.count();

        // Load items for each order
        orders.forEach(order -> order.setItems(orderRepository.findOrderItems(order.getId())));

        List<OrderResponse> responses = orders.stream()
                .map(OrderResponse::fromEntity)
                .collect(Collectors.toList());

        return paginationHelper.buildPageResponse(responses, totalCount, normalized);
    }

    /**
     * Get orders by user with pagination.
     */
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getOrdersByUser(Long userId, PageRequest pageRequest) {
        log.debug("Fetching orders for user: {} with pagination: {}", userId, pageRequest);

        // Validate user exists
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with ID: " + userId);
        }

        PageRequest normalized = paginationHelper.normalize(pageRequest);
        List<Order> orders = orderRepository.findByUserId(userId, normalized);
        long totalCount = orderRepository.countByUserId(userId);

        // Load items for each order
        orders.forEach(order -> order.setItems(orderRepository.findOrderItems(order.getId())));

        List<OrderResponse> responses = orders.stream()
                .map(OrderResponse::fromEntity)
                .collect(Collectors.toList());

        return paginationHelper.buildPageResponse(responses, totalCount, normalized);
    }

    /**
     * Get orders by status with pagination.
     */
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getOrdersByStatus(String status, PageRequest pageRequest) {
        log.debug("Fetching orders with status: {} and pagination: {}", status, pageRequest);

        PageRequest normalized = paginationHelper.normalize(pageRequest);
        List<Order> orders = orderRepository.findByStatus(status, normalized);

        // Load items for each order
        orders.forEach(order -> order.setItems(orderRepository.findOrderItems(order.getId())));

        List<OrderResponse> responses = orders.stream()
                .map(OrderResponse::fromEntity)
                .collect(Collectors.toList());

        return paginationHelper.buildPageResponse(responses, responses.size(), normalized);
    }

    /**
     * Update an existing order.
     */
    @CacheEvict(value = "orders", key = "#id")
    @Transactional
    public OrderResponse updateOrder(Long id, UpdateOrderRequest request) {
        log.info("Updating order with ID: {}", id);

        Order existingOrder = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + id));

        // Update fields
        if (request.getStatus() != null) {
            validateStatusTransition(existingOrder.getStatus(), request.getStatus());
            existingOrder.setStatus(request.getStatus());
        }
        if (request.getShippingAddress() != null) {
            existingOrder.setShippingAddress(request.getShippingAddress());
        }
        if (request.getBillingAddress() != null) {
            existingOrder.setBillingAddress(request.getBillingAddress());
        }

        existingOrder.setVersion(request.getVersion());

        Order updatedOrder = orderRepository.update(existingOrder)
                .orElseThrow(() -> new OptimisticLockException(
                        "Order was modified by another transaction. Please refresh and try again."));

        updatedOrder.setItems(orderRepository.findOrderItems(updatedOrder.getId()));
        log.info("Order updated successfully with ID: {}", id);

        return OrderResponse.fromEntity(updatedOrder);
    }

    /**
     * Update order status.
     */
    @CacheEvict(value = "orders", key = "#id")
    @Transactional
    public OrderResponse updateOrderStatus(Long id, String status) {
        log.info("Updating status for order ID: {} to {}", id, status);

        Order order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + id));

        validateStatusTransition(order.getStatus(), status);

        boolean updated = orderRepository.updateStatus(id, status, order.getVersion());
        if (!updated) {
            throw new OptimisticLockException(
                    "Order was modified by another transaction. Please refresh and try again.");
        }

        // Handle cancellation - restore inventory
        if ("CANCELLED".equals(status)) {
            restoreInventory(order);
        }

        return getOrderById(id);
    }

    /**
     * Cancel an order (soft delete).
     */
    @CacheEvict(value = "orders", key = "#id")
    @Transactional
    public void cancelOrder(Long id) {
        log.info("Cancelling order with ID: {}", id);

        Order order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + id));

        if ("SHIPPED".equals(order.getStatus()) || "DELIVERED".equals(order.getStatus())) {
            throw new BusinessException("Cannot cancel order that has been shipped or delivered");
        }

        // Restore inventory
        restoreInventory(order);

        // Soft delete
        orderRepository.deleteById(id);
        log.info("Order cancelled successfully with ID: {}", id);
    }

    /**
     * Validate order status transition.
     */
    private void validateStatusTransition(String currentStatus, String newStatus) {
        // Define valid transitions
        switch (currentStatus) {
            case "PENDING" -> {
                if (!"CONFIRMED".equals(newStatus) && !"CANCELLED".equals(newStatus)) {
                    throw new BusinessException("Invalid status transition from " + currentStatus + " to " + newStatus);
                }
            }
            case "CONFIRMED" -> {
                if (!"SHIPPED".equals(newStatus) && !"CANCELLED".equals(newStatus)) {
                    throw new BusinessException("Invalid status transition from " + currentStatus + " to " + newStatus);
                }
            }
            case "SHIPPED" -> {
                if (!"DELIVERED".equals(newStatus)) {
                    throw new BusinessException("Invalid status transition from " + currentStatus + " to " + newStatus);
                }
            }
            case "DELIVERED", "CANCELLED" ->
                throw new BusinessException("Cannot change status of " + currentStatus + " order");
        }
    }

    /**
     * Restore inventory when order is cancelled.
     */
    private void restoreInventory(Order order) {
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                Product product = productRepository.findById(item.getProductId()).orElse(null);
                if (product != null) {
                    productRepository.updateQuantity(item.getProductId(), item.getQuantity(), product.getVersion());
                }
            }
        }
    }
}
