# CSMS (Coin Security Management System)

## 아키텍처 개선 사항

### 1. 계층 구조 개선
- **Handler (Controller)**: HTTP 요청 처리 및 응답 생성
- **Service (Business Logic)**: 비즈니스 로직 처리
- **Repository (Persistence)**: 데이터베이스 접근

### 2. 의존성 주입 패턴
- `ServiceFactory` 인터페이스를 통한 의존성 주입
- `DefaultServiceFactory`를 통한 구현
- 각 도메인 모듈에서 ServiceFactory를 통해 의존성 주입

### 3. 패키지 구조 개선
```
com.csms/
├── common/           # 공통 기능
│   ├── dto/          # 공통 DTO
│   ├── enums/        # 열거형
│   ├── exceptions/   # 예외 클래스
│   ├── handler/      # BaseHandler
│   ├── repository/   # BaseRepository
│   ├── service/      # BaseService
│   └── utils/        # 유틸리티
├── config/           # 설정 관리
├── core/             # 핵심 기능
│   └── factory/      # 의존성 주입 팩토리
├── verticle/         # Verticle 클래스
└── [domain]/         # 도메인별 모듈
    ├── handler/
    ├── service/
    ├── repository/
    ├── dto/
    └── entities/
```

### 4. 디자인 패턴
- **Repository Pattern**: 데이터 접근 계층 분리
- **Service Layer Pattern**: 비즈니스 로직 캡슐화
- **Factory Pattern**: 의존성 주입 및 객체 생성
- **Template Method Pattern**: Base 클래스를 통한 공통 로직 재사용

### 5. 개선된 점
1. **모듈화**: 각 도메인을 독립적인 모듈로 구성
2. **의존성 관리**: ServiceFactory를 통한 명시적 의존성 주입
3. **코드 재사용**: Base 클래스를 통한 공통 로직 재사용
4. **테스트 용이성**: 의존성 주입을 통한 테스트 용이성 향상
5. **확장성**: 새로운 도메인 추가 시 일관된 구조 유지

## 로컬 개발 환경 설정

### 사전 요구사항

- **Docker Desktop**: Redis Cluster 실행을 위해 필요
  - 다운로드: https://www.docker.com/products/docker-desktop
  - 설치 가이드: [docs/DOCKER_INSTALLATION_WINDOWS.md](docs/DOCKER_INSTALLATION_WINDOWS.md)
  - 설치 후 Docker Desktop을 실행해야 합니다 (시스템 트레이에 고래 아이콘 확인)

### Redis Cluster 설정

프로젝트는 Redis Cluster 모드를 사용합니다. 로컬 테스트를 위해 Redis Cluster를 시작해야 합니다.

#### Windows

**PowerShell 사용 (권장):**

```powershell
# Redis Cluster 시작
.\scripts\start-redis-cluster.ps1

# 데이터 정리 후 시작
.\scripts\start-redis-cluster.ps1 --clean

# Redis Cluster 중지
.\scripts\stop-redis-cluster.ps1
```

**CMD 또는 배치 파일 사용:**

```cmd
# Redis Cluster 시작
scripts\start-redis-cluster.bat

# 데이터 정리 후 시작
scripts\start-redis-cluster.bat --clean

# Redis Cluster 중지
scripts\stop-redis-cluster.bat
```

#### Linux/Mac

```bash
# 실행 권한 부여 (최초 1회)
chmod +x scripts/start-redis-cluster.sh
chmod +x scripts/stop-redis-cluster.sh

# Redis Cluster 시작
./scripts/start-redis-cluster.sh

# 데이터 정리 후 시작
./scripts/start-redis-cluster.sh --clean

# Redis Cluster 중지
./scripts/stop-redis-cluster.sh
```

#### Docker Compose 직접 사용

```bash
# 클러스터 시작
docker-compose -f docker-compose.cluster.yml up -d

# 클러스터 중지
docker-compose -f docker-compose.cluster.yml down
```

자세한 내용은 [redis-cluster/README.md](redis-cluster/README.md)를 참고하세요.

### 설정 파일

- `src/main/resources/config.json`: 메인 설정 파일
- `src/test/resources/config.json`: 테스트 환경 설정 파일

Redis 클러스터 모드 사용 시 `config.json`의 `redis` 섹션에서 `mode: "cluster"`와 노드 목록을 설정해야 합니다.

**참고**: Redis Cluster가 시작되지 않아도 애플리케이션은 시작되지만, Rate Limiting 기능이 비활성화됩니다.

## 빌드 및 실행

```bash
# 빌드
./gradlew build

# 실행
./gradlew run

# Fat JAR 생성
./gradlew shadowJar
```

## 환경 설정

환경 변수로 설정을 변경할 수 있습니다:
- `APP_ENV` 또는 `ENV`: 환경 (local, prod 등)
- `CONFIG_PATH`: 설정 파일 경로

