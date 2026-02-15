-- Find all products
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
WHERE deleted = false
