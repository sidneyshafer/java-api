-- Count all users
-- Returns the total count of non-deleted users
SELECT COUNT(*) FROM users WHERE deleted = false
