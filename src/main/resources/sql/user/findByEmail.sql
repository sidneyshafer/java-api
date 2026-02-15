-- Find user by email
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
WHERE email = :email AND deleted = false
