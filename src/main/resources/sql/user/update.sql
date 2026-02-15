-- Update user
-- Updates an existing user with optimistic locking
UPDATE users SET
    email = :email,
    first_name = :firstName,
    last_name = :lastName,
    phone = :phone,
    status = :status,
    role = :role,
    updated_at = :updatedAt,
    updated_by = :updatedBy,
    version = :newVersion
WHERE id = :id 
    AND version = :version 
    AND deleted = false
