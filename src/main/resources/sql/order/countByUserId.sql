-- Count orders by user ID
SELECT COUNT(*) FROM orders WHERE user_id = :userId AND deleted = false
