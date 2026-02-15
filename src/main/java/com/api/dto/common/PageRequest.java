package com.api.dto.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request object for paginated queries.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageRequest {
    
    private int page = 0;
    private int size = 20;
    private String sortBy;
    private String sortDirection = "ASC";
}
