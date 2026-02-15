-- Search users by name
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
WHERE (
    LOWER(first_name) LIKE LOWER(:searchTerm) 
    OR LOWER(last_name) LIKE LOWER(:searchTerm)
) AND deleted = false
