-- 채굴 관리용 테이블 생성 (Admin 모듈)

-- 채굴 설정 테이블 (BASIC, PROGRESS, LEVEL_LIMIT 타입)
CREATE TABLE mining_settings (
    id SERIAL NOT NULL,
    setting_type VARCHAR(50) NOT NULL,
    setting_key VARCHAR(50) NULL,
    is_enabled BOOLEAN NOT NULL DEFAULT true,
    base_time_enabled BOOLEAN NULL,
    base_time_minutes INT NULL,
    time_per_hour INT NULL,
    coins_per_hour DECIMAL(36, 18) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT PK_mining_settings PRIMARY KEY (id),
    CONSTRAINT UK_mining_settings_type_key UNIQUE (setting_type, setting_key)
);

COMMENT ON TABLE mining_settings IS '채굴 설정 테이블';
COMMENT ON COLUMN mining_settings.id IS 'Sequence ID';
COMMENT ON COLUMN mining_settings.setting_type IS '설정 타입 (BASIC, PROGRESS, LEVEL_LIMIT)';
COMMENT ON COLUMN mining_settings.setting_key IS '설정 키 (PROGRESS 타입일 때: BROADCAST_PROGRESS, BROADCAST_LISTENING)';
COMMENT ON COLUMN mining_settings.is_enabled IS '활성화 여부';
COMMENT ON COLUMN mining_settings.base_time_enabled IS '기본 시간 활성화 여부 (BASIC 타입)';
COMMENT ON COLUMN mining_settings.base_time_minutes IS '기본 시간 (분) (BASIC 타입)';
COMMENT ON COLUMN mining_settings.time_per_hour IS '시간당 분 (PROGRESS 타입)';
COMMENT ON COLUMN mining_settings.coins_per_hour IS '시간당 코인 (PROGRESS 타입)';

-- 채굴 미션 테이블
CREATE TABLE mining_missions (
    id SERIAL NOT NULL,
    type VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    required_count INT NOT NULL DEFAULT 1,
    is_enabled BOOLEAN NOT NULL DEFAULT true,
    has_input BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT PK_mining_missions PRIMARY KEY (id),
    CONSTRAINT UK_mining_missions_type UNIQUE (type)
);

COMMENT ON TABLE mining_missions IS '채굴 미션 테이블';
COMMENT ON COLUMN mining_missions.id IS 'Sequence ID';
COMMENT ON COLUMN mining_missions.type IS '미션 타입 (UNIQUE)';
COMMENT ON COLUMN mining_missions.name IS '미션 이름';
COMMENT ON COLUMN mining_missions.required_count IS '필요 횟수';
COMMENT ON COLUMN mining_missions.is_enabled IS '활성화 여부';
COMMENT ON COLUMN mining_missions.has_input IS '입력 필요 여부';

-- 채굴 레벨 제한 테이블
CREATE TABLE mining_level_limits (
    id SERIAL NOT NULL,
    level INT NOT NULL,
    daily_limit DECIMAL(36, 18) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT PK_mining_level_limits PRIMARY KEY (id),
    CONSTRAINT UK_mining_level_limits_level UNIQUE (level)
);

COMMENT ON TABLE mining_level_limits IS '채굴 레벨별 일일 제한 테이블';
COMMENT ON COLUMN mining_level_limits.id IS 'Sequence ID';
COMMENT ON COLUMN mining_level_limits.level IS '레벨 (1~9, UNIQUE)';
COMMENT ON COLUMN mining_level_limits.daily_limit IS '일일 채굴 제한량';

-- 채굴 부스터 테이블
CREATE TABLE mining_boosters (
    id SERIAL NOT NULL,
    type VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    is_enabled BOOLEAN NOT NULL DEFAULT true,
    efficiency INT NULL,
    max_count INT NULL,
    per_unit_efficiency INT NULL,
    note TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT PK_mining_boosters PRIMARY KEY (id),
    CONSTRAINT UK_mining_boosters_type UNIQUE (type)
);

COMMENT ON TABLE mining_boosters IS '채굴 부스터 테이블';
COMMENT ON COLUMN mining_boosters.id IS 'Sequence ID';
COMMENT ON COLUMN mining_boosters.type IS '부스터 타입 (UNIQUE)';
COMMENT ON COLUMN mining_boosters.name IS '부스터 이름';
COMMENT ON COLUMN mining_boosters.is_enabled IS '활성화 여부';
COMMENT ON COLUMN mining_boosters.efficiency IS '채굴 효율 (%)';
COMMENT ON COLUMN mining_boosters.max_count IS '최대 횟수 (NULL이면 단순 효율)';
COMMENT ON COLUMN mining_boosters.per_unit_efficiency IS '단위당 효율 (max_count와 함께 사용)';
COMMENT ON COLUMN mining_boosters.note IS '설명/노트';

-- 인덱스 생성
CREATE INDEX IDX_mining_settings_type ON mining_settings(setting_type);
CREATE INDEX IDX_mining_settings_enabled ON mining_settings(is_enabled);
CREATE INDEX IDX_mining_missions_enabled ON mining_missions(is_enabled);
CREATE INDEX IDX_mining_level_limits_level ON mining_level_limits(level);
CREATE INDEX IDX_mining_boosters_enabled ON mining_boosters(is_enabled);

-- updated_at 트리거
CREATE TRIGGER update_mining_settings_updated_at BEFORE UPDATE ON mining_settings
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_mining_missions_updated_at BEFORE UPDATE ON mining_missions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_mining_level_limits_updated_at BEFORE UPDATE ON mining_level_limits
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_mining_boosters_updated_at BEFORE UPDATE ON mining_boosters
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

