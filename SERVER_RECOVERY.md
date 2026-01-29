# 서버 복구 가이드 (Ubuntu Docker 환경)

## 현재 문제 상황
1. coin_csms 로그 파일 권한 오류
2. Redis 연결 실패 (foxya-redis를 찾을 수 없음)
3. Nginx 웹 서비스 중단

## 전체 복구 절차

### 1단계: 현재 상태 확인

```bash
# 모든 컨테이너 상태 확인
docker ps -a

# 네트워크 확인
docker network ls

# fox_coin_foxya-network 네트워크 상세 확인
docker network inspect fox_coin_foxya-network

# Nginx 컨테이너 상태 확인
docker ps | grep nginx
docker logs foxya-nginx --tail 50
```

### 2단계: fox_coin 서비스 복구 (먼저 실행)

```bash
# fox_coin 프로젝트로 이동
cd /var/www/fox_coin  # 또는 실제 경로

# 컨테이너 상태 확인
docker compose -f docker-compose.prod.yml ps

# 네트워크 확인 (fox_coin_foxya-network가 생성되어 있는지)
docker network ls | grep fox_coin_foxya-network

# 네트워크가 없으면 생성 (docker-compose.prod.yml에서 자동 생성됨)
docker compose -f docker-compose.prod.yml up -d

# 모든 서비스가 정상 실행되는지 확인
docker compose -f docker-compose.prod.yml ps

# Redis 컨테이너 확인
docker ps | grep foxya-redis

# 네트워크에 Redis가 연결되어 있는지 확인
docker network inspect fox_coin_foxya-network | grep -A 5 "foxya-redis"
```

### 3단계: coin_csms 로그 디렉토리 권한 수정

```bash
# coin_csms 프로젝트로 이동
cd /var/www/coin_csms  # 또는 실제 경로

# logs 디렉토리 생성 및 권한 설정
sudo mkdir -p logs
sudo chown -R 1001:1001 logs  # Dockerfile의 appuser UID/GID
sudo chmod 755 logs

# 또는 현재 사용자로 설정 (Docker가 호스트 사용자로 실행되는 경우)
sudo chown -R $USER:$USER logs
sudo chmod 755 logs
```

### 4단계: coin_csms 재시작

```bash
# 기존 컨테이너 중지 및 제거
docker compose -f docker-compose.prod.yml down

# 네트워크 연결 확인
docker network inspect fox_coin_foxya-network | grep -A 10 "Containers"

# 컨테이너 재시작
docker compose -f docker-compose.prod.yml up -d

# 로그 확인
docker logs -f csms-api
```

### 5단계: Nginx 복구

```bash
# fox_coin 프로젝트로 이동
cd /var/www/fox_coin

# Nginx 컨테이너 상태 확인
docker compose -f docker-compose.prod.yml ps nginx

# Nginx 로그 확인
docker compose -f docker-compose.prod.yml logs nginx --tail 100

# Nginx 설정 테스트
docker compose -f docker-compose.prod.yml exec nginx nginx -t

# Nginx 재시작
docker compose -f docker-compose.prod.yml restart nginx

# 또는 완전히 재시작
docker compose -f docker-compose.prod.yml stop nginx
docker compose -f docker-compose.prod.yml up -d nginx
```

### 6단계: 전체 서비스 상태 확인

```bash
# 모든 컨테이너 상태
docker ps

# 네트워크 연결 확인
docker network inspect fox_coin_foxya-network

# 포트 확인
sudo netstat -tlnp | grep -E '80|443|8080|8081'

# 서비스 헬스 체크
curl http://localhost:8080/health  # fox_coin
curl http://localhost:8081/health  # coin_csms
curl http://localhost/nginx-health  # Nginx
```

## 문제별 해결 방법

### 문제 1: 네트워크가 없는 경우

```bash
# fox_coin 먼저 실행하여 네트워크 생성
cd /var/www/fox_coin
docker compose -f docker-compose.prod.yml up -d

# 네트워크 확인
docker network ls | grep fox_coin_foxya-network

# 네트워크가 없으면 수동 생성
docker network create fox_coin_foxya-network
```

### 문제 2: Redis 연결 실패

```bash
# Redis 컨테이너가 실행 중인지 확인
docker ps | grep redis

# Redis가 없으면 fox_coin 재시작
cd /var/www/fox_coin
docker compose -f docker-compose.prod.yml up -d redis

# 네트워크에 Redis 연결 확인
docker network inspect fox_coin_foxya-network | grep -A 5 "foxya-redis"

# coin_csms에서 Redis 연결 테스트
docker exec csms-api ping -c 3 foxya-redis
```

### 문제 3: Nginx가 시작되지 않는 경우

```bash
cd /var/www/fox_coin

# Nginx 설정 파일 확인
docker compose -f docker-compose.prod.yml exec nginx nginx -t

# Nginx 로그 확인
docker compose -f docker-compose.prod.yml logs nginx

# 볼륨 마운트 확인
docker compose -f docker-compose.prod.yml config | grep -A 5 "nginx:"

# Nginx 컨테이너 재생성
docker compose -f docker-compose.prod.yml stop nginx
docker compose -f docker-compose.prod.yml rm -f nginx
docker compose -f docker-compose.prod.yml up -d nginx
```

### 문제 4: 포트 충돌

```bash
# 포트 사용 확인
sudo lsof -i :80
sudo lsof -i :443
sudo lsof -i :8080
sudo lsof -i :8081

# 포트를 사용하는 프로세스 종료 (필요한 경우)
sudo kill -9 <PID>
```

## 전체 재시작 (최후의 수단)

```bash
# 1. 모든 서비스 중지
cd /var/www/fox_coin
docker compose -f docker-compose.prod.yml down

cd /var/www/coin_csms
docker compose -f docker-compose.prod.yml down

# 2. 네트워크 확인 및 재생성 (필요한 경우)
docker network rm fox_coin_foxya-network  # 기존 네트워크 제거
docker network create fox_coin_foxya-network  # 새로 생성

# 3. fox_coin 먼저 시작
cd /var/www/fox_coin
docker compose -f docker-compose.prod.yml up -d

# 4. coin_csms 로그 디렉토리 권한 설정
cd /var/www/coin_csms
sudo mkdir -p logs
sudo chown -R 1001:1001 logs
sudo chmod 755 logs

# 5. coin_csms 시작
docker compose -f docker-compose.prod.yml up -d

# 6. 모든 서비스 상태 확인
docker ps
docker network inspect fox_coin_foxya-network
```

## 빠른 복구 스크립트

```bash
#!/bin/bash
# quick_recovery.sh

echo "=== 서버 복구 시작 ==="

# 1. fox_coin 복구
echo "1. fox_coin 서비스 복구 중..."
cd /var/www/fox_coin
docker compose -f docker-compose.prod.yml up -d
sleep 5

# 2. 네트워크 확인
echo "2. 네트워크 확인 중..."
docker network inspect fox_coin_foxya-network > /dev/null 2>&1
if [ $? -ne 0 ]; then
    echo "네트워크가 없습니다. fox_coin을 먼저 실행하세요."
    exit 1
fi

# 3. coin_csms 로그 디렉토리 권한 설정
echo "3. coin_csms 로그 디렉토리 권한 설정 중..."
cd /var/www/coin_csms
sudo mkdir -p logs
sudo chown -R 1001:1001 logs
sudo chmod 755 logs

# 4. coin_csms 재시작
echo "4. coin_csms 재시작 중..."
docker compose -f docker-compose.prod.yml down
docker compose -f docker-compose.prod.yml up -d

# 5. Nginx 재시작
echo "5. Nginx 재시작 중..."
cd /var/www/fox_coin
docker compose -f docker-compose.prod.yml restart nginx

# 6. 상태 확인
echo "=== 복구 완료 ==="
echo "컨테이너 상태:"
docker ps

echo ""
echo "네트워크 상태:"
docker network inspect fox_coin_foxya-network | grep -A 5 "Containers"
```

## 로그 확인 명령어

```bash
# coin_csms 로그
docker logs -f csms-api

# fox_coin 로그
docker logs -f foxya-api

# Nginx 로그
docker logs -f foxya-nginx

# Redis 로그
docker logs -f foxya-redis

# 모든 로그 동시 확인
docker compose -f docker-compose.prod.yml logs -f
```

