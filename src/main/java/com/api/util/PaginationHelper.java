package com.api.util;

import com.api.dto.common.PageRequest;
import com.api.dto.common.PageResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Utility for handling pagination.
 */
@Component
public class PaginationHelper {

    @Value("${app.pagination.default-page-size:20}")
    private int defaultPageSize;

    @Value("${app.pagination.max-page-size:100}")
    private int maxPageSize;

    /**
     * Normalize page request with defaults and limits.
     */
    public PageRequest normalize(PageRequest request) {
        if (request == null) {
            return new PageRequest(0, defaultPageSize, null, null);
        }

        int page = Math.max(0, request.getPage());
        int size = request.getSize();
        
        if (size <= 0) {
            size = defaultPageSize;
        } else if (size > maxPageSize) {
            size = maxPageSize;
        }

        return new PageRequest(page, size, request.getSortBy(), request.getSortDirection());
    }

    /**
     * Calculate offset for SQL pagination.
     */
    public int calculateOffset(int page, int size) {
        return page * size;
    }

    /**
     * Build SQL pagination clause.
     */
    public String buildPaginationClause(PageRequest request) {
        PageRequest normalized = normalize(request);
        int offset = calculateOffset(normalized.getPage(), normalized.getSize());
        
        StringBuilder sql = new StringBuilder();
        
        // Add ORDER BY if sortBy is specified
        if (normalized.getSortBy() != null && !normalized.getSortBy().isBlank()) {
            String direction = "DESC".equalsIgnoreCase(normalized.getSortDirection()) ? "DESC" : "ASC";
            sql.append(" ORDER BY ").append(sanitizeSortColumn(normalized.getSortBy()))
               .append(" ").append(direction);
        }
        
        // Add LIMIT and OFFSET
        sql.append(" LIMIT ").append(normalized.getSize())
           .append(" OFFSET ").append(offset);
        
        return sql.toString();
    }

    /**
     * Sanitize sort column to prevent SQL injection.
     */
    private String sanitizeSortColumn(String column) {
        // Only allow alphanumeric characters and underscores
        return column.replaceAll("[^a-zA-Z0-9_]", "");
    }

    /**
     * Build page response from results.
     */
    public <T> PageResponse<T> buildPageResponse(List<T> content, long totalElements, PageRequest request) {
        PageRequest normalized = normalize(request);
        int totalPages = (int) Math.ceil((double) totalElements / normalized.getSize());
        
        return PageResponse.<T>builder()
                .content(content)
                .page(normalized.getPage())
                .size(normalized.getSize())
                .totalElements(totalElements)
                .totalPages(totalPages)
                .first(normalized.getPage() == 0)
                .last(normalized.getPage() >= totalPages - 1)
                .build();
    }
}
