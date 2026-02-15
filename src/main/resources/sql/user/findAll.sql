-- Find all users
-- Retrieves all non-deleted users (pagination is added dynamically)
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
WHERE deleted = false
