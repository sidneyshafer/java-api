-- Find product by ID
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
WHERE id = :id AND deleted = false
