package com.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Base entity with common audit fields.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseEntity {
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    private boolean deleted = false;
    private Integer version; // For optimistic locking
}
