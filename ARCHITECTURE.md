# CSMS 아키텍처 설계 문서

## 개요

CSMS (Coin Security Management System)는 fox_coin 프로젝트의 아키텍처를 분석하고 개선한 버전입니다.

## 아키텍처 개선 사항

### 1. 계층 분리 (Layered Architecture)

#### 기존 구조
- Handler, Service, Repository가 각 도메인 패키지에 혼재
- Base 클래스가 common 패키지에 있으나 일관성 부족

#### 개선된 구조
```
com.csms/
├── common/              # 공통 기능
│   ├── handler/        # BaseHandler
│   ├── service/        # BaseService
│   ├── repository/     # BaseRepository
│   ├── dto/           # 공통 DTO
│   ├── enums/         # 열거형
│   ├── exceptions/    # 예외 클래스
│   └── utils/         # 유틸리티
├── config/            # 설정 관리
├── core/              # 핵심 기능
│   └── factory/       # 의존성 주입
├── verticle/          # Verticle 클래스
└── [domain]/          # 도메인별 모듈
    ├── handler/       # HTTP 요청 처리
    ├── service/       # 비즈니스 로직
    ├── repository/    # 데이터 접근
    ├── dto/          # 도메인 DTO
    └── entities/     # 도메인 엔티티
```

### 2. 의존성 주입 패턴

#### 기존 방식
- ApiVerticle에서 모든 Repository, Service, Handler를 직접 생성
- 의존성 관계가 복잡하고 테스트 어려움

#### 개선된 방식
- `ServiceFactory` 인터페이스로 의존성 주입 추상화
- `DefaultServiceFactory`로 구현체 제공
- 각 도메인에서 ServiceFactory를 통해 의존성 주입

```java
// 개선 전
UserRepository userRepository = new UserRepository();
UserService userService = new UserService(pool, userRepository, jwtAuth, ...);

// 개선 후
ServiceFactory factory = DefaultServiceFactory.create(vertx, config);
UserService userService = new UserService(
    factory.getPool(),
    new UserRepository(),
    factory.getJwtAuth(),
    factory.getJwtConfig()
);
```

### 3. Base 클래스 개선

#### BaseRepository
- 메서드명 개선: `getStringColumnValue` → `getString`
- null 체크 로직 개선
- 타입 안전성 향상

#### BaseHandler
- 응답 생성 메서드 개선
- ApiResponse 팩토리 메서드 활용
- 에러 처리 개선

#### BaseService
- PgPool을 protected로 제공
- 공통 비즈니스 로직을 위한 기반 제공

### 4. 디자인 패턴 적용

#### Repository Pattern
- 데이터 접근 로직을 Repository에 캡슐화
- 데이터베이스 변경 시 영향 범위 최소화

#### Service Layer Pattern
- 비즈니스 로직을 Service에 집중
- 트랜잭션 관리 및 비즈니스 규칙 적용

#### Factory Pattern
- 객체 생성 로직을 Factory에 위임
- 의존성 주입을 통한 느슨한 결합

#### Template Method Pattern
- Base 클래스를 통한 공통 로직 재사용
- 각 도메인에서 필요한 부분만 구현

### 5. 패키지 구조 개선

#### 도메인 중심 구조
각 도메인은 독립적인 모듈로 구성:
- `handler/`: HTTP 요청 처리
- `service/`: 비즈니스 로직
- `repository/`: 데이터 접근
- `dto/`: 데이터 전송 객체
- `entities/`: 도메인 엔티티

#### 공통 기능 분리
- `common/`: 모든 도메인에서 사용하는 공통 기능
- `config/`: 설정 관리
- `core/`: 핵심 기능 (Factory 등)

### 6. 코드 품질 개선

#### 예외 처리
- 명확한 예외 계층 구조
- 적절한 예외 메시지

#### 로깅
- 일관된 로깅 패턴
- 적절한 로그 레벨 사용

#### 유틸리티
- JsonUtils: JSON 처리 유틸리티
- DateUtils: 날짜/시간 처리
- AuthUtils: 인증/인가 유틸리티

## 아키텍처 평가

### Controller (Handler) 계층
✅ **적절함**
- HTTP 요청/응답 처리에 집중
- 비즈니스 로직은 Service에 위임
- Validation 처리 포함

### Persistence (Repository) 계층
✅ **적절함**
- 데이터 접근 로직만 포함
- 비즈니스 로직 제외
- BaseRepository를 통한 공통 기능 재사용

### Business (Service) 계층
✅ **적절함**
- 비즈니스 로직 처리
- 트랜잭션 관리
- 도메인 간 협력

## 리팩토링 권장 사항

### 1. 추가 개선 가능 영역
- [ ] 이벤트 시스템 모듈화 (EventPublisher/Subscriber)
- [ ] 쿼리 빌더 유틸리티 추가
- [ ] 캐싱 전략 추가
- [ ] 메트릭 수집 추가

### 2. 확장성 고려
- [ ] 다중 데이터베이스 지원
- [ ] 마이크로서비스 분리 준비
- [ ] API 버전 관리

### 3. 테스트
- [ ] 단위 테스트 추가
- [ ] 통합 테스트 추가
- [ ] 테스트 유틸리티 추가

## 결론

기존 fox_coin 프로젝트의 아키텍처를 분석하고 다음과 같이 개선했습니다:

1. **명확한 계층 분리**: Handler, Service, Repository 계층이 명확히 분리됨
2. **의존성 주입**: Factory 패턴을 통한 의존성 주입으로 테스트 용이성 향상
3. **모듈화**: 도메인별로 독립적인 모듈 구성
4. **코드 재사용**: Base 클래스를 통한 공통 로직 재사용
5. **확장성**: 새로운 도메인 추가 시 일관된 구조 유지

이러한 개선으로 유지보수성, 테스트 용이성, 확장성이 크게 향상되었습니다.

