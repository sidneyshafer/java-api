-- Soft delete order
UPDATE orders SET
    deleted = true,
    status = 'CANCELLED',
    updated_at = CURRENT_TIMESTAMP
WHERE id = :id AND deleted = false
