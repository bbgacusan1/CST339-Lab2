CREATE TABLE IF NOT EXISTS users (
    user_id SERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'USER',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    password_change_required BOOLEAN NOT NULL DEFAULT TRUE
);

COMMENT ON TABLE users IS 'Stores application users for registration and authentication.';
COMMENT ON COLUMN users.user_id IS 'Primary key for the user record.';
COMMENT ON COLUMN users.username IS 'Unique username used for sign-in.';
COMMENT ON COLUMN users.password IS 'Stored password hash.';
COMMENT ON COLUMN users.role IS 'User role such as USER or ADMIN.';
COMMENT ON COLUMN users.enabled IS 'Whether the account is enabled.';
COMMENT ON COLUMN users.password_change_required IS 'Whether the user must change their password at next sign-in.';

INSERT INTO users (username, password, role, enabled, password_change_required)
VALUES ('admin', 'password', 'ADMIN', TRUE, TRUE)
ON CONFLICT (username) DO NOTHING;