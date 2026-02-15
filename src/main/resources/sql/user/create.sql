-- User creation SQL
-- Insert a new user into the users table
INSERT INTO users (
    email,
    first_name,
    last_name,
    phone,
    status,
    role,
    created_at,
    updated_at,
    created_by,
    deleted,
    version
) VALUES (
    :email,
    :firstName,
    :lastName,
    :phone,
    :status,
    :role,
    :createdAt,
    :updatedAt,
    :createdBy,
    false,
    :version
)
