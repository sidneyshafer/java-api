-- Update product with optimistic locking
UPDATE products SET
    sku = :sku,
    name = :name,
    description = :description,
    price = :price,
    quantity = :quantity,
    category = :category,
    status = :status,
    updated_at = :updatedAt,
    updated_by = :updatedBy,
    version = :newVersion
WHERE id = :id 
    AND version = :version 
    AND deleted = false
