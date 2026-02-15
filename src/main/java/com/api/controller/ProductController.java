package com.api.controller;

import com.api.dto.common.ApiResponse;
import com.api.dto.common.PageRequest;
import com.api.dto.common.PageResponse;
import com.api.dto.product.CreateProductRequest;
import com.api.dto.product.ProductResponse;
import com.api.dto.product.UpdateProductRequest;
import com.api.service.internal.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for product operations.
 * API versioning: /api/v1/products
 */
@RestController
@RequestMapping("/products")
@Tag(name = "Products", description = "Product management operations")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    @Operation(summary = "Create a new product", description = "Creates a new product with the provided details")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Product created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "SKU already exists")
    })
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody CreateProductRequest request) {
        
        ProductResponse product = productService.createProduct(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(product, "Product created successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID", description = "Retrieves a product by its unique identifier")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(
            @Parameter(description = "Product ID") @PathVariable Long id) {
        
        ProductResponse product = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    @GetMapping("/sku/{sku}")
    @Operation(summary = "Get product by SKU", description = "Retrieves a product by its SKU")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductBySku(
            @Parameter(description = "Product SKU") @PathVariable String sku) {
        
        ProductResponse product = productService.getProductBySku(sku);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    @GetMapping
    @Operation(summary = "Get all products", description = "Retrieves a paginated list of all products")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getAllProducts(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort by field") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction (ASC/DESC)") @RequestParam(defaultValue = "ASC") String sortDirection) {
        
        PageRequest pageRequest = new PageRequest(page, size, sortBy, sortDirection);
        PageResponse<ProductResponse> products = productService.getAllProducts(pageRequest);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Get products by category", description = "Retrieves a paginated list of products by category")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getProductsByCategory(
            @Parameter(description = "Product category") @PathVariable String category,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort by field") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction (ASC/DESC)") @RequestParam(defaultValue = "ASC") String sortDirection) {
        
        PageRequest pageRequest = new PageRequest(page, size, sortBy, sortDirection);
        PageResponse<ProductResponse> products = productService.getProductsByCategory(category, pageRequest);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/search")
    @Operation(summary = "Search products", description = "Search products by name")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> searchProducts(
            @Parameter(description = "Search term") @RequestParam String q,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort by field") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction (ASC/DESC)") @RequestParam(defaultValue = "ASC") String sortDirection) {
        
        PageRequest pageRequest = new PageRequest(page, size, sortBy, sortDirection);
        PageResponse<ProductResponse> products = productService.searchProducts(q, pageRequest);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update product", description = "Updates an existing product")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Concurrent modification detected")
    })
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @Parameter(description = "Product ID") @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest request) {
        
        ProductResponse product = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.success(product, "Product updated successfully"));
    }

    @PatchMapping("/{id}/quantity")
    @Operation(summary = "Update product quantity", description = "Updates the quantity of a product")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProductQuantity(
            @Parameter(description = "Product ID") @PathVariable Long id,
            @Parameter(description = "Quantity change (positive to add, negative to subtract)") @RequestParam int change) {
        
        ProductResponse product = productService.updateProductQuantity(id, change);
        return ResponseEntity.ok(ApiResponse.success(product, "Product quantity updated successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete product", description = "Soft deletes a product")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Product deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<Void> deleteProduct(
            @Parameter(description = "Product ID") @PathVariable Long id) {
        
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
