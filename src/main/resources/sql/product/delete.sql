-- Soft delete product
UPDATE products SET
    deleted = true,
    updated_at = CURRENT_TIMESTAMP
WHERE id = :id AND deleted = false
