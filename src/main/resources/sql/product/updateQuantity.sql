-- Update product quantity with optimistic locking
UPDATE products SET
    quantity = quantity + :quantityChange,
    updated_at = :updatedAt,
    version = :newVersion
WHERE id = :id 
    AND version = :version 
    AND deleted = false
    AND (quantity + :quantityChange) >= 0
