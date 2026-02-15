package com.api.dto.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for updating an existing product.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProductRequest {
    
    @Size(max = 50, message = "SKU cannot exceed 50 characters")
    private String sku;
    
    @Size(min = 1, max = 200, message = "Name must be between 1 and 200 characters")
    private String name;
    
    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;
    
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price;
    
    @Min(value = 0, message = "Quantity cannot be negative")
    private Integer quantity;
    
    @Size(max = 100, message = "Category cannot exceed 100 characters")
    private String category;
    
    private String status;
    
    @NotNull(message = "Version is required for optimistic locking")
    private Integer version;
}
