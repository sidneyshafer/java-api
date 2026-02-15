-- Find user by ID
-- Retrieves a single user by their unique identifier
SELECT 
    id,
    email,
    first_name,
    last_name,
    phone,
    status,
    role,
    created_at,
    updated_at,
    deleted,
    version
FROM users
WHERE id = :id AND deleted = false
