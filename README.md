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

