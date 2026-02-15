-- Soft delete user
-- Sets the deleted flag to true instead of removing the record
UPDATE users SET
    deleted = true,
    updated_at = CURRENT_TIMESTAMP
WHERE id = :id AND deleted = false
