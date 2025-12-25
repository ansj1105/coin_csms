@echo off
chcp 65001 >nul
REM Redis Cluster 시작 스크립트 (Windows)
REM 사용법: scripts\start-redis-cluster.bat

echo Starting Redis Cluster...

REM Docker 설치 확인
docker --version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Docker is not installed or not in PATH
    echo Please install Docker Desktop from https://www.docker.com/products/docker-desktop
    exit /b 1
)

REM Docker Compose 명령 확인 (docker compose 또는 docker-compose)
where docker-compose >nul 2>&1
if errorlevel 1 (
    docker compose version >nul 2>&1
    if errorlevel 1 (
        echo ERROR: docker-compose or docker compose is not available
        exit /b 1
    )
    set DOCKER_COMPOSE_CMD=docker compose
) else (
    set DOCKER_COMPOSE_CMD=docker-compose
)

REM 기존 클러스터 데이터 정리 (선택적)
if "%1"=="--clean" (
    echo Cleaning up existing cluster data...
    if exist redis-cluster\node-1 rmdir /s /q redis-cluster\node-1
    if exist redis-cluster\node-2 rmdir /s /q redis-cluster\node-2
    if exist redis-cluster\node-3 rmdir /s /q redis-cluster\node-3
    if exist redis-cluster\node-4 rmdir /s /q redis-cluster\node-4
    if exist redis-cluster\node-5 rmdir /s /q redis-cluster\node-5
    if exist redis-cluster\node-6 rmdir /s /q redis-cluster\node-6
    mkdir redis-cluster\node-1 2>nul
    mkdir redis-cluster\node-2 2>nul
    mkdir redis-cluster\node-3 2>nul
    mkdir redis-cluster\node-4 2>nul
    mkdir redis-cluster\node-5 2>nul
    mkdir redis-cluster\node-6 2>nul
)

REM Docker Compose로 클러스터 시작
echo Starting Redis Cluster containers...
%DOCKER_COMPOSE_CMD% -f docker-compose.cluster.yml up -d

if errorlevel 1 (
    echo ERROR: Failed to start Redis Cluster
    exit /b 1
)

REM 클러스터 초기화 대기
echo Waiting for cluster initialization...
timeout /t 10 /nobreak >nul

REM 클러스터 상태 확인
echo Checking cluster status...
docker exec redis-node-1 redis-cli -p 7001 cluster info >nul 2>&1
if errorlevel 1 (
    echo WARNING: Cluster may still be initializing, please wait a few more seconds
) else (
    docker exec redis-node-1 redis-cli -p 7001 cluster info | findstr /C:"cluster_state"
    echo.
    echo Cluster nodes:
    docker exec redis-node-1 redis-cli -p 7001 cluster nodes
)

echo.
echo Redis Cluster is ready!
echo.
echo Connection info:
echo    - Node 1: localhost:7001
echo    - Node 2: localhost:7002
echo    - Node 3: localhost:7003
echo    - Node 4: localhost:7004
echo    - Node 5: localhost:7005
echo    - Node 6: localhost:7006
echo.
echo To use cluster mode, set 'mode': 'cluster' in config.json

