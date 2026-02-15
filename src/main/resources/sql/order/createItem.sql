-- Create order item
INSERT INTO order_items (
    order_id,
    product_id,
    quantity,
    unit_price,
    total_price
) VALUES (
    :orderId,
    :productId,
    :quantity,
    :unitPrice,
    :totalPrice
)
