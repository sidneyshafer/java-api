-- Find users by status
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
WHERE status = :status AND deleted = false
