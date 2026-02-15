-- Check if order exists by ID
SELECT COUNT(*) FROM orders WHERE id = :id AND deleted = false
