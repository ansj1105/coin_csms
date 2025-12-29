-- Add role column to users table
-- 0: 테스트, 1: 유저, 2: 어드민, 3: 슈퍼어드민, NULL 허용

ALTER TABLE users ADD COLUMN role INT NULL;

COMMENT ON COLUMN users.role IS '역할 (0=테스트, 1=유저, 2=어드민, 3=슈퍼어드민, NULL=미설정)';

CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);

