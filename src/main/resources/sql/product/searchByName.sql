-- Search products by name
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
WHERE LOWER(name) LIKE LOWER(:searchTerm) AND deleted = false
