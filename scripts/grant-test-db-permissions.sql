-- 테스트 DB 권한 부여 스크립트
-- postgres superuser로 실행해야 합니다.
-- 테스트 DB 사용자(foxya)에게 모든 테이블에 대한 접근 권한 부여

-- public 스키마의 모든 테이블, 시퀀스, 함수에 대한 권한 부여
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO foxya;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO foxya;
GRANT ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA public TO foxya;

-- flyway_schema_history 테이블에 대한 권한 부여
GRANT ALL PRIVILEGES ON TABLE flyway_schema_history TO foxya;

-- 향후 생성될 객체에도 자동으로 권한 부여
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT ALL ON TABLES TO foxya;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT ALL ON SEQUENCES TO foxya;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT ALL ON FUNCTIONS TO foxya;

-- users 테이블에 대한 명시적 권한 부여 (방어적 접근)
GRANT ALL PRIVILEGES ON TABLE users TO foxya;

-- 모든 테이블 목록 확인 (권한 부여 확인용)
-- SELECT table_name FROM information_schema.tables WHERE table_schema = 'public';

