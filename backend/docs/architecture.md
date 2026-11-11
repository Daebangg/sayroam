# 백엔드 아키텍처 규약

`backend/`의 모든 도메인 패키지에 적용하는 구조 규칙입니다. 새 코드는 이 규약을 따르고, 기존 코드는 리팩터링할 때 이 규약에 맞춥니다.

## 목적

헥사고날 아키텍처의 **의존성 방향 원칙만 지키고, 포트는 효과가 큰 곳에만 둡니다.** 유스케이스마다 in/out 포트를 모두 만드는 방식은 인터페이스와 매핑 코드가 늘어 구조 파악과 변경 비용이 커지므로 쓰지 않습니다.

> 비즈니스 로직을 기술과 전달 방식(웹, DB, 외부 API)에서 분리해, 기술이 바뀌어도 비즈니스 로직은 그대로 두고 기술 없이도 테스트할 수 있게 합니다.

의존성은 항상 비즈니스 로직(domain) 쪽으로만 향합니다. 서비스가 JPA 같은 기술을 직접 참조하지 않도록 인터페이스로 방향을 뒤집어(DIP) 기술이 비즈니스 로직에 의존하게 합니다.

이 규약이 지키려는 것은 세 가지입니다.

- 비즈니스 로직(domain, application)이 외부 기술(외부 API, MQ, 캐시 등)에 직접 의존하지 않습니다.
- JPA의 장점(변경 감지, 지연 로딩, 영속성 컨텍스트)은 그대로 활용합니다.
- 패키지 구조만 보고 코드 위치를 예측할 수 있습니다.

## 핵심 원칙

| 대상 | 규칙 |
| --- | --- |
| 의존성 방향 | 항상 안쪽(domain)을 향합니다. domain은 다른 계층을 모릅니다 |
| 도메인 규칙 | 규칙을 가진 객체에게 맡깁니다. 값을 꺼내 바깥에서 판단하지 않습니다 |
| JPA 엔티티 | 도메인 클래스에 `@Entity`를 직접 붙입니다. 도메인과 엔티티를 분리하지 않습니다 |
| DB 리포지토리 | domain에 스프링 import 없는 순수 인터페이스를 두고, infrastructure의 Spring Data 인터페이스가 다중 상속으로 구현합니다 |
| 외부 연동(결제, 외부 API, MQ, Redis, S3 등) | application에 out 포트를 두고 infrastructure에서 어댑터로 구현합니다 |
| in 포트(UseCase 인터페이스) | 만들지 않습니다. 컨트롤러가 서비스를 직접 호출합니다 |
| 복잡한 조회 | application에 조회 전용 인터페이스를 두고 infrastructure에서 구현합니다 |
| 트랜잭션 | `@Transactional`은 application 서비스 메서드에만 붙입니다 |
| Swagger | 애너테이션은 `XxxApi` 인터페이스에만 둡니다. 컨트롤러에는 두지 않습니다 |

## 패키지 구조

도메인(기능) 단위로 먼저 나누고, 그 안을 4개 계층으로 나눕니다.

```text
presentation ──▶ application ──▶ domain
                      ▲             ▲
                      │ out 포트 구현 │ Repository 구현
                      └── infrastructure ──┘
```

domain은 어떤 계층도 참조하지 않습니다. infrastructure는 application의 out 포트와 domain의 리포지토리 인터페이스를 구현하는 방식으로만 안쪽에 의존합니다.

```text
com.daebangg.sayroam.order
├── presentation
│   ├── OrderApi                 Swagger 애너테이션을 가진 인터페이스
│   ├── OrderController          implements OrderApi
│   └── dto/                     OrderCreateRequest, OrderResponse
├── application
│   ├── OrderService             @Transactional
│   ├── OrderQueryRepository     조회 전용 인터페이스
│   ├── dto/                     OrderCommand, OrderResult
│   └── port/
│       ├── PaymentPort
│       └── OrderCachePort
├── domain
│   ├── Order                    @Entity + 비즈니스 로직
│   ├── OrderStatus
│   └── OrderRepository          순수 인터페이스
└── infrastructure
    ├── persistence/
    │   ├── OrderJpaRepository         extends JpaRepository, OrderRepository
    │   └── OrderQueryRepositoryImpl   조회 전용 구현
    ├── payment/
    │   └── TossPaymentAdapter         implements PaymentPort
    └── cache/
        └── RedisOrderCacheAdapter     implements OrderCachePort
```

- 계층 안의 하위 패키지는 위 구조를 기준으로 합니다. `service/`, `controller/`, `entity/`, `repository/`처럼 클래스 종류별로 한 번 더 나누지 않습니다.
- infrastructure는 기술별(`persistence/`, `redis/`, `smtp/` 등)로 나눕니다.
- 여러 도메인이 함께 쓰는 값 객체는 `common/domain`, 공통 설정·예외·보안은 `global`에 둡니다.

## 계층별 규칙

| 계층 | 둘 수 있는 것 | 두면 안 되는 것 | 의존 가능 대상 |
| --- | --- | --- | --- |
| presentation | Controller, `XxxApi` 인터페이스, 요청/응답 DTO, 입력 검증(`@Valid`), 예외 핸들러 | 비즈니스 로직, Repository 직접 호출, 엔티티를 응답으로 그대로 반환 | application |
| application | 유스케이스 서비스, `@Transactional`, out 포트 인터페이스, 서비스 입출력 DTO(Command/Result) | JPA/MyBatis/QueryDSL 코드, `RedisTemplate`·`RestClient` 등 기술 구현 클래스, Spring Data 타입, presentation의 요청 DTO | domain |
| domain | `@Entity` 도메인 모델과 비즈니스 로직, 값 객체, 도메인 예외·에러 코드, 리포지토리 순수 인터페이스 | Spring 빈 애너테이션(`@Service`, `@Component`), `org.springframework` import, 다른 계층 참조 | 없음(JPA 애너테이션 `jakarta.persistence`만 허용) |
| infrastructure | Spring Data JPA 인터페이스, QueryDSL, MyBatis Mapper, 외부 연동 어댑터, 기술 설정(Config) | 비즈니스 규칙 판단 | domain, application(포트 구현용) |

- 서비스는 흐름 조정(조회, 호출 순서, 트랜잭션)만 담당합니다. application에서 쓰는 스프링 기능은 `@Service`, `@Transactional`, 이벤트 발행처럼 특정 기술 구현에 묶이지 않는 것으로 한정합니다.
- 서비스 간 호출은 허용합니다. 순환 참조가 생기면 공통 로직을 도메인이나 별도 서비스로 분리합니다.
- 컨트롤러는 인증 주체를 받기 위해 `auth.domain.AuthenticatedAccount`를 참조할 수 있습니다. presentation이 application 밖을 참조하는 예외는 이것뿐입니다.

## 도메인 규칙의 위치

다른 객체가 어떤 객체의 내부를 알게 되면 결합도가 높아져 변경이 어려워집니다. 그래서 **도메인 규칙은 그 규칙에 필요한 데이터를 가진 객체에게 맡깁니다.**

- 값을 getter로 꺼내 서비스나 다른 객체에서 계산·판단하지 않습니다. 객체에게 일을 시킵니다.
- 서비스에 `if (x.getStatus() == ...)` 같은 분기나 `a.getB().getC()` 체인이 생기면 그 책임을 해당 객체로 옮깁니다.
- 불변식은 생성자와 상태 변경 메서드에서 검증하고, 위반하면 `ErrorCode`를 가진 `BusinessException`을 던집니다.
- getter는 협력에 필요한 것만 엽니다. 작성 방식은 [백엔드 지침의 엔티티 getter](../AGENTS.md#엔티티-getter)를 따릅니다.

```java
// 피할 예: 서비스가 Order의 상태를 꺼내 취소 가능 여부를 판단한다
if (order.getStatus() != OrderStatus.PAID) {
    throw new BusinessException(OrderErrorCode.NOT_CANCELABLE);
}
order.changeStatus(OrderStatus.CANCELED);

// 좋은 예: 취소 규칙을 가진 Order에게 맡긴다
order.cancel();
```

## Swagger 분리

Swagger 애너테이션(`io.swagger.v3.oas.annotations.*`)은 presentation의 `XxxApi` 인터페이스에만 두고, 컨트롤러는 그 인터페이스를 구현합니다. 컨트롤러에서 문서용 코드를 걷어내 요청 처리 흐름만 보이게 하기 위해서입니다.

| 위치 | 두는 것 |
| --- | --- |
| `XxxApi` 인터페이스 | `@Tag`, `@Operation`, `@ApiResponses`, `@Parameter`, `@SecurityRequirement` 등 문서 애너테이션 전부 |
| `XxxController` | `@RestController`, `@PostMapping` 등 매핑, `@Valid`, 서비스 호출과 응답 변환 |
| 요청/응답 DTO | 필드 설명용 `@Schema`는 DTO에 둡니다 |

- 컨트롤러에는 `io.swagger` import가 없어야 합니다.
- 이름은 컨트롤러와 짝을 맞춥니다(`TripController implements TripApi`).
- 기준 구현은 `trip/presentation`의 `TripApi`와 `TripController`입니다.

## 포트 사용 기준

포트는 **우리 서비스가 외부 시스템을 호출할 때(out 방향)만** 만듭니다. 외부가 우리 서비스를 호출하는 쪽(in 방향)은 이미 의존 방향이 안쪽이므로 인터페이스를 두지 않습니다.

| 상황 | 포트 | 이유 |
| --- | --- | --- |
| 결제(PG), 문자/알림, 메일, 외부 API 호출 | 만든다 | 업체 교체 가능성, 테스트 시 대체, 장애 격리 |
| 메시지 발행(Kafka, RabbitMQ 등) | 만든다 | 브로커 교체, 테스트 용이성 |
| Redis 등 캐시·임시 저장소, 분산 락 | 만든다 | 기술 교체, 테스트 용이성 |
| 파일 저장소(S3 등) | 만든다 | 로컬/클라우드 구현 교체 |
| RDB 저장/조회 | 만들지 않는다 | 리포지토리 순수 인터페이스로 대체(아래 리포지토리 규약) |
| Controller → Service | 만들지 않는다 | in 방향은 이미 의존 방향이 올바름 |

### 이름 규칙

- 포트: `기능 + Port` (예: `PaymentPort`, `OrderCachePort`, `NotificationPort`)
- 어댑터: `기술/업체 + 기능 + Adapter` (예: `TossPaymentAdapter`, `RedisOrderCacheAdapter`)
- 포트 메서드의 파라미터와 반환값에는 외부 기술 타입(업체 SDK 객체, `ResponseEntity` 등)을 쓰지 않고 도메인 타입이나 application 전용 DTO를 씁니다.

```java
// application/port
public interface PaymentPort {
    PaymentResult pay(PaymentRequest request);
}

// infrastructure/payment
@Component
class TossPaymentAdapter implements PaymentPort {
    public PaymentResult pay(PaymentRequest request) {
        // 업체 API 호출 및 응답 변환
    }
}
```

## 리포지토리 규약

서비스는 domain의 **순수 리포지토리 인터페이스만** 주입받습니다. `JpaRepository`를 상속한 인터페이스는 infrastructure에만 둡니다.

### 기본 구조

```java
// domain: 스프링 import 없음, 서비스가 실제로 쓰는 메서드만 선언
public interface OrderRepository {
    <S extends Order> S save(S order);
    Optional<Order> findById(Long id);
    List<Order> findByMemberIdAndStatus(Long memberId, OrderStatus status);
}

// infrastructure/persistence: Spring Data가 구현을 자동 생성
public interface OrderJpaRepository extends OrderRepository, JpaRepository<Order, Long> {
}
```

- `save`, `findById`처럼 `JpaRepository`와 시그니처가 같은 메서드는 Spring Data 기본 구현이 연결됩니다. `save`는 `JpaRepository`와 같은 제네릭 형태(`<S extends T> S save(S)`)로 선언합니다.
- `findBy...` 쿼리 메서드는 domain 인터페이스에 선언해도 메서드 이름으로 쿼리가 생성됩니다.
- `@Query`, `Pageable`·`Limit` 같은 Spring Data 타입이 필요한 메서드는 infrastructure 인터페이스에 선언합니다. 스프링 타입을 domain에 두지 않기 위해서입니다.
- domain 인터페이스에는 application이 실제로 쓰는 메서드만 선언합니다. 테스트 정리(`deleteAllInBatch` 등)처럼 그 밖의 연산이 필요한 테스트는 infrastructure 타입(`OrderJpaRepository`)을 주입받습니다.
- 리포지토리에 메서드를 추가하면 `DatabaseIntegrationTest` 기반 통합 테스트로 실제 쿼리가 동작하는지 확인합니다.

### 복잡한 조회

- 화면용 조회, 동적 검색, 통계처럼 도메인 로직과 무관한 조회는 조회 전용 리포지토리로 분리합니다.
- 인터페이스(`OrderQueryRepository`)는 application에 두고, 구현(`OrderQueryRepositoryImpl`)은 infrastructure에 둡니다.
- 조회 결과는 엔티티가 아니라 조회 전용 DTO로 반환합니다.

### MyBatis를 함께 쓸 때

현재 MyBatis와 QueryDSL은 도입하지 않았습니다. 도입할 때 아래 기준을 적용합니다.

서비스가 `TripRepository` 타입으로 주입받는 한 그 타입의 빈은 **하나만** 있어야 합니다. 같은 인터페이스에 JPA 구현과 MyBatis 구현을 둘 다 빈으로 등록하면 `NoUniqueBeanDefinitionException`이 발생합니다. 메서드 일부만 다른 기술로 구현해야 하면 아래 중 하나를 씁니다.

| 방법 | 빈 수 | 사용 시점 |
| --- | --- | --- |
| A. Spring Data 커스텀 구현 | 1 | 일부 메서드만 다른 기술로 구현하고 인터페이스는 하나로 유지할 때(**기본값**) |
| B. 직접 만든 어댑터가 위임 | 1 | 메서드별 구현을 명시적으로 통제해야 할 때 |
| C. 역할별 인터페이스 분리 | 2(타입도 2개) | 다른 기술로 구현할 메서드가 조회 전용일 때(위 "복잡한 조회") |

#### A. Spring Data 커스텀 구현

Spring Data 프록시는 하나지만 메서드마다 처리할 곳을 다르게 연결합니다. `save`, `findById`는 `SimpleJpaRepository`로, 커스텀 메서드는 `Impl` 클래스로 위임됩니다.

```java
// domain
public interface TripRepository {
    <S extends Trip> S save(S trip);                       // JPA 기본 구현
    Optional<Trip> findById(Long id);                      // JPA 기본 구현
    List<Trip> findByComplexCondition(TripSearch search);  // MyBatis로 구현
}

// infrastructure/persistence
interface TripRepositoryCustom {
    List<Trip> findByComplexCondition(TripSearch search);
}

@RequiredArgsConstructor
class TripRepositoryCustomImpl implements TripRepositoryCustom {
    private final TripMapper tripMapper;                   // MyBatis

    public List<Trip> findByComplexCondition(TripSearch search) {
        return tripMapper.findByComplexCondition(search);
    }
}

public interface TripJpaRepository
        extends TripRepository, TripRepositoryCustom, JpaRepository<Trip, Long> {
}
```

- 구현 클래스 이름은 기본 설정에서 `커스텀 인터페이스 이름 + Impl`이어야 합니다.
- 서비스는 `TripRepository` 하나만 주입받습니다.

#### B. 직접 만든 어댑터가 위임

어댑터 클래스가 `TripRepository`를 구현하고 메서드별로 JPA나 MyBatis에 위임합니다.

```java
// infrastructure/persistence
interface TripJpaRepository extends JpaRepository<Trip, Long> {
    // TripRepository를 상속하지 않는다
}

@Repository
@RequiredArgsConstructor
class TripRepositoryAdapter implements TripRepository {
    private final TripJpaRepository jpa;
    private final TripMapper mapper;

    public <S extends Trip> S save(S trip) { return jpa.save(trip); }
    public Optional<Trip> findById(Long id) { return jpa.findById(id); }
    public List<Trip> findByComplexCondition(TripSearch search) {
        return mapper.findByComplexCondition(search);
    }
}
```

- `TripJpaRepository`가 `TripRepository`를 상속하면 `TripRepository` 타입 빈이 두 개가 되므로 상속하지 않습니다.
- 모든 메서드에 위임 코드를 써야 하므로 A로 해결되지 않을 때만 씁니다.
