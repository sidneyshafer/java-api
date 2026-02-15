-- Update order with optimistic locking
UPDATE orders SET
    status = :status,
    shipping_address = :shippingAddress,
    billing_address = :billingAddress,
    updated_at = :updatedAt,
    updated_by = :updatedBy,
    version = :newVersion
WHERE id = :id 
    AND version = :version 
    AND deleted = false
