-- 누락된 컬럼 추가 및 wallets 테이블 참조 수정

-- users 테이블에 kori_points 컬럼 추가
ALTER TABLE users ADD COLUMN IF NOT EXISTS kori_points DECIMAL(36, 18) DEFAULT 0 NOT NULL;

COMMENT ON COLUMN users.kori_points IS 'KORI 포인트 잔액';

-- users 테이블에 transaction_password 컬럼 추가 (transaction_password_hash와 별도로 평문 저장용)
-- 주의: 실제 운영 환경에서는 해시만 저장하는 것이 안전하지만, 테스트/개발 환경을 위해 추가
ALTER TABLE users ADD COLUMN IF NOT EXISTS transaction_password VARCHAR(255) NULL;

COMMENT ON COLUMN users.transaction_password IS '거래 비밀번호 (해시 또는 평문, 개발/테스트용)';

-- wallets 테이블에 대한 VIEW 생성 (기존 코드 호환성을 위해)
-- 실제로는 user_wallets 테이블을 사용하지만, 코드에서 wallets를 참조하는 경우를 대비
CREATE OR REPLACE VIEW wallets AS
SELECT 
    id,
    user_id,
    currency_id,
    address,
    private_key,
    tag_memo,
    balance,
    locked_balance,
    status,
    last_sync_height,
    created_at,
    updated_at
FROM user_wallets;

COMMENT ON VIEW wallets IS 'user_wallets 테이블에 대한 VIEW (기존 코드 호환성)';

