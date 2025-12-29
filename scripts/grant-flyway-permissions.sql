-- Flyway 권한 부여 스크립트
-- postgres superuser로 실행해야 합니다.

-- flyway_schema_history 테이블에 대한 모든 권한 부여
GRANT ALL PRIVILEGES ON TABLE flyway_schema_history TO foxya;

-- public 스키마의 모든 테이블, 시퀀스, 함수에 대한 권한 부여
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO foxya;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO foxya;
GRANT ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA public TO foxya;

-- 향후 생성될 객체에도 자동으로 권한 부여
-- 주의: ALTER DEFAULT PRIVILEGES는 현재 세션의 역할로 생성된 객체에만 적용됩니다.
-- Flyway는 일반적으로 postgres 사용자로 실행되므로, FOR ROLE postgres를 명시합니다.
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT ALL ON TABLES TO foxya;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT ALL ON SEQUENCES TO foxya;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT ALL ON FUNCTIONS TO foxya;

-- 이미 생성된 모든 테이블에 대한 권한도 다시 한번 명시적으로 부여 (방어적 접근)
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO foxya;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO foxya;
GRANT ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA public TO foxya;

-- 특정 함수에 대한 소유권 변경 (필요한 경우)
-- ALTER FUNCTION update_updated_at_column() OWNER TO foxya;

