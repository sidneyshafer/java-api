-- Check if product exists by ID
SELECT COUNT(*) FROM products WHERE id = :id AND deleted = false
