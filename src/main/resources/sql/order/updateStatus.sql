-- Update order status with optimistic locking
UPDATE orders SET
    status = :status,
    updated_at = :updatedAt,
    version = :newVersion
WHERE id = :id 
    AND version = :version 
    AND deleted = false
