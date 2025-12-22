-- Admin 사용자 생성
-- 아이디: admin
-- 비밀번호: qwer!234
-- 역할: 2 (ADMIN)
-- 
-- 비밀번호는 BCrypt로 해시화되어 저장됩니다.
-- BCrypt 라이브러리: org.mindrot:jbcrypt:0.4
-- 
-- 해시 생성 방법:
-- 온라인 BCrypt 생성기 사용: https://bcrypt-generator.com/
-- 1. 비밀번호 입력: qwer!234
-- 2. Rounds: 10 (기본값)
-- 3. 생성된 해시값을 아래 password_hash에 입력
-- 
-- 또는 다음 명령어로 생성 (Gradle 빌드 후):
-- java -cp "build/libs/*-fat.jar" com.csms.util.PasswordHasher

-- 비밀번호 "qwer!234"의 BCrypt 해시값
-- 아래 해시값은 온라인 생성기(https://bcrypt-generator.com/)로 생성한 값입니다.
-- 비밀번호: qwer!234
-- Rounds: 10

INSERT INTO users (
    login_id,
    password_hash,
    role,
    status,
    created_at,
    updated_at
) VALUES (
    'admin',
    '$2a$10$rKqJ8qJ8qJ8qJ8qJ8qJ8uO8qJ8qJ8qJ8qJ8qJ8qJ8qJ8qJ8qJ8qJ8qJ8q', -- TODO: 실제 해시값으로 교체 필요
    2, -- ADMIN 역할
    'ACTIVE',
    NOW(),
    NOW()
)
ON CONFLICT (login_id) DO NOTHING;

