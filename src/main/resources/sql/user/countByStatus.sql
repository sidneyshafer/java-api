-- Count users by status
SELECT COUNT(*) FROM users WHERE status = :status AND deleted = false
