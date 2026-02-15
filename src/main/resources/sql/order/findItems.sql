-- Find order items by order ID
SELECT 
    id,
    order_id,
    product_id,
    quantity,
    unit_price,
    total_price
FROM order_items
WHERE order_id = :orderId
