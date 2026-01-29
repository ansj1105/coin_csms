# Ubuntu 서버 배포 가이드

이 문서는 coin_csms 프로젝트를 Ubuntu 서버에서 실행하기 위한 환경 변수 및 설정 가이드입니다.

## 프론트엔드 API URL 설정

Docker로 백엔드를 실행하는 경우, 프론트엔드에서 API 요청 시 올바른 포트를 사용해야 합니다.

### 개발 환경

#### Docker 사용 시
프론트엔드 프로젝트 루트에 `.env` 파일 생성:
```bash
VITE_API_BASE_URL=http://localhost:8081
VITE_USE_DOCKER=true
```

또는 `.env.docker` 파일 사용:
```bash
cp .env.docker .env
```

#### Docker 없이 실행 시
```bash
VITE_API_BASE_URL=http://localhost:8080
VITE_USE_DOCKER=false
```

### 프로덕션 환경

#### Nginx 프록시 사용 시
```bash
# Nginx가 /api/ 경로를 프록시하므로 빈 문자열 사용
VITE_API_BASE_URL=
```

#### 직접 포트 접근 시
```bash
# 서버 IP 또는 도메인 사용
VITE_API_BASE_URL=http://your-server-ip:8081
# 또는
VITE_API_BASE_URL=https://api.yourdomain.com
```

### 환경 변수 우선순위

1. `VITE_API_BASE_URL` 환경 변수 (최우선)
2. `VITE_USE_DOCKER=true` 설정 시 → `http://localhost:8081`
3. 개발 환경 기본값 → `http://localhost:8080`
4. 프로덕션 웹 → 빈 문자열 (Nginx 프록시)
5. Capacitor 앱 → `https://korion.io.kr`

## 필수 환경 변수

### 1. 애플리케이션 환경 설정

```bash
# 애플리케이션 환경 (local, prod, local-cluster, prod-sentinel)
export APP_ENV=prod

# 또는 ENV 환경 변수 사용 가능
export ENV=prod

# 설정 파일 경로 (선택사항, 기본값: config.json)
export CONFIG_PATH=/app/config.json
```

### 2. 데이터베이스 설정

프로젝트는 `config.json`의 `database` 섹션을 사용하지만, 테스트 환경에서는 다음 환경 변수로 오버라이드 가능:

```bash
# 테스트 데이터베이스 설정 (테스트 실행 시)
export TEST_DB_HOST=localhost
export TEST_DB_PORT=5432
export TEST_DB_DATABASE=coin_system_cloud
export TEST_DB_USER=an
export TEST_DB_PASSWORD=your_password
```

또는 시스템 프로퍼티로 설정:
```bash
-Dtest.db.host=localhost
-Dtest.db.port=5432
-Dtest.db.database=coin_system_cloud
-Dtest.db.user=an
-Dtest.db.password=your_password
```

### 3. 암호화 키 (필수)

```bash
# 암호화 키 설정 (테스트 및 프로덕션)
export ENCRYPTION_KEY=your-secret-encryption-key-here

# 프로덕션에서는 반드시 강력한 키 사용
# 예: openssl rand -base64 32
```

### 4. JVM 옵션

```bash
# JVM 메모리 및 GC 설정
export JAVA_OPTS="-Xmx1024m -Xms512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseStringDeduplication"
```

### 5. 타임존 설정

```bash
# 시스템 타임존 설정
export TZ=Asia/Seoul

# 또는 시스템 레벨에서 설정
sudo timedatectl set-timezone Asia/Seoul
```

## Docker를 사용한 배포

### Docker Compose 사용 시

프로덕션 환경에서는 `docker-compose.prod.yml`을 사용:

```bash
# 1. .env.example을 .env로 복사
cp .env.example .env

# 2. .env 파일 수정 (필요한 값 변경)
# 특히 ENCRYPTION_KEY는 반드시 변경!

# 3. Docker Compose로 실행
docker-compose -f docker-compose.prod.yml --env-file .env up -d

# 또는 환경 변수가 이미 export되어 있으면
docker-compose -f docker-compose.prod.yml up -d
```

**중요**: `.env` 파일은 `.gitignore`에 포함되어 있으므로 Git에 커밋되지 않습니다.

### Docker 네트워크 설정

프로젝트는 `foxya-network`를 사용하여 외부 PostgreSQL 및 Redis에 연결합니다:

```bash
# foxya-network가 존재하는지 확인
docker network ls | grep foxya-network

# 없으면 생성 (foxya 프로젝트와 동일한 네트워크)
docker network create foxya-network
```

## 시스템 서비스로 실행 (systemd)

### 1. systemd 서비스 파일 생성

```bash
sudo nano /etc/systemd/system/csms-api.service
```

```ini
[Unit]
Description=CSMS Coin Service API
After=network.target postgresql.service

[Service]
Type=simple
User=your_user
WorkingDirectory=/path/to/coin_csms
Environment="APP_ENV=prod"
Environment="ENCRYPTION_KEY=your-encryption-key"
Environment="TZ=Asia/Seoul"
Environment="JAVA_OPTS=-Xmx1024m -Xms512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
ExecStart=/usr/bin/java $JAVA_OPTS -jar /path/to/coin_csms/build/libs/coin_csms-fat.jar
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
```

### 2. 서비스 시작

```bash
# 서비스 파일 리로드
sudo systemctl daemon-reload

# 서비스 시작
sudo systemctl start csms-api

# 부팅 시 자동 시작
sudo systemctl enable csms-api

# 상태 확인
sudo systemctl status csms-api

# 로그 확인
sudo journalctl -u csms-api -f
```

## 환경별 설정 요약

### Local 환경
- `APP_ENV=local` 또는 설정 안 함
- `config.json`의 `local` 섹션 사용
- 데이터베이스: `localhost:5432`
- Redis: `localhost:6379`

### Production 환경
- `APP_ENV=prod`
- `config.json`의 `prod` 섹션 사용
- 데이터베이스: `foxya-postgres:5432` (Docker 네트워크)
- Redis: `foxya-redis:6379` (Docker 네트워크)

### Local Cluster 환경
- `APP_ENV=local-cluster`
- Redis Cluster 모드 사용

### Production Sentinel 환경
- `APP_ENV=prod-sentinel`
- Redis Sentinel 모드 사용

## 필수 시스템 패키지

```bash
# Java 17 설치
sudo apt update
sudo apt install openjdk-17-jdk

# 또는 Eclipse Temurin 설치
wget -O - https://packages.adoptium.net/artifactory/api/gpg/key/public | sudo apt-key add -
echo "deb https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print$2}' /etc/os-release) main" | sudo tee /etc/apt/sources.list.d/adoptium.list
sudo apt update
sudo apt install temurin-17-jdk

# PostgreSQL 클라이언트 (선택사항, 마이그레이션 확인용)
sudo apt install postgresql-client

# 타임존 데이터
sudo apt install tzdata
```

## 포트 설정

애플리케이션은 기본적으로 `8080` 포트를 사용하지만, Docker Compose에서는 `8081:8080`으로 매핑됩니다.

```bash
# 포트 확인
sudo netstat -tlnp | grep 8081
# 또는
sudo ss -tlnp | grep 8081
```

## 로그 디렉토리

```bash
# 로그 디렉토리 생성 및 권한 설정
mkdir -p /path/to/coin_csms/logs
chmod 755 /path/to/coin_csms/logs
```

## 보안 고려사항

1. **암호화 키**: 프로덕션에서는 반드시 강력한 암호화 키 사용
2. **JWT Secret**: `config.json`의 `jwt.secret`을 프로덕션용으로 변경
3. **데이터베이스 비밀번호**: 강력한 비밀번호 사용
4. **방화벽**: 필요한 포트만 열기
5. **SSL/TLS**: 프로덕션에서는 HTTPS 사용 권장

## 모니터링

### Health Check

프로덕션 Docker 이미지에는 Health Check가 포함되어 있습니다:

```bash
# Health Check 확인
docker inspect --format='{{.State.Health.Status}}' csms-api
```

### 로그 모니터링

```bash
# 실시간 로그 확인
tail -f /path/to/coin_csms/logs/application.log

# 또는 Docker 로그
docker logs -f csms-api
```

## 문제 해결

### 데이터베이스 연결 실패

```bash
# PostgreSQL 연결 확인
psql -h foxya-postgres -U foxya -d coin_system_cloud

# 네트워크 확인
docker network inspect foxya-network
```

### Redis 연결 실패

```bash
# Redis 연결 확인
redis-cli -h foxya-redis ping

# 네트워크 확인
docker network inspect foxya-network
```

### 메모리 부족

JVM 옵션 조정:
```bash
export JAVA_OPTS="-Xmx2048m -Xms1024m -XX:+UseG1GC"
```

## 환경 변수 체크리스트

배포 전 확인사항:

### 백엔드
- [ ] `APP_ENV` 또는 `ENV` 설정
- [ ] `ENCRYPTION_KEY` 설정 (테스트/프로덕션)
- [ ] `JAVA_OPTS` 설정 (메모리, GC)
- [ ] `TZ` 타임존 설정
- [ ] `config.json`의 데이터베이스 설정 확인
- [ ] `config.json`의 JWT secret 변경 (프로덕션)
- [ ] Docker 네트워크 (`foxya-network`) 연결 확인
- [ ] 포트 충돌 확인 (8081)
- [ ] 로그 디렉토리 권한 확인

### 프론트엔드
- [ ] `.env` 또는 `.env.production` 파일 생성
- [ ] `VITE_API_BASE_URL` 설정
  - Docker 사용 시: `http://localhost:8081`
  - Docker 미사용 시: `http://localhost:8080`
  - 프로덕션 (Nginx): 빈 문자열 또는 `/api`
- [ ] `VITE_USE_DOCKER` 설정 (개발 환경)
- [ ] 빌드 후 API 연결 테스트

**자세한 내용은 프론트엔드 프로젝트의 `ENV_SETUP.md` 파일을 참고하세요.**
