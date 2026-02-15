-- Find product by SKU
SELECT 
    id,
    sku,
    name,
    description,
    price,
    quantity,
    category,
    status,
    created_at,
    updated_at,
    deleted,
    version
FROM products
WHERE sku = :sku AND deleted = false
