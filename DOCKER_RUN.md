# Docker 실행 가이드

이 문서는 coin_csms 프로젝트를 Docker로 실행하는 방법을 설명합니다.

## 사전 준비

### 1. 필수 요구사항
- Docker 및 Docker Compose 설치
- `foxya-network` Docker 네트워크 존재 (foxya 프로젝트와 동일한 DB 사용)
- 프로젝트 빌드 완료 (`./gradlew shadowJar`)

### 2. foxya-network 확인

```bash
# foxya-network가 존재하는지 확인 (프로젝트명에 따라 이름이 다를 수 있음)
docker network ls | grep foxya

# 일반적인 네트워크 이름:
# - fox_coin_foxya-network (fox_coin 프로젝트명 사용 시)
# - foxya-network (직접 생성한 경우)
```

### 3. 프로젝트 빌드

```bash
# Fat JAR 빌드
./gradlew shadowJar
```

빌드된 JAR 파일은 `build/libs/*-fat.jar`에 생성됩니다.

---

## 개발 환경 실행

### 기본 실행

```bash
# Docker Compose로 실행
docker-compose up -d

# 로그 확인
docker-compose logs -f app

# 중지
docker-compose down
```

### 포트
- **애플리케이션**: `http://localhost:8081` (내부 포트 8080)

### 볼륨 마운트
- `config.json`: `./src/main/resources/config.json` → `/app/config.json`
- `logs`: `./logs` → `/app/logs`

---

## 프로덕션 환경 실행

### 1. 환경 변수 파일 준비

```bash
# .env.example을 .env로 복사
cp .env.example .env

# .env 파일 수정 (필요한 값 설정)
# 특히 ENCRYPTION_KEY는 반드시 변경!
```

### 2. Docker Compose로 실행

```bash
# 프로덕션 빌드 및 실행
docker-compose -f docker-compose.prod.yml --env-file .env up -d

# 또는 환경 변수가 이미 export되어 있으면
docker-compose -f docker-compose.prod.yml up -d
```

### 3. 로그 확인

```bash
# 실시간 로그 확인
docker-compose -f docker-compose.prod.yml logs -f app

# 최근 로그만 확인
docker-compose -f docker-compose.prod.yml logs --tail=100 app
```

### 4. 컨테이너 관리

```bash
# 컨테이너 상태 확인
docker-compose -f docker-compose.prod.yml ps

# 컨테이너 중지
docker-compose -f docker-compose.prod.yml stop

# 컨테이너 중지 및 제거
docker-compose -f docker-compose.prod.yml down

# 컨테이너 재시작
docker-compose -f docker-compose.prod.yml restart app

# 이미지 재빌드 및 실행
docker-compose -f docker-compose.prod.yml up -d --build
```

---

## 주요 명령어

### 컨테이너 상태 확인

```bash
# 실행 중인 컨테이너 확인
docker ps | grep csms-api

# 컨테이너 상세 정보
docker inspect csms-api

# 컨테이너 리소스 사용량
docker stats csms-api
```

### 로그 관리

```bash
# 실시간 로그
docker logs -f csms-api

# 최근 100줄 로그
docker logs --tail=100 csms-api

# 특정 시간 이후 로그
docker logs --since 10m csms-api
```

### 컨테이너 내부 접속

```bash
# 컨테이너 내부 쉘 접속
docker exec -it csms-api sh

# 컨테이너 내부 파일 확인
docker exec csms-api ls -la /app
```

### Health Check

```bash
# Health Check 확인
curl http://localhost:8081/health

# 또는
docker exec csms-api wget -qO- http://localhost:8080/health
```

---

## 환경 변수 설정

### .env 파일 예시

```bash
# 애플리케이션 환경
APP_ENV=prod

# 암호화 키 (필수)
ENCRYPTION_KEY=your-secret-encryption-key-change-this-in-production

# JVM 옵션
JAVA_OPTS=-Xmx1024m -Xms512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200

# 타임존
TZ=Asia/Seoul

# Docker 이미지 버전
APP_VERSION=latest

# 선택사항: DB 설정 오버라이드
# DB_HOST=foxya-postgres
# DB_USER=foxya
# DB_PASSWORD=your-password
# JWT_SECRET=your-jwt-secret
```

### 환경 변수 우선순위

1. `.env` 파일 (docker-compose에서 사용)
2. 시스템 환경 변수
3. `config.json` 파일 (기본값)

---

## 문제 해결

### 1. 포트 충돌

```bash
# 포트 8081이 이미 사용 중인 경우
# docker-compose.yml에서 포트 변경
ports:
  - "8082:8080"  # 8081 → 8082로 변경
```

### 2. foxya-network 없음

```bash
# foxya 프로젝트가 실행 중이면 네트워크가 자동으로 생성됨
# 네트워크 이름 확인
docker network ls | grep foxya

# 네트워크 이름이 다르면 docker-compose.yml에서 수정 필요
# 예: fox_coin_foxya-network, foxya-network 등
```

### 3. 빌드 실패

```bash
# 캐시 없이 재빌드
docker-compose build --no-cache

# 또는
docker-compose -f docker-compose.prod.yml build --no-cache
```

### 4. 컨테이너가 계속 재시작됨

```bash
# 로그 확인하여 원인 파악
docker logs csms-api

# 컨테이너 상태 확인
docker ps -a | grep csms-api
```

### 5. config.json 파일 없음

```bash
# config.json 파일이 있는지 확인
ls -la src/main/resources/config.json

# 없으면 생성하거나 복사
```

---

## 개발 vs 프로덕션 차이점

| 항목 | 개발 환경 | 프로덕션 환경 |
|------|----------|--------------|
| Dockerfile | `Dockerfile` | `Dockerfile.prod` |
| Compose 파일 | `docker-compose.yml` | `docker-compose.prod.yml` |
| 빌드 방식 | 단일 스테이지 | 멀티 스테이지 (최적화) |
| 볼륨 마운트 | config.json 마운트 | 마운트 없음 (이미지에 포함) |
| 로그 관리 | 기본 | 로그 로테이션 설정 |
| 리소스 제한 | 없음 | 메모리 2G 제한 |
| Health Check | 없음 | 5분마다 체크 |

---

## 빠른 시작 (개발 환경)

```bash
# 1. 빌드
./gradlew shadowJar

# 2. foxya-network 확인 (이미 foxya 프로젝트가 실행 중이면 자동으로 존재)
docker network ls | grep foxya

# 3. 실행
docker-compose up -d

# 4. 로그 확인
docker-compose logs -f app

# 5. Health Check
curl http://localhost:8081/health
```

---

## 빠른 시작 (프로덕션 환경)

```bash
# 1. 빌드
./gradlew shadowJar

# 2. .env 파일 준비
cp .env.example .env
# .env 파일 수정 (ENCRYPTION_KEY 등)

# 3. foxya-network 확인 (이미 foxya 프로젝트가 실행 중이면 자동으로 존재)
docker network ls | grep foxya

# 4. 실행
docker-compose -f docker-compose.prod.yml --env-file .env up -d

# 5. 로그 확인
docker-compose -f docker-compose.prod.yml logs -f app
```

---

## 참고사항

- **포트**: 개발/프로덕션 모두 `8081:8080`으로 매핑 (foxya와 충돌 방지)
- **네트워크**: `fox_coin_foxya-network`를 통해 foxya 프로젝트의 PostgreSQL과 Redis에 연결 (프로젝트명에 따라 다를 수 있음)
- **로그**: `./logs` 디렉토리에 저장됨
- **설정**: `config.json`은 환경 변수로 오버라이드 가능 (ConfigLoader 참고)
