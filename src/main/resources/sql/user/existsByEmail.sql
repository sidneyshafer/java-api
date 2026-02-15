-- Check if email exists
SELECT COUNT(*) FROM users WHERE email = :email AND deleted = false
