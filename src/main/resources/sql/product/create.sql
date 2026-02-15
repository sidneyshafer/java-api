-- Product creation SQL
INSERT INTO products (
    sku,
    name,
    description,
    price,
    quantity,
    category,
    status,
    created_at,
    updated_at,
    created_by,
    deleted,
    version
) VALUES (
    :sku,
    :name,
    :description,
    :price,
    :quantity,
    :category,
    :status,
    :createdAt,
    :updatedAt,
    :createdBy,
    false,
    :version
)
