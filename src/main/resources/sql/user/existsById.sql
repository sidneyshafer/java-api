-- Check if user exists by ID
SELECT COUNT(*) FROM users WHERE id = :id AND deleted = false
