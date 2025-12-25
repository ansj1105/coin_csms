@echo off
chcp 65001 >nul
REM Redis Cluster 중지 스크립트 (Windows)
REM 사용법: scripts\stop-redis-cluster.bat

echo Stopping Redis Cluster...

REM Docker Compose 명령 확인 (docker compose 또는 docker-compose)
where docker-compose >nul 2>&1
if errorlevel 1 (
    docker compose version >nul 2>&1
    if errorlevel 1 (
        echo ERROR: docker-compose or docker compose is not available
        exit /b 1
    )
    docker compose -f docker-compose.cluster.yml down
) else (
    docker-compose -f docker-compose.cluster.yml down
)

echo Redis Cluster stopped!

