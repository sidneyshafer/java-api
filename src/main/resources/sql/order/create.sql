-- Order creation SQL
INSERT INTO orders (
    order_number,
    user_id,
    total_amount,
    status,
    shipping_address,
    billing_address,
    order_date,
    created_at,
    updated_at,
    created_by,
    deleted,
    version
) VALUES (
    :orderNumber,
    :userId,
    :totalAmount,
    :status,
    :shippingAddress,
    :billingAddress,
    :orderDate,
    :createdAt,
    :updatedAt,
    :createdBy,
    false,
    :version
)
