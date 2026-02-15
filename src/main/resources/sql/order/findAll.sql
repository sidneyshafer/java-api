-- Find all orders
SELECT 
    id,
    order_number,
    user_id,
    total_amount,
    status,
    shipping_address,
    billing_address,
    order_date,
    created_at,
    updated_at,
    deleted,
    version
FROM orders
WHERE deleted = false
