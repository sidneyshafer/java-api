package com.api.controller;

import com.api.dto.common.ApiResponse;
import com.api.dto.common.PageRequest;
import com.api.dto.common.PageResponse;
import com.api.dto.order.CreateOrderRequest;
import com.api.dto.order.OrderResponse;
import com.api.dto.order.UpdateOrderRequest;
import com.api.service.internal.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for order operations.
 * API versioning: /api/v1/orders
 */
@RestController
@RequestMapping("/orders")
@Tag(name = "Orders", description = "Order management operations")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @Operation(summary = "Create a new order", description = "Creates a new order with the provided details")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Order created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User or product not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Insufficient inventory")
    })
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {
        
        OrderResponse order = orderService.createOrder(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(order, "Order created successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID", description = "Retrieves an order by its unique identifier")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @Parameter(description = "Order ID") @PathVariable Long id) {
        
        OrderResponse order = orderService.getOrderById(id);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @GetMapping("/number/{orderNumber}")
    @Operation(summary = "Get order by order number", description = "Retrieves an order by its order number")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderByOrderNumber(
            @Parameter(description = "Order number") @PathVariable String orderNumber) {
        
        OrderResponse order = orderService.getOrderByOrderNumber(orderNumber);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @GetMapping
    @Operation(summary = "Get all orders", description = "Retrieves a paginated list of all orders")
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getAllOrders(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort by field") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction (ASC/DESC)") @RequestParam(defaultValue = "DESC") String sortDirection) {
        
        PageRequest pageRequest = new PageRequest(page, size, sortBy, sortDirection);
        PageResponse<OrderResponse> orders = orderService.getAllOrders(pageRequest);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get orders by user", description = "Retrieves a paginated list of orders for a specific user")
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getOrdersByUser(
            @Parameter(description = "User ID") @PathVariable Long userId,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort by field") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction (ASC/DESC)") @RequestParam(defaultValue = "DESC") String sortDirection) {
        
        PageRequest pageRequest = new PageRequest(page, size, sortBy, sortDirection);
        PageResponse<OrderResponse> orders = orderService.getOrdersByUser(userId, pageRequest);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get orders by status", description = "Retrieves a paginated list of orders by status")
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getOrdersByStatus(
            @Parameter(description = "Order status") @PathVariable String status,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort by field") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction (ASC/DESC)") @RequestParam(defaultValue = "DESC") String sortDirection) {
        
        PageRequest pageRequest = new PageRequest(page, size, sortBy, sortDirection);
        PageResponse<OrderResponse> orders = orderService.getOrdersByStatus(status, pageRequest);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update order", description = "Updates an existing order")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Concurrent modification detected"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Invalid status transition")
    })
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrder(
            @Parameter(description = "Order ID") @PathVariable Long id,
            @Valid @RequestBody UpdateOrderRequest request) {
        
        OrderResponse order = orderService.updateOrder(id, request);
        return ResponseEntity.ok(ApiResponse.success(order, "Order updated successfully"));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update order status", description = "Updates the status of an order")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order status updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Invalid status transition")
    })
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @Parameter(description = "Order ID") @PathVariable Long id,
            @Parameter(description = "New status") @RequestParam String status) {
        
        OrderResponse order = orderService.updateOrderStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success(order, "Order status updated successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel order", description = "Cancels an order (soft delete)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Order cancelled successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Order cannot be cancelled")
    })
    public ResponseEntity<Void> cancelOrder(
            @Parameter(description = "Order ID") @PathVariable Long id) {
        
        orderService.cancelOrder(id);
        return ResponseEntity.noContent().build();
    }
}
