-- Count products by category
SELECT COUNT(*) FROM products WHERE category = :category AND deleted = false
