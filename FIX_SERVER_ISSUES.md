# 서버 배포 시 문제 해결 가이드

**⚠️ 중요**: Nginx가 안 열리는 경우 `SERVER_RECOVERY.md`를 참고하세요.

## 문제 1: 로그 파일 권한 오류

### 증상
```
ERROR StatusConsoleListener Unable to create file logs/csms.log
java.io.IOException: Permission denied
```

### 해결 방법

서버에서 다음 명령어를 실행하세요:

```bash
# coin_csms 디렉토리로 이동
cd /var/www/coin_csms

# logs 디렉토리 생성 및 권한 설정
sudo mkdir -p logs
sudo chown -R 1001:1001 logs  # Dockerfile에서 appuser의 UID/GID가 1001
sudo chmod 755 logs

# 또는 현재 사용자로 권한 설정 (Docker가 호스트 사용자로 실행되는 경우)
sudo chown -R $USER:$USER logs
sudo chmod 755 logs
```

또는 docker-compose.prod.yml에서 볼륨 마운트를 수정:

```yaml
volumes:
  - ./logs:/app/logs:rw  # rw 권한 명시
```

그리고 컨테이너 재시작:
```bash
docker compose -f docker-compose.prod.yml down
docker compose -f docker-compose.prod.yml up -d
```

## 문제 2: Redis 연결 실패

### 증상
```
java.net.UnknownHostException: Failed to resolve 'foxya-redis' [A(1)] after 2 queries
```

### 해결 방법

#### 1. 네트워크 확인

```bash
# fox_coin_foxya-network 네트워크 확인
docker network inspect fox_coin_foxya-network

# foxya-redis 컨테이너가 네트워크에 연결되어 있는지 확인
docker network inspect fox_coin_foxya-network | grep foxya-redis
```

#### 2. 네트워크에 컨테이너 연결 확인

```bash
# fox_coin 프로젝트의 Redis 컨테이너 확인
cd /var/www/fox_coin  # 또는 실제 fox_coin 프로젝트 경로
docker compose -f docker-compose.prod.yml ps redis

# Redis 컨테이너가 실행 중인지 확인
docker ps | grep foxya-redis
```

#### 3. 네트워크 재생성 (필요한 경우)

```bash
# 1. fox_coin 먼저 실행하여 네트워크 생성
cd /var/www/fox_coin
docker compose -f docker-compose.prod.yml up -d

# 2. 네트워크 확인
docker network ls | grep fox_coin_foxya-network

# 3. coin_csms 실행
cd /var/www/coin_csms
docker compose -f docker-compose.prod.yml up -d

# 4. 네트워크 연결 확인
docker network inspect fox_coin_foxya-network | grep -A 10 "Containers"
```

#### 4. 컨테이너 재시작

```bash
# coin_csms 재시작
cd /var/www/coin_csms
docker compose -f docker-compose.prod.yml restart

# 또는 완전히 재시작
docker compose -f docker-compose.prod.yml down
docker compose -f docker-compose.prod.yml up -d
```

## 문제 3: 네트워크 이름 불일치

### 확인 사항

fox_coin의 네트워크 이름이 `fox_coin_foxya-network`인지 확인:

```bash
cd /var/www/fox_coin
cat docker-compose.prod.yml | grep -A 2 "networks:"
```

만약 `foxya-network`로 되어 있다면, `fox_coin_foxya-network`로 변경해야 합니다.

## 전체 배포 순서

```bash
# 1. fox_coin 먼저 실행 (네트워크 생성)
cd /var/www/fox_coin
docker compose -f docker-compose.prod.yml up -d

# 2. 네트워크 확인
docker network ls | grep fox_coin_foxya-network

# 3. coin_csms 로그 디렉토리 권한 설정
cd /var/www/coin_csms
sudo mkdir -p logs
sudo chown -R 1001:1001 logs
sudo chmod 755 logs

# 4. coin_csms 실행
docker compose -f docker-compose.prod.yml up -d

# 5. 로그 확인
docker logs -f csms-api
```

## 디버깅 명령어

```bash
# 네트워크 내 컨테이너 확인
docker network inspect fox_coin_foxya-network

# coin_csms 컨테이너에서 Redis 연결 테스트
docker exec csms-api ping -c 3 foxya-redis

# Redis 컨테이너 확인
docker ps | grep redis

# coin_csms 컨테이너 로그 확인
docker logs csms-api

# 네트워크 연결 상태 확인
docker inspect csms-api | grep -A 20 "Networks"
```

