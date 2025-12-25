-- 테스트용 더미 데이터 삽입
-- 이 파일은 테스트 환경에서 사용되는 더미 데이터입니다.

-- 테스트용 Admin 사용자 생성 (비밀번호: password123)
-- BCrypt 해시: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
INSERT INTO users (login_id, password_hash, role, status, created_at, updated_at)
VALUES 
    ('admin1', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 2, 'ACTIVE', NOW(), NOW())
ON CONFLICT (login_id) DO NOTHING;

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
INSERT INTO referral_relations (referrer_id, referred_id, status, created_at, updated_at)
SELECT 
    u1.id,
    u2.id,
    'ACTIVE',
    NOW() - INTERVAL '15 days',
    NOW()
FROM users u1
CROSS JOIN users u2
WHERE u1.login_id = 'testuser1' AND u2.login_id = 'testuser2'
ON CONFLICT DO NOTHING;

INSERT INTO referral_relations (referrer_id, referred_id, status, created_at, updated_at)
SELECT 
    u1.id,
    u2.id,
    'ACTIVE',
    NOW() - INTERVAL '10 days',
    NOW()
FROM users u1
CROSS JOIN users u2
WHERE u1.login_id = 'testuser1' AND u2.login_id = 'testuser3'
ON CONFLICT DO NOTHING;

-- 마이닝 히스토리 데이터
INSERT INTO mining_history (user_id, type, amount, created_at, updated_at)
SELECT 
    u.id,
    'BROADCAST_PROGRESS',
    100.5,
    NOW() - INTERVAL '7 days',
    NOW()
FROM users u
WHERE u.login_id = 'testuser1'
ON CONFLICT DO NOTHING;

INSERT INTO mining_history (user_id, type, amount, created_at, updated_at)
SELECT 
    u.id,
    'BROADCAST_WATCH',
    50.25,
    NOW() - INTERVAL '6 days',
    NOW()
FROM users u
WHERE u.login_id = 'testuser2'
ON CONFLICT DO NOTHING;

INSERT INTO mining_history (user_id, type, amount, created_at, updated_at)
SELECT 
    u.id,
    'BROADCAST_PROGRESS',
    200.75,
    NOW() - INTERVAL '5 days',
    NOW()
FROM users u
WHERE u.login_id = 'testuser1'
ON CONFLICT DO NOTHING;

INSERT INTO mining_history (user_id, type, amount, created_at, updated_at)
SELECT 
    u.id,
    'REFERRAL_REWARD',
    10.0,
    NOW() - INTERVAL '4 days',
    NOW()
FROM users u
WHERE u.login_id = 'testuser1'
ON CONFLICT DO NOTHING;

INSERT INTO mining_history (user_id, type, amount, created_at, updated_at)
SELECT 
    u.id,
    'BROADCAST_PROGRESS',
    150.0,
    NOW() - INTERVAL '3 days',
    NOW()
FROM users u
WHERE u.login_id = 'testuser3'
ON CONFLICT DO NOTHING;

INSERT INTO mining_history (user_id, type, amount, created_at, updated_at)
SELECT 
    u.id,
    'BROADCAST_WATCH',
    75.5,
    NOW() - INTERVAL '2 days',
    NOW()
FROM users u
WHERE u.login_id = 'testuser4'
ON CONFLICT DO NOTHING;

INSERT INTO mining_history (user_id, type, amount, created_at, updated_at)
SELECT 
    u.id,
    'BROADCAST_PROGRESS',
    300.0,
    NOW() - INTERVAL '1 day',
    NOW()
FROM users u
WHERE u.login_id = 'testuser1'
ON CONFLICT DO NOTHING;

-- 오늘 날짜 마이닝 데이터
INSERT INTO mining_history (user_id, type, amount, created_at, updated_at)
SELECT 
    u.id,
    'BROADCAST_PROGRESS',
    120.0,
    NOW(),
    NOW()
FROM users u
WHERE u.login_id = 'testuser1'
ON CONFLICT DO NOTHING;

-- 일일 마이닝 데이터
INSERT INTO daily_mining (user_id, mining_date, total_amount, created_at, updated_at)
SELECT 
    u.id,
    CURRENT_DATE,
    120.0,
    NOW(),
    NOW()
FROM users u
WHERE u.login_id = 'testuser1'
ON CONFLICT DO NOTHING;

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
INSERT INTO token_deposits (user_id, amount, currency_code, tx_hash, status, created_at, updated_at)
SELECT 
    u.id,
    1000.0,
    'TRX',
    'test_tx_hash_001',
    'COMPLETED',
    NOW(),
    NOW()
FROM users u
WHERE u.login_id = 'testuser1'
ON CONFLICT DO NOTHING;

-- 결제 입금 데이터 (오늘)
INSERT INTO payment_deposits (user_id, amount, payment_method, status, created_at, updated_at)
SELECT 
    u.id,
    5000.0,
    'CARD',
    'COMPLETED',
    NOW(),
    NOW()
FROM users u
WHERE u.login_id = 'testuser2'
ON CONFLICT DO NOTHING;

-- 교환 데이터 (오늘)
INSERT INTO exchanges (user_id, from_currency_code, to_currency_code, from_amount, to_amount, status, created_at, updated_at)
SELECT 
    u.id,
    'TRX',
    'USDT',
    100.0,
    95.0,
    'COMPLETED',
    NOW(),
    NOW()
FROM users u
WHERE u.login_id = 'testuser3'
ON CONFLICT DO NOTHING;

-- 외부 전송 데이터 (오늘)
INSERT INTO external_transfers (user_id, amount, fee, currency_code, to_address, status, created_at, updated_at)
SELECT 
    u.id,
    200.0,
    5.0,
    'TRX',
    'test_address_001',
    'PENDING',
    NOW(),
    NOW()
FROM users u
WHERE u.login_id = 'testuser4'
ON CONFLICT DO NOTHING;

-- 과거 데이터 (7일 전)
INSERT INTO token_deposits (user_id, amount, currency_code, tx_hash, status, created_at, updated_at)
SELECT 
    u.id,
    2000.0,
    'TRX',
    'test_tx_hash_002',
    'COMPLETED',
    NOW() - INTERVAL '7 days',
    NOW() - INTERVAL '7 days'
FROM users u
WHERE u.login_id = 'testuser1'
ON CONFLICT DO NOTHING;

INSERT INTO payment_deposits (user_id, amount, payment_method, status, created_at, updated_at)
SELECT 
    u.id,
    10000.0,
    'BANK',
    'COMPLETED',
    NOW() - INTERVAL '7 days',
    NOW() - INTERVAL '7 days'
FROM users u
WHERE u.login_id = 'testuser2'
ON CONFLICT DO NOTHING;

INSERT INTO exchanges (user_id, from_currency_code, to_currency_code, from_amount, to_amount, status, created_at, updated_at)
SELECT 
    u.id,
    'USDT',
    'TRX',
    50.0,
    52.5,
    'COMPLETED',
    NOW() - INTERVAL '7 days',
    NOW() - INTERVAL '7 days'
FROM users u
WHERE u.login_id = 'testuser3'
ON CONFLICT DO NOTHING;

INSERT INTO external_transfers (user_id, amount, fee, currency_code, to_address, status, created_at, updated_at)
SELECT 
    u.id,
    500.0,
    10.0,
    'TRX',
    'test_address_002',
    'COMPLETED',
    NOW() - INTERVAL '7 days',
    NOW() - INTERVAL '7 days'
FROM users u
WHERE u.login_id = 'testuser4'
ON CONFLICT DO NOTHING;

-- 스왑 데이터
INSERT INTO swaps (user_id, from_currency_code, to_currency_code, from_amount, to_amount, status, created_at, updated_at)
SELECT 
    u.id,
    'TRX',
    'USDT',
    100.0,
    95.0,
    'COMPLETED',
    NOW() - INTERVAL '5 days',
    NOW() - INTERVAL '5 days'
FROM users u
WHERE u.login_id = 'testuser1'
ON CONFLICT DO NOTHING;

-- Currency 데이터 (지갑 테스트용)
INSERT INTO currency (code, name, chain, decimals, is_active, created_at, updated_at)
VALUES 
    ('KRWT', 'Kori Wallet Token', 'KRC-20', 6, true, NOW(), NOW()),
    ('TRX', 'Tron', 'TRC-20', 6, true, NOW(), NOW()),
    ('USDT', 'Tether', 'TRC-20', 6, true, NOW(), NOW())
ON CONFLICT (code, chain) DO NOTHING;

-- 지갑 데이터 (testuser1용 - testGetMemberWallets 테스트)
INSERT INTO wallets (user_id, currency_id, address, balance, created_at, updated_at)
SELECT 
    u.id,
    c.id,
    'KR-DKWA38JDDJI29FJM1239WIJWR30R03J1239RFJ13202W3JF029FJ02JSKJASFF',
    200000.0,
    NOW(),
    NOW()
FROM users u
CROSS JOIN currency c
WHERE u.login_id = 'testuser1' AND c.code = 'KRWT' AND c.chain = 'KRC-20'
ON CONFLICT DO NOTHING;

INSERT INTO wallets (user_id, currency_id, address, balance, created_at, updated_at)
SELECT 
    u.id,
    c.id,
    'TR-TRX1234567890123456789012345678901234567890',
    50000.0,
    NOW(),
    NOW()
FROM users u
CROSS JOIN currency c
WHERE u.login_id = 'testuser1' AND c.code = 'TRX' AND c.chain = 'TRC-20'
ON CONFLICT DO NOTHING;

INSERT INTO wallets (user_id, currency_id, address, balance, created_at, updated_at)
SELECT 
    u.id,
    c.id,
    'TR-USDT1234567890123456789012345678901234567890',
    10000.0,
    NOW(),
    NOW()
FROM users u
CROSS JOIN currency c
WHERE u.login_id = 'testuser1' AND c.code = 'USDT' AND c.chain = 'TRC-20'
ON CONFLICT DO NOTHING;

-- 더 많은 추천인 관계 (TopReferrers 테스트용)
INSERT INTO referral_relations (referrer_id, referred_id, status, created_at, updated_at)
SELECT 
    u1.id,
    u2.id,
    'ACTIVE',
    NOW() - INTERVAL '8 days',
    NOW()
FROM users u1
CROSS JOIN users u2
WHERE u1.login_id = 'testuser1' AND u2.login_id = 'testuser4'
ON CONFLICT DO NOTHING;

INSERT INTO referral_relations (referrer_id, referred_id, status, created_at, updated_at)
SELECT 
    u1.id,
    u2.id,
    'ACTIVE',
    NOW() - INTERVAL '6 days',
    NOW()
FROM users u1
CROSS JOIN users u2
WHERE u1.login_id = 'testuser1' AND u2.login_id = 'testuser5'
ON CONFLICT DO NOTHING;

INSERT INTO referral_relations (referrer_id, referred_id, status, created_at, updated_at)
SELECT 
    u1.id,
    u2.id,
    'ACTIVE',
    NOW() - INTERVAL '4 days',
    NOW()
FROM users u1
CROSS JOIN users u2
WHERE u1.login_id = 'testuser2' AND u2.login_id = 'testuser4'
ON CONFLICT DO NOTHING;

-- 더 많은 마이닝 히스토리 데이터 (차트 데이터용 - 다양한 날짜)
INSERT INTO mining_history (user_id, type, amount, created_at, updated_at)
SELECT 
    u.id,
    'BROADCAST_PROGRESS',
    80.0,
    NOW() - INTERVAL '14 days',
    NOW() - INTERVAL '14 days'
FROM users u
WHERE u.login_id = 'testuser1'
ON CONFLICT DO NOTHING;

INSERT INTO mining_history (user_id, type, amount, created_at, updated_at)
SELECT 
    u.id,
    'BROADCAST_PROGRESS',
    90.0,
    NOW() - INTERVAL '13 days',
    NOW() - INTERVAL '13 days'
FROM users u
WHERE u.login_id = 'testuser1'
ON CONFLICT DO NOTHING;

INSERT INTO mining_history (user_id, type, amount, created_at, updated_at)
SELECT 
    u.id,
    'BROADCAST_PROGRESS',
    110.0,
    NOW() - INTERVAL '12 days',
    NOW() - INTERVAL '12 days'
FROM users u
WHERE u.login_id = 'testuser1'
ON CONFLICT DO NOTHING;

INSERT INTO mining_history (user_id, type, amount, created_at, updated_at)
SELECT 
    u.id,
    'BROADCAST_PROGRESS',
    130.0,
    NOW() - INTERVAL '11 days',
    NOW() - INTERVAL '11 days'
FROM users u
WHERE u.login_id = 'testuser1'
ON CONFLICT DO NOTHING;

INSERT INTO mining_history (user_id, type, amount, created_at, updated_at)
SELECT 
    u.id,
    'BROADCAST_PROGRESS',
    140.0,
    NOW() - INTERVAL '10 days',
    NOW() - INTERVAL '10 days'
FROM users u
WHERE u.login_id = 'testuser1'
ON CONFLICT DO NOTHING;

INSERT INTO mining_history (user_id, type, amount, created_at, updated_at)
SELECT 
    u.id,
    'BROADCAST_PROGRESS',
    160.0,
    NOW() - INTERVAL '9 days',
    NOW() - INTERVAL '9 days'
FROM users u
WHERE u.login_id = 'testuser1'
ON CONFLICT DO NOTHING;

INSERT INTO mining_history (user_id, type, amount, created_at, updated_at)
SELECT 
    u.id,
    'BROADCAST_PROGRESS',
    170.0,
    NOW() - INTERVAL '8 days',
    NOW() - INTERVAL '8 days'
FROM users u
WHERE u.login_id = 'testuser1'
ON CONFLICT DO NOTHING;

-- 일일 마이닝 데이터 추가 (과거 날짜)
INSERT INTO daily_mining (user_id, mining_date, total_amount, created_at, updated_at)
SELECT 
    u.id,
    CURRENT_DATE - INTERVAL '1 day',
    300.0,
    NOW() - INTERVAL '1 day',
    NOW() - INTERVAL '1 day'
FROM users u
WHERE u.login_id = 'testuser1'
ON CONFLICT DO NOTHING;

INSERT INTO daily_mining (user_id, mining_date, total_amount, created_at, updated_at)
SELECT 
    u.id,
    CURRENT_DATE - INTERVAL '2 days',
    250.0,
    NOW() - INTERVAL '2 days',
    NOW() - INTERVAL '2 days'
FROM users u
WHERE u.login_id = 'testuser1'
ON CONFLICT DO NOTHING;

INSERT INTO daily_mining (user_id, mining_date, total_amount, created_at, updated_at)
SELECT 
    u.id,
    CURRENT_DATE - INTERVAL '3 days',
    200.0,
    NOW() - INTERVAL '3 days',
    NOW() - INTERVAL '3 days'
FROM users u
WHERE u.login_id = 'testuser1'
ON CONFLICT DO NOTHING;

