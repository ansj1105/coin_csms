-- 테스트용 사용자 데이터
-- R__로 시작하는 파일은 Repeatable migration으로, 내용이 변경되면 다시 실행됩니다
-- 비밀번호: Test1234!@
-- 실제 BCrypt 해시 (rounds=10, salt 포함)

-- 기존 테스트 데이터 삭제 (referral_relations도 함께 삭제)
DELETE FROM referral_relations WHERE referrer_id IN (SELECT id FROM users WHERE login_id IN ('testuser', 'testuser2', 'admin_user', 'blocked_user', 'referrer_user', 'no_code_user', 'testuser1', 'testuser3', 'testuser4', 'testuser5', 'admin1'));
DELETE FROM referral_relations WHERE referred_id IN (SELECT id FROM users WHERE login_id IN ('testuser', 'testuser2', 'admin_user', 'blocked_user', 'referrer_user', 'no_code_user', 'testuser1', 'testuser3', 'testuser4', 'testuser5', 'admin1'));
DELETE FROM referral_stats_logs WHERE user_id IN (SELECT id FROM users WHERE login_id IN ('testuser', 'testuser2', 'admin_user', 'blocked_user', 'referrer_user', 'no_code_user', 'testuser1', 'testuser3', 'testuser4', 'testuser5', 'admin1'));
DELETE FROM users WHERE login_id IN ('testuser', 'testuser2', 'admin_user', 'blocked_user', 'referrer_user', 'no_code_user', 'testuser1', 'testuser3', 'testuser4', 'testuser5', 'admin1');

-- 테스트 사용자 추가
-- 아래 해시는 BCrypt.hashpw("Test1234!@", BCrypt.gensalt(10))로 생성됨
-- 비밀번호: Test1234!@
INSERT INTO users (login_id, password_hash, referral_code, status, role, nickname, email, phone, name, created_at, updated_at)
VALUES 
    -- testuser (ID:1): 일반 사용자 (레퍼럴 코드 있음)
    ('testuser', '$2a$10$W84fmGkwKoh6wyU83VwQ8Ovw1wqZYxZ8E6RvWZzpwwZMLGNlSGtwa', 'REF001', 'ACTIVE', 1, '테스트유저', 'testuser@example.com', '010-1111-1111', '테스트유저', NOW(), NOW()),
    -- testuser2 (ID:2): 추가 테스트 사용자 (레퍼럴 코드 없음)
    ('testuser2', '$2a$10$W84fmGkwKoh6wyU83VwQ8Ovw1wqZYxZ8E6RvWZzpwwZMLGNlSGtwa', NULL, 'ACTIVE', 1, '테스트유저2', 'testuser2@example.com', '010-2222-2222', '테스트유저2', NOW(), NOW()),
    -- admin_user (ID:3): 관리자
    ('admin_user', '$2a$10$W84fmGkwKoh6wyU83VwQ8Ovw1wqZYxZ8E6RvWZzpwwZMLGNlSGtwa', 'ADMIN001', 'ACTIVE', 2, '관리자', 'admin@example.com', '010-0000-0000', '관리자', NOW(), NOW()),
    -- blocked_user (ID:4): 차단된 사용자 (레퍼럴 코드 없음 - 생성 테스트용)
    ('blocked_user', '$2a$10$W84fmGkwKoh6wyU83VwQ8Ovw1wqZYxZ8E6RvWZzpwwZMLGNlSGtwa', NULL, 'BLOCKED', 1, '차단유저', 'blocked@example.com', '010-9999-9999', '차단유저', NOW(), NOW()),
    -- referrer_user (ID:5): 추천인 역할 (레퍼럴 코드 있음)
    ('referrer_user', '$2a$10$W84fmGkwKoh6wyU83VwQ8Ovw1wqZYxZ8E6RvWZzpwwZMLGNlSGtwa', 'REFER123', 'ACTIVE', 1, '추천인유저', 'referrer@example.com', '010-5555-5555', '추천인유저', NOW(), NOW()),
    -- no_code_user (ID:6): 레퍼럴 코드 없는 사용자 (통계 조회 테스트용 피추천인)
    ('no_code_user', '$2a$10$W84fmGkwKoh6wyU83VwQ8Ovw1wqZYxZ8E6RvWZzpwwZMLGNlSGtwa', NULL, 'ACTIVE', 1, '코드없는유저', 'nocode@example.com', '010-6666-6666', '코드없는유저', NOW(), NOW()),
    -- testuser1 (ID:7): 추가 테스트 사용자
    ('testuser1', '$2a$10$W84fmGkwKoh6wyU83VwQ8Ovw1wqZYxZ8E6RvWZzpwwZMLGNlSGtwa', 'TEST001', 'ACTIVE', 1, '테스트유저1', 'test1@example.com', '010-1111-1111', '테스트유저1', NOW() - INTERVAL '30 days', NOW()),
    -- testuser3 (ID:8): 추가 테스트 사용자
    ('testuser3', '$2a$10$W84fmGkwKoh6wyU83VwQ8Ovw1wqZYxZ8E6RvWZzpwwZMLGNlSGtwa', 'TEST003', 'ACTIVE', 1, '테스트유저3', 'test3@example.com', '010-3333-3333', '테스트유저3', NOW() - INTERVAL '10 days', NOW()),
    -- testuser4 (ID:9): 추가 테스트 사용자
    ('testuser4', '$2a$10$W84fmGkwKoh6wyU83VwQ8Ovw1wqZYxZ8E6RvWZzpwwZMLGNlSGtwa', 'TEST004', 'ACTIVE', 1, '테스트유저4', 'test4@example.com', '010-4444-4444', '테스트유저4', NOW() - INTERVAL '5 days', NOW()),
    -- testuser5 (ID:10): 비활성 사용자
    ('testuser5', '$2a$10$W84fmGkwKoh6wyU83VwQ8Ovw1wqZYxZ8E6RvWZzpwwZMLGNlSGtwa', 'TEST005', 'INACTIVE', 1, '테스트유저5', 'test5@example.com', '010-5555-5555', '테스트유저5', NOW() - INTERVAL '1 day', NOW()),
    -- admin1 (ID:11): 관리자
    ('admin1', '$2a$10$f7R8Z3w4fHCDjrgZ1bCo8OynXzErkt4c9TQHXxxmTE6BfOAvukDFG', NULL, 'ACTIVE', 2, '관리자1', 'admin1@example.com', '010-0001-0001', '관리자1', NOW(), NOW())
ON CONFLICT (login_id) DO NOTHING;

-- testuser 거래 비밀번호(123456) 설정
UPDATE users
SET transaction_password_hash = '$2y$05$0t.D4AHR2raDheFBwUfFTOPNc3xjZ8QL0tTV89KyIGEakdMdj4vNK'
WHERE login_id = 'testuser';

-- 시퀀스 리셋 (ID 순서 보장)
SELECT setval('users_id_seq', (SELECT COALESCE(MAX(id), 1) FROM users));

-- 테스트용 레퍼럴 관계 데이터 (통계 조회 테스트용)
-- no_code_user(ID:6)가 referrer_user(ID:5)의 피추천인으로 등록
INSERT INTO referral_relations (referrer_id, referred_id, level, status, created_at)
SELECT 
    (SELECT id FROM users WHERE login_id = 'referrer_user'),
    (SELECT id FROM users WHERE login_id = 'no_code_user'),
    1, 'ACTIVE', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM referral_relations 
    WHERE referrer_id = (SELECT id FROM users WHERE login_id = 'referrer_user')
    AND referred_id = (SELECT id FROM users WHERE login_id = 'no_code_user')
);

-- 추가 레퍼럴 관계
INSERT INTO referral_relations (referrer_id, referred_id, level, status, created_at)
SELECT 
    u1.id,
    u2.id,
    1, 'ACTIVE', NOW() - INTERVAL '15 days'
FROM users u1
CROSS JOIN users u2
WHERE u1.login_id = 'testuser1' AND u2.login_id = 'testuser2'
ON CONFLICT (referred_id, level) DO NOTHING;

INSERT INTO referral_relations (referrer_id, referred_id, level, status, created_at)
SELECT 
    u1.id,
    u2.id,
    1, 'ACTIVE', NOW() - INTERVAL '10 days'
FROM users u1
CROSS JOIN users u2
WHERE u1.login_id = 'testuser1' AND u2.login_id = 'testuser3'
ON CONFLICT (referred_id, level) DO NOTHING;

INSERT INTO referral_relations (referrer_id, referred_id, level, status, created_at)
SELECT 
    u1.id,
    u2.id,
    1, 'ACTIVE', NOW() - INTERVAL '8 days'
FROM users u1
CROSS JOIN users u2
WHERE u1.login_id = 'testuser1' AND u2.login_id = 'testuser4'
ON CONFLICT (referred_id, level) DO NOTHING;

INSERT INTO referral_relations (referrer_id, referred_id, level, status, created_at)
SELECT 
    u1.id,
    u2.id,
    1, 'ACTIVE', NOW() - INTERVAL '6 days'
FROM users u1
CROSS JOIN users u2
WHERE u1.login_id = 'testuser1' AND u2.login_id = 'testuser5'
ON CONFLICT (referred_id, level) DO NOTHING;

INSERT INTO referral_relations (referrer_id, referred_id, level, status, created_at)
SELECT 
    u1.id,
    u2.id,
    1, 'ACTIVE', NOW() - INTERVAL '4 days'
FROM users u1
CROSS JOIN users u2
WHERE u1.login_id = 'testuser2' AND u2.login_id = 'testuser4'
ON CONFLICT (referred_id, level) DO NOTHING;
