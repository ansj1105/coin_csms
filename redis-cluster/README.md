# Redis Cluster 설정

이 디렉토리는 로컬 개발 및 테스트를 위한 Redis Cluster 설정을 포함합니다.

## 구조

- `redis-node-1.conf` ~ `redis-node-6.conf`: 각 Redis 노드의 설정 파일
- `node-1/` ~ `node-6/`: 각 노드의 데이터 디렉토리 (자동 생성)

## Redis Cluster 시작

### Windows

```powershell
# 클러스터 시작
scripts\start-redis-cluster.bat

# 데이터 정리 후 시작
scripts\start-redis-cluster.bat --clean
```

### Linux/Mac

```bash
# 실행 권한 부여 (최초 1회)
chmod +x scripts/start-redis-cluster.sh
chmod +x scripts/stop-redis-cluster.sh

# 클러스터 시작
./scripts/start-redis-cluster.sh

# 데이터 정리 후 시작
./scripts/start-redis-cluster.sh --clean
```

## Redis Cluster 중지

### Windows

```powershell
scripts\stop-redis-cluster.bat
```

### Linux/Mac

```bash
./scripts/stop-redis-cluster.sh
```

## 클러스터 구성

- **노드 수**: 6개 (3 Master + 3 Slave)
- **포트**: 7001-7006 (클라이언트 포트), 17001-17006 (버스 포트)
- **복제**: 각 Master 노드당 1개의 Replica

## 연결 정보

클러스터 모드로 사용하려면 `config.json`에서 다음과 같이 설정:

```json
{
  "redis": {
    "mode": "cluster",
    "nodes": [
      "redis://localhost:7001",
      "redis://localhost:7002",
      "redis://localhost:7003",
      "redis://localhost:7004",
      "redis://localhost:7005",
      "redis://localhost:7006"
    ],
    "password": "",
    "maxPoolSize": 8,
    "maxPoolWaiting": 32,
    "poolRecycleTimeout": 15000
  }
}
```

## 클러스터 상태 확인

```bash
# 클러스터 정보 확인
docker exec redis-node-1 redis-cli -p 7001 cluster info

# 노드 목록 확인
docker exec redis-node-1 redis-cli -p 7001 cluster nodes

# 특정 키의 슬롯 확인
docker exec redis-node-1 redis-cli -p 7001 cluster keyslot "your-key"
```

## 주의사항

- 클러스터를 처음 시작하면 초기화에 약 5-10초가 소요됩니다
- 데이터를 영구 보존하려면 `node-*/` 디렉토리가 삭제되지 않도록 주의하세요
- 프로덕션 환경에서는 외부 Redis 클러스터를 사용하는 것을 권장합니다

