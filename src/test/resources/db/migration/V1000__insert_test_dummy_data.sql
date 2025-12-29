-- 테스트용 더미 데이터 삽입
-- 이 파일은 테스트 환경에서 사용되는 더미 데이터입니다.

-- 테스트용 Admin 사용자 생성 (비밀번호: password123)
-- BCrypt 해시: $2a$10$fZAPd02/HDjmdMfnpWhp/uSn.BBYDaQta8qVSvo8qWXw/Q45SqFo.
INSERT INTO users (login_id, password_hash, role, status, created_at, updated_at)
VALUES 
    ('admin1', '$2a$10$fZAPd02/HDjmdMfnpWhp/uSn.BBYDaQta8qVSvo8qWXw/Q45SqFo.', 2, 'ACTIVE', NOW(), NOW())
ON CONFLICT (login_id) DO UPDATE SET password_hash = EXCLUDED.password_hash;

-- 테스트용 사용자 생성 (비밀번호: test1234)
-- BCrypt 해시: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
INSERT INTO users (login_id, password_hash, nickname, email, phone, status, level, referral_code, sanction_status, created_at, updated_at)
VALUES 
    ('testuser1', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '테스트유저1', 'test1@example.com', '010-1111-1111', 'ACTIVE', 1, 'TEST001', 'WARNING', NOW() - INTERVAL '30 days', NOW()),
    ('testuser2', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '테스트유저2', 'test2@example.com', '010-2222-2222', 'ACTIVE', 2, 'TEST002', NULL, NOW() - INTERVAL '20 days', NOW()),
    ('testuser3', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '테스트유저3', 'test3@example.com', '010-3333-3333', 'ACTIVE', 1, 'TEST003', 'SUSPENDED', NOW() - INTERVAL '10 days', NOW()),
    ('testuser4', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '테스트유저4', 'test4@example.com', '010-4444-4444', 'ACTIVE', 3, 'TEST004', NULL, NOW() - INTERVAL '5 days', NOW()),
    ('testuser5', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '테스트유저5', 'test5@example.com', '010-5555-5555', 'INACTIVE', 2, 'TEST005', NULL, NOW() - INTERVAL '1 day', NOW())
ON CONFLICT (login_id) DO NOTHING;

-- 추천인 관계 설정
INSERT INTO referral_relations (referrer_id, referred_id, status, created_at)
SELECT 
    u1.id,
    u2.id,
    'ACTIVE',
    NOW() - INTERVAL '15 days'
FROM users u1
CROSS JOIN users u2
WHERE u1.login_id = 'testuser1' AND u2.login_id = 'testuser2'
ON CONFLICT (referred_id, level) DO NOTHING;

INSERT INTO referral_relations (referrer_id, referred_id, status, created_at)
SELECT 
    u1.id,
    u2.id,
    'ACTIVE',
    NOW() - INTERVAL '10 days'
FROM users u1
CROSS JOIN users u2
WHERE u1.login_id = 'testuser1' AND u2.login_id = 'testuser3'
ON CONFLICT (referred_id, level) DO NOTHING;

-- 마이닝 히스토리 데이터
INSERT INTO mining_history (user_id, level, type, amount, created_at, updated_at)
SELECT 
    u.id,
    u.level,
    'BROADCAST_PROGRESS',
    100.5,
    NOW() - INTERVAL '7 days',
    NOW()
FROM users u
WHERE u.login_id = 'testuser1'
ON CONFLICT DO NOTHING;

INSERT INTO mining_history (user_id, level, type, amount, created_at, updated_at)
SELECT 
    u.id,
    u.level,
    'BROADCAST_WATCH',
    50.25,
    NOW() - INTERVAL '6 days',
    NOW()
FROM users u
WHERE u.login_id = 'testuser2'
ON CONFLICT DO NOTHING;

INSERT INTO mining_history (user_id, level, type, amount, created_at, updated_at)
SELECT 
    u.id,
    u.level,
    'BROADCAST_PROGRESS',
    200.75,
    NOW() - INTERVAL '5 days',
    NOW()
FROM users u
WHERE u.login_id = 'testuser1'
ON CONFLICT DO NOTHING;

INSERT INTO mining_history (user_id, level, type, amount, created_at, updated_at)
SELECT 
    u.id,
    u.level,
    'REFERRAL_REWARD',
    10.0,
    NOW() - INTERVAL '4 days',
    NOW()
FROM users u
WHERE u.login_id = 'testuser1'
ON CONFLICT DO NOTHING;

INSERT INTO mining_history (user_id, level, type, amount, created_at, updated_at)
SELECT 
    u.id,
    u.level,
    'BROADCAST_PROGRESS',
    150.0,
    NOW() - INTERVAL '3 days',
    NOW()
FROM users u
WHERE u.login_id = 'testuser3'
ON CONFLICT DO NOTHING;

INSERT INTO mining_history (user_id, level, type, amount, created_at, updated_at)
SELECT 
    u.id,
    u.level,
    'BROADCAST_WATCH',
    75.5,
    NOW() - INTERVAL '2 days',
    NOW()
FROM users u
WHERE u.login_id = 'testuser4'
ON CONFLICT DO NOTHING;

INSERT INTO mining_history (user_id, level, type, amount, created_at, updated_at)
SELECT 
    u.id,
    u.level,
    'BROADCAST_PROGRESS',
    300.0,
    NOW() - INTERVAL '1 day',
    NOW()
FROM users u
WHERE u.login_id = 'testuser1'
ON CONFLICT DO NOTHING;

-- 오늘 날짜 마이닝 데이터
INSERT INTO mining_history (user_id, level, type, amount, created_at, updated_at)
SELECT 
    u.id,
    u.level,
    'BROADCAST_PROGRESS',
    120.0,
    NOW(),
    NOW()
FROM users u
WHERE u.login_id = 'testuser1'
ON CONFLICT DO NOTHING;

-- 일일 마이닝 데이터
INSERT INTO daily_mining (user_id, mining_date, mining_amount, reset_at, created_at, updated_at)
SELECT 
    u.id,
    CURRENT_DATE,
    120.0,
    CURRENT_DATE + INTERVAL '1 day',
    NOW(),
    NOW()
FROM users u
WHERE u.login_id = 'testuser1'
ON CONFLICT (user_id, mining_date) DO NOTHING;

-- 알림 데이터
INSERT INTO notifications (user_id, type, title, message, is_read, created_at, updated_at)
SELECT 
    u.id,
    'INQUIRY',
    '코인문의',
    '테스트 문의 내용입니다.',
    false,
    NOW() - INTERVAL '2 days',
    NOW()
FROM users u
WHERE u.login_id = 'testuser1'
ON CONFLICT DO NOTHING;

INSERT INTO notifications (user_id, type, title, message, is_read, created_at, updated_at)
SELECT 
    u.id,
    'WITHDRAWAL',
    '출금 요청',
    '테스트 출금 요청입니다.',
    false,
    NOW() - INTERVAL '1 day',
    NOW()
FROM users u
WHERE u.login_id = 'testuser2'
ON CONFLICT DO NOTHING;

INSERT INTO notifications (user_id, type, title, message, is_read, created_at, updated_at)
SELECT 
    u.id,
    'ANOMALY',
    '이상 거래',
    '테스트 이상 거래 알림입니다.',
    false,
    NOW(),
    NOW()
FROM users u
WHERE u.login_id = 'testuser3'
ON CONFLICT DO NOTHING;

-- 토큰 입금 데이터 (오늘)
INSERT INTO token_deposits (deposit_id, user_id, order_number, currency_id, amount, network, sender_address, tx_hash, status, created_at)
SELECT 
    'deposit-' || u.id || '-001',
    u.id,
    'ORD-' || u.id || '-001',
    c.id,
    1000.0,
    'TRON',
    'TRON_SENDER_ADDRESS_001',
    'test_tx_hash_001',
    'COMPLETED',
    NOW()
FROM users u
CROSS JOIN currency c
WHERE u.login_id = 'testuser1' AND c.code = 'TRX' AND c.chain = 'TRON'
ON CONFLICT DO NOTHING;

-- 결제 입금 데이터 (오늘)
INSERT INTO payment_deposits (deposit_id, user_id, order_number, currency_id, amount, deposit_method, payment_amount, status, created_at)
SELECT 
    'pay-deposit-' || u.id || '-001',
    u.id,
    'PAY-' || u.id || '-001',
    c.id,
    5000.0,
    'CARD',
    500000.0,
    'COMPLETED',
    NOW()
FROM users u
CROSS JOIN currency c
WHERE u.login_id = 'testuser2' AND c.code = 'USDT' AND c.chain = 'TRON'
ON CONFLICT DO NOTHING;

-- 교환 데이터 (오늘)
INSERT INTO exchanges (exchange_id, user_id, order_number, from_currency_id, to_currency_id, from_amount, to_amount, status, created_at)
SELECT 
    'exchange-' || u.id || '-001',
    u.id,
    'EXC-' || u.id || '-001',
    c1.id,
    c2.id,
    100.0,
    95.0,
    'COMPLETED',
    NOW()
FROM users u
CROSS JOIN currency c1
CROSS JOIN currency c2
WHERE u.login_id = 'testuser3' AND c1.code = 'TRX' AND c1.chain = 'TRON' AND c2.code = 'USDT' AND c2.chain = 'TRON'
ON CONFLICT DO NOTHING;


-- 과거 데이터 (7일 전)
INSERT INTO token_deposits (deposit_id, user_id, order_number, currency_id, amount, network, sender_address, tx_hash, status, created_at)
SELECT 
    'deposit-' || u.id || '-002',
    u.id,
    'ORD-' || u.id || '-002',
    c.id,
    2000.0,
    'TRON',
    'TRON_SENDER_ADDRESS_002',
    'test_tx_hash_002',
    'COMPLETED',
    NOW() - INTERVAL '7 days'
FROM users u
CROSS JOIN currency c
WHERE u.login_id = 'testuser1' AND c.code = 'TRX' AND c.chain = 'TRON'
ON CONFLICT DO NOTHING;

INSERT INTO payment_deposits (deposit_id, user_id, order_number, currency_id, amount, deposit_method, payment_amount, status, created_at)
SELECT 
    'pay-deposit-' || u.id || '-002',
    u.id,
    'PAY-' || u.id || '-002',
    c.id,
    10000.0,
    'BANK_TRANSFER',
    1000000.0,
    'COMPLETED',
    NOW() - INTERVAL '7 days'
FROM users u
CROSS JOIN currency c
WHERE u.login_id = 'testuser2' AND c.code = 'USDT' AND c.chain = 'TRON'
ON CONFLICT DO NOTHING;

INSERT INTO exchanges (exchange_id, user_id, order_number, from_currency_id, to_currency_id, from_amount, to_amount, status, created_at)
SELECT 
    'exchange-' || u.id || '-002',
    u.id,
    'EXC-' || u.id || '-002',
    c1.id,
    c2.id,
    50.0,
    52.5,
    'COMPLETED',
    NOW() - INTERVAL '7 days'
FROM users u
CROSS JOIN currency c1
CROSS JOIN currency c2
WHERE u.login_id = 'testuser3' AND c1.code = 'USDT' AND c1.chain = 'TRON' AND c2.code = 'TRX' AND c2.chain = 'TRON'
ON CONFLICT DO NOTHING;

-- 스왑 데이터
INSERT INTO swaps (swap_id, user_id, order_number, from_currency_id, to_currency_id, from_amount, to_amount, network, status, created_at)
SELECT 
    'swap-' || u.id || '-001',
    u.id,
    'SWP-' || u.id || '-001',
    c1.id,
    c2.id,
    100.0,
    95.0,
    'TRON',
    'COMPLETED',
    NOW() - INTERVAL '5 days'
FROM users u
CROSS JOIN currency c1
CROSS JOIN currency c2
WHERE u.login_id = 'testuser1' AND c1.code = 'TRX' AND c1.chain = 'TRON' AND c2.code = 'USDT' AND c2.chain = 'TRON'
ON CONFLICT DO NOTHING;

-- Currency 데이터는 이미 V13에서 추가됨 (중복 방지를 위해 제거)

-- 지갑 데이터 (testuser1용 - testGetMemberWallets 테스트)
-- 주의: currency 테이블에 KRWT가 없을 수 있으므로 TRX와 USDT만 추가
INSERT INTO user_wallets (user_id, currency_id, address, balance, created_at, updated_at)
SELECT 
    u.id,
    c.id,
    'TR-TRX1234567890123456789012345678901234567890',
    50000.0,
    NOW(),
    NOW()
FROM users u
CROSS JOIN currency c
WHERE u.login_id = 'testuser1' AND c.code = 'TRX' AND c.chain = 'TRON'
ON CONFLICT (user_id, currency_id) DO NOTHING;

INSERT INTO user_wallets (user_id, currency_id, address, balance, created_at, updated_at)
SELECT 
    u.id,
    c.id,
    'TR-USDT1234567890123456789012345678901234567890',
    10000.0,
    NOW(),
    NOW()
FROM users u
CROSS JOIN currency c
WHERE u.login_id = 'testuser1' AND c.code = 'USDT' AND c.chain = 'TRON'
ON CONFLICT (user_id, currency_id) DO NOTHING;

-- 외부 전송 데이터 (오늘) - wallet_id는 user_wallets에서 가져옴
INSERT INTO external_transfers (transfer_id, user_id, wallet_id, currency_id, to_address, amount, fee, network_fee, chain, status, created_at)
SELECT 
    'transfer-' || u.id || '-001',
    u.id,
    uw.id,
    c.id,
    'test_address_001',
    200.0,
    5.0,
    0.0,
    'TRON',
    'PENDING',
    NOW()
FROM users u
CROSS JOIN currency c
INNER JOIN user_wallets uw ON uw.user_id = u.id AND uw.currency_id = c.id
WHERE u.login_id = 'testuser1' AND c.code = 'TRX' AND c.chain = 'TRON'
ON CONFLICT DO NOTHING;

-- 외부 전송 데이터 (7일 전)
INSERT INTO external_transfers (transfer_id, user_id, wallet_id, currency_id, to_address, amount, fee, network_fee, chain, status, created_at)
SELECT 
    'transfer-' || u.id || '-002',
    u.id,
    uw.id,
    c.id,
    'test_address_002',
    500.0,
    10.0,
    0.0,
    'TRON',
    'COMPLETED',
    NOW() - INTERVAL '7 days'
FROM users u
CROSS JOIN currency c
INNER JOIN user_wallets uw ON uw.user_id = u.id AND uw.currency_id = c.id
WHERE u.login_id = 'testuser1' AND c.code = 'TRX' AND c.chain = 'TRON'
ON CONFLICT DO NOTHING;

-- 더 많은 추천인 관계 (TopReferrers 테스트용)
INSERT INTO referral_relations (referrer_id, referred_id, status, created_at)
SELECT 
    u1.id,
    u2.id,
    'ACTIVE',
    NOW() - INTERVAL '8 days'
FROM users u1
CROSS JOIN users u2
WHERE u1.login_id = 'testuser1' AND u2.login_id = 'testuser4'
ON CONFLICT (referred_id, level) DO NOTHING;

INSERT INTO referral_relations (referrer_id, referred_id, status, created_at)
SELECT 
    u1.id,
    u2.id,
    'ACTIVE',
    NOW() - INTERVAL '6 days'
FROM users u1
CROSS JOIN users u2
WHERE u1.login_id = 'testuser1' AND u2.login_id = 'testuser5'
ON CONFLICT (referred_id, level) DO NOTHING;

INSERT INTO referral_relations (referrer_id, referred_id, status, created_at)
SELECT 
    u1.id,
    u2.id,
    'ACTIVE',
    NOW() - INTERVAL '4 days'
FROM users u1
CROSS JOIN users u2
WHERE u1.login_id = 'testuser2' AND u2.login_id = 'testuser4'
ON CONFLICT (referred_id, level) DO NOTHING;

-- 더 많은 마이닝 히스토리 데이터 (차트 데이터용 - 다양한 날짜)
INSERT INTO mining_history (user_id, level, type, amount, created_at, updated_at)
SELECT 
    u.id,
    u.level,
    'BROADCAST_PROGRESS',
    80.0,
    NOW() - INTERVAL '14 days',
    NOW() - INTERVAL '14 days'
FROM users u
WHERE u.login_id = 'testuser1'
ON CONFLICT DO NOTHING;

INSERT INTO mining_history (user_id, level, type, amount, created_at, updated_at)
SELECT 
    u.id,
    u.level,
    'BROADCAST_PROGRESS',
    90.0,
    NOW() - INTERVAL '13 days',
    NOW() - INTERVAL '13 days'
FROM users u
WHERE u.login_id = 'testuser1'
ON CONFLICT DO NOTHING;

INSERT INTO mining_history (user_id, level, type, amount, created_at, updated_at)
SELECT 
    u.id,
    u.level,
    'BROADCAST_PROGRESS',
    110.0,
    NOW() - INTERVAL '12 days',
    NOW() - INTERVAL '12 days'
FROM users u
WHERE u.login_id = 'testuser1'
ON CONFLICT DO NOTHING;

INSERT INTO mining_history (user_id, level, type, amount, created_at, updated_at)
SELECT 
    u.id,
    u.level,
    'BROADCAST_PROGRESS',
    130.0,
    NOW() - INTERVAL '11 days',
    NOW() - INTERVAL '11 days'
FROM users u
WHERE u.login_id = 'testuser1'
ON CONFLICT DO NOTHING;

INSERT INTO mining_history (user_id, level, type, amount, created_at, updated_at)
SELECT 
    u.id,
    u.level,
    'BROADCAST_PROGRESS',
    140.0,
    NOW() - INTERVAL '10 days',
    NOW() - INTERVAL '10 days'
FROM users u
WHERE u.login_id = 'testuser1'
ON CONFLICT DO NOTHING;

INSERT INTO mining_history (user_id, level, type, amount, created_at, updated_at)
SELECT 
    u.id,
    u.level,
    'BROADCAST_PROGRESS',
    160.0,
    NOW() - INTERVAL '9 days',
    NOW() - INTERVAL '9 days'
FROM users u
WHERE u.login_id = 'testuser1'
ON CONFLICT DO NOTHING;

INSERT INTO mining_history (user_id, level, type, amount, created_at, updated_at)
SELECT 
    u.id,
    u.level,
    'BROADCAST_PROGRESS',
    170.0,
    NOW() - INTERVAL '8 days',
    NOW() - INTERVAL '8 days'
FROM users u
WHERE u.login_id = 'testuser1'
ON CONFLICT DO NOTHING;

-- 일일 마이닝 데이터 추가 (과거 날짜)
INSERT INTO daily_mining (user_id, mining_date, mining_amount, reset_at, created_at, updated_at)
SELECT 
    u.id,
    CURRENT_DATE - INTERVAL '1 day',
    300.0,
    CURRENT_DATE,
    NOW() - INTERVAL '1 day',
    NOW() - INTERVAL '1 day'
FROM users u
WHERE u.login_id = 'testuser1'
ON CONFLICT (user_id, mining_date) DO NOTHING;

INSERT INTO daily_mining (user_id, mining_date, mining_amount, reset_at, created_at, updated_at)
SELECT 
    u.id,
    CURRENT_DATE - INTERVAL '2 days',
    250.0,
    CURRENT_DATE - INTERVAL '1 day',
    NOW() - INTERVAL '2 days',
    NOW() - INTERVAL '2 days'
FROM users u
WHERE u.login_id = 'testuser1'
ON CONFLICT (user_id, mining_date) DO NOTHING;

INSERT INTO daily_mining (user_id, mining_date, mining_amount, reset_at, created_at, updated_at)
SELECT 
    u.id,
    CURRENT_DATE - INTERVAL '3 days',
    200.0,
    CURRENT_DATE - INTERVAL '2 days',
    NOW() - INTERVAL '3 days',
    NOW() - INTERVAL '3 days'
FROM users u
WHERE u.login_id = 'testuser1'
ON CONFLICT (user_id, mining_date) DO NOTHING;

