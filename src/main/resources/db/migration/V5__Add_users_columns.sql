-- Add additional columns to users table required by admin module
ALTER TABLE users ADD COLUMN IF NOT EXISTS nickname VARCHAR(100) NULL;
ALTER TABLE users ADD COLUMN IF NOT EXISTS email VARCHAR(255) NULL;
ALTER TABLE users ADD COLUMN IF NOT EXISTS sanction_status VARCHAR(20) NULL;
ALTER TABLE users ADD COLUMN IF NOT EXISTS phone VARCHAR(20) NULL;

-- Add comments for new columns
COMMENT ON COLUMN users.nickname IS '닉네임';
COMMENT ON COLUMN users.email IS '이메일';
COMMENT ON COLUMN users.sanction_status IS '제재 상태 (NONE, WARNING, SUSPENDED, BANNED 등)';
COMMENT ON COLUMN users.phone IS '전화번호';

-- Add indexes for new columns
CREATE INDEX IF NOT EXISTS idx_users_nickname ON users(nickname);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_sanction_status ON users(sanction_status);

