-- Check if SKU exists
SELECT COUNT(*) FROM products WHERE sku = :sku AND deleted = false
