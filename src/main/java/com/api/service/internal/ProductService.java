package com.api.service.internal;

import com.api.dto.common.PageRequest;
import com.api.dto.common.PageResponse;
import com.api.dto.product.CreateProductRequest;
import com.api.dto.product.ProductResponse;
import com.api.dto.product.UpdateProductRequest;
import com.api.exception.ConflictException;
import com.api.exception.OptimisticLockException;
import com.api.exception.ResourceNotFoundException;
import com.api.model.Product;
import com.api.repository.ProductRepository;
import com.api.util.PaginationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Internal service for product business logic.
 */
@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final PaginationHelper paginationHelper;

    public ProductService(ProductRepository productRepository, PaginationHelper paginationHelper) {
        this.productRepository = productRepository;
        this.paginationHelper = paginationHelper;
    }

    /**
     * Create a new product.
     */
    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        log.info("Creating product with SKU: {}", request.getSku());

        // Check for duplicate SKU
        if (productRepository.existsBySku(request.getSku())) {
            throw new ConflictException("Product with SKU already exists: " + request.getSku());
        }

        Product product = Product.builder()
                .sku(request.getSku())
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .quantity(request.getQuantity())
                .category(request.getCategory())
                .status("ACTIVE")
                .build();

        Product savedProduct = productRepository.create(product);
        log.info("Product created with ID: {}", savedProduct.getId());

        return ProductResponse.fromEntity(savedProduct);
    }

    /**
     * Get product by ID.
     */
    @Cacheable(value = "products", key = "#id")
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        log.debug("Fetching product with ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));

        return ProductResponse.fromEntity(product);
    }

    /**
     * Get product by SKU.
     */
    @Transactional(readOnly = true)
    public ProductResponse getProductBySku(String sku) {
        log.debug("Fetching product with SKU: {}", sku);

        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with SKU: " + sku));

        return ProductResponse.fromEntity(product);
    }

    /**
     * Get all products with pagination.
     */
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getAllProducts(PageRequest pageRequest) {
        log.debug("Fetching all products with pagination: {}", pageRequest);

        PageRequest normalized = paginationHelper.normalize(pageRequest);
        List<Product> products = productRepository.findAll(normalized);
        long totalCount = productRepository.count();

        List<ProductResponse> responses = products.stream()
                .map(ProductResponse::fromEntity)
                .collect(Collectors.toList());

        return paginationHelper.buildPageResponse(responses, totalCount, normalized);
    }

    /**
     * Get products by category with pagination.
     */
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getProductsByCategory(String category, PageRequest pageRequest) {
        log.debug("Fetching products by category: {} with pagination: {}", category, pageRequest);

        PageRequest normalized = paginationHelper.normalize(pageRequest);
        List<Product> products = productRepository.findByCategory(category, normalized);
        long totalCount = productRepository.countByCategory(category);

        List<ProductResponse> responses = products.stream()
                .map(ProductResponse::fromEntity)
                .collect(Collectors.toList());

        return paginationHelper.buildPageResponse(responses, totalCount, normalized);
    }

    /**
     * Search products by name.
     */
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> searchProducts(String searchTerm, PageRequest pageRequest) {
        log.debug("Searching products with term: {} and pagination: {}", searchTerm, pageRequest);

        PageRequest normalized = paginationHelper.normalize(pageRequest);
        List<Product> products = productRepository.searchByName(searchTerm, normalized);

        List<ProductResponse> responses = products.stream()
                .map(ProductResponse::fromEntity)
                .collect(Collectors.toList());

        return paginationHelper.buildPageResponse(responses, responses.size(), normalized);
    }

    /**
     * Update an existing product.
     */
    @CacheEvict(value = "products", key = "#id")
    @Transactional
    public ProductResponse updateProduct(Long id, UpdateProductRequest request) {
        log.info("Updating product with ID: {}", id);

        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));

        // Check for SKU conflict if SKU is being changed
        if (request.getSku() != null && !request.getSku().equals(existingProduct.getSku())) {
            if (productRepository.existsBySku(request.getSku())) {
                throw new ConflictException("Product with SKU already exists: " + request.getSku());
            }
            existingProduct.setSku(request.getSku());
        }

        // Update fields
        if (request.getName() != null) {
            existingProduct.setName(request.getName());
        }
        if (request.getDescription() != null) {
            existingProduct.setDescription(request.getDescription());
        }
        if (request.getPrice() != null) {
            existingProduct.setPrice(request.getPrice());
        }
        if (request.getQuantity() != null) {
            existingProduct.setQuantity(request.getQuantity());
        }
        if (request.getCategory() != null) {
            existingProduct.setCategory(request.getCategory());
        }
        if (request.getStatus() != null) {
            existingProduct.setStatus(request.getStatus());
        }

        existingProduct.setVersion(request.getVersion());

        Product updatedProduct = productRepository.update(existingProduct)
                .orElseThrow(() -> new OptimisticLockException(
                        "Product was modified by another transaction. Please refresh and try again."));

        log.info("Product updated successfully with ID: {}", id);
        return ProductResponse.fromEntity(updatedProduct);
    }

    /**
     * Update product quantity (inventory management).
     */
    @CacheEvict(value = "products", key = "#id")
    @Transactional
    public ProductResponse updateProductQuantity(Long id, int quantityChange) {
        log.info("Updating quantity for product ID: {} by {}", id, quantityChange);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));

        boolean updated = productRepository.updateQuantity(id, quantityChange, product.getVersion());
        if (!updated) {
            throw new OptimisticLockException(
                    "Product was modified by another transaction. Please refresh and try again.");
        }

        return getProductById(id);
    }

    /**
     * Soft delete a product.
     */
    @CacheEvict(value = "products", key = "#id")
    @Transactional
    public void deleteProduct(Long id) {
        log.info("Deleting product with ID: {}", id);

        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found with ID: " + id);
        }

        boolean deleted = productRepository.deleteById(id);
        if (!deleted) {
            throw new ResourceNotFoundException("Product not found with ID: " + id);
        }

        log.info("Product deleted successfully with ID: {}", id);
    }
}
