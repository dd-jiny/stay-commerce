# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

숙박 커머스 도메인의 Spring Boot 3.4.4 + Java 21 기반 Gradle 멀티 모듈 프로젝트입니다 (Gradle 8.13, Spring Cloud 2024.0.1, Testcontainers 2.0.2). 도메인 모델링과 에러 메시지는 한국어로 작성합니다.

## 빌드 및 테스트 명령어

```bash
# 전체 빌드
./gradlew build

# 전체 테스트
./gradlew test

# 특정 모듈 테스트
./gradlew :apps:stay-api:test

# 단일 테스트 클래스 실행
./gradlew :apps:stay-api:test --tests "com.staycommerce.domain.example.ExampleServiceIntegrationTest"

# 단일 테스트 메서드 실행 (Nested 클래스는 Outer.Inner.method 형태)
./gradlew :apps:stay-api:test --tests "com.staycommerce.domain.example.ExampleServiceIntegrationTest.Get.returnsExampleInfo_whenValidIdIsProvided"

# stay-api 애플리케이션 실행 (infra docker-compose 필요)
./gradlew :apps:stay-api:bootRun
```

테스트는 `spring.profiles.active=test`, `user.timezone=Asia/Seoul`로 실행됩니다 (root build.gradle.kts에서 설정). Testcontainers(MySQL, Redis)를 사용하므로 Docker가 필요합니다. 테스트 병렬 실행은 비활성화(`maxParallelForks = 1`).

## 로컬 인프라

```bash
# MySQL(3306), Redis master/replica(6379/6380), Kafka(19092), Kafka UI(9099) 기동
docker-compose -f ./docker/infra-compose.yml up

# 모니터링 (Prometheus + Grafana, localhost:3000, admin/admin)
docker-compose -f ./docker/monitoring-compose.yml up
```

## 멀티 모듈 구조

```
apps/       -> 실행 가능한 Spring Boot 애플리케이션 (BootJar 생성)
modules/    -> 도메인에 의존적이지 않은 재사용 가능한 인프라 설정 (plain jar)
supports/   -> logging, monitoring 등 부가 기능 add-on (plain jar)
```

**apps**는 **modules**와 **supports**에 의존하며, modules와 supports는 독립적입니다.

- **apps** — `stay-api` (REST API: Spring MVC, JPA, Redis, QueryDSL, Swagger/OpenAPI), `stay-batch` (Spring Batch 잡. `@ConditionalOnProperty(name = "spring.batch.job.name")`으로 활성화 제어, `SpringApplication.exit()`으로 종료 코드 반환), `stay-streamer` (Kafka 컨슈머)
- **modules** — `jpa` (Spring Data JPA + QueryDSL + `BaseEntity`/테스트 픽스처), `redis` (master/replica Redis), `kafka` (Spring Kafka)
- **supports** — `jackson` (커스텀 직렬화), `logging` (Logback + Slack appender), `monitoring` (Prometheus/Micrometer)
- modules/supports는 `java-test-fixtures`로 공유 테스트 설정을 노출 (예: Testcontainers config, `DatabaseCleanUp`은 `modules/jpa`의 testFixtures에 위치).

각 모듈 설정은 Spring config import 방식으로 앱의 `application.yml`에서 가져옵니다:
```yaml
spring.config.import: jpa.yml, redis.yml, logging.yml, monitoring.yml
```

프로필: `local`, `test`, `dev`, `qa`, `prd`. 모든 앱은 `@PostConstruct`에서 timezone을 `Asia/Seoul`로 설정합니다.

## 레이어드 아키텍처 (각 앱 내부)

각 앱은 `com.staycommerce` 패키지 아래에서 다음 레이어 구조를 따릅니다:

- **`interfaces/api/`** — Controller, DTO(`*V1Dto`), API 스펙 인터페이스(`*V1ApiSpec`, Swagger 어노테이션 분리). Controller는 `ApiResponse<T>`를 반환. URL 패턴은 `/api/v1/{resource}`.
- **`application/`** — Facade 클래스. 도메인 서비스를 조합하고, 도메인 모델을 `*Info` record로 변환.
- **`domain/`** — 도메인 모델(JPA 엔티티, `BaseEntity` 상속), 도메인 서비스, 리포지토리 인터페이스 (Spring Data 아님).
- **`infrastructure/`** — 리포지토리 구현체. Spring Data `JpaRepository`를 감싸서 도메인 리포지토리 인터페이스를 구현.
- **`support/error/`** — `CoreException` + `ErrorType` enum. 모든 비즈니스 에러는 `CoreException`을 사용.

주요 패턴:
- **Spring 어노테이션**: 도메인 서비스, Facade, 리포지토리 구현체 모두 `@Component`를 사용 (`@Service`/`@Repository` 아님).
- **리포지토리 추상화**: 도메인이 리포지토리 인터페이스를 정의하고(예: `ExampleRepository`), infrastructure에서 `JpaRepository`를 감싸 구현. 도메인은 Spring Data에 직접 의존하지 않음.
- **DTO 변환 체인**: 도메인 모델 → `*Info` record (application) → `*Response` record (interfaces). 각 레이어마다 `from()` 정적 팩토리 메서드로 변환.
- **API 스펙 분리**: Swagger 어노테이션은 인터페이스(`*V1ApiSpec`)에 선언하고, Controller가 이를 구현.
- **전역 에러 처리**: `ApiControllerAdvice`가 `CoreException`을 잡아 `ErrorType`의 HTTP 상태 코드와 한국어 에러 메시지로 매핑. `customMessage`가 있으면 기본 메시지 대신 사용.

## 엔티티 규약

모든 JPA 엔티티는 `BaseEntity`(`modules/jpa`)를 상속하며, 다음을 제공합니다:
- 자동 생성 `id` (IDENTITY 전략)
- `createdAt`, `updatedAt` (`@PrePersist`/`@PreUpdate`로 자동 관리)
- `deletedAt`을 활용한 소프트 삭제 (`delete()`/`restore()`는 멱등)
- `guard()` 훅: persist/update 시점에 엔티티 유효성 검증용
- 반드시 `@Table(name = "...")` 어노테이션을 명시해야 함 (`DatabaseCleanUp`이 `@Table.name`으로 truncate 대상을 수집)

## 테스트 규약

- **단위 테스트**: JUnit 5, 도메인 모델 불변식 검증. `@DisplayName`과 `@Nested`로 BDD 스타일 구조화.
- **통합 테스트**: `@SpringBootTest`, Testcontainers 사용 (MySQL: `MySqlTestContainersConfig`, Redis: `RedisTestContainersConfig`). `@AfterEach`에서 `DatabaseCleanUp.truncateAllTables()`로 정리.
- **E2E 테스트**: `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `TestRestTemplate`. HTTP 요청/응답 전체 흐름 검증.
- 테스트 라이브러리: AssertJ, Mockito, SpringMockK, Instancio (테스트 데이터 생성)
- 모듈 프로젝트의 테스트 픽스처는 Gradle `java-test-fixtures` 플러그인으로 공유 (`testFixtures(project(":modules:jpa"))` 형태로 의존)
- JaCoCo 커버리지 리포트는 XML로만 생성됩니다 (HTML/CSV 비활성화).

## DataSource 설정

DataSource는 표준 `spring.datasource.*`가 아닌 커스텀 `datasource.mysql-jpa.main.*` 접두사로 HikariCP를 설정합니다. `test` 프로필에서는 `MySqlTestContainersConfig`가 Testcontainers URL을 시스템 프로퍼티로 동적 주입합니다. JPA는 Hibernate의 timezone을 UTC로 저장(`NORMALIZE_UTC`)합니다.

## Redis 설정

Redis는 master/replica 토폴로지로 구성됩니다. 두 개의 `RedisTemplate`이 등록됩니다:
- **기본(Primary)**: `REPLICA_PREFERRED` — 읽기는 replica 우선
- **`redisTemplateMaster`**: `MASTER` — 쓰기 또는 강한 일관성이 필요한 읽기용

설정은 커스텀 `RedisProperties`(`redis.*` 접두사)를 사용합니다.

## 인증

인증이 필요한 모든 API 요청은 다음 헤더를 사용합니다 (JWT/Session 아님):
- `X-Loopers-LoginId` — 로그인 ID
- `X-Loopers-LoginPw` — 비밀번호

`AuthenticatedMemberResolver`(`interfaces/api/auth/`)가 헤더를 읽어 `MemberService.authenticate()`로 검증 후 `AuthenticatedMemberInfo`를 컨트롤러 인자로 주입합니다. 누락/실패 시 `UNAUTHORIZED`. 컨트롤러는 `@AuthenticatedMember` 어노테이션이 붙은 파라미터로 받습니다.

## 문서 / HTTP 요청 예시

- `docs/sessionXX/` — 주차별 퀘스트(요구사항) 원본
- `docs/design/` — 설계 산출물
  - 산출물: `01-requirements.md`, `02-sequence-diagrams.md`, `03-class-diagram.md`, `04-erd.md`
  - 전략/근거 문서(예약 도메인 핵심 결정): `05-auto-expiration-strategy.md`(PENDING 만료 = Kafka 지연 메시지 + 배치 안전망 하이브리드), `06-core-design-principles.md`(호텔/독채 통합 모델 등 핵심 원칙), `07-period-handling.md`(체크인~체크아웃 기간 규칙)
- `http/stay-api/*.http` — JetBrains HTTP Client용 API 요청 예시. 환경 변수는 `http/http-client.env.json`
- `.github/pull_request_template.md` — PR은 한국어 구조화 템플릿(Summary, Context & Decision, Design Overview, Flow Diagram)을 따름

## 도메인 & 객체 설계 전략
- 도메인 객체는 비즈니스 규칙을 캡슐화해야 합니다.
- 애플리케이션 서비스는 서로 다른 도메인을 조립해, 도메인 로직을 조정하여 기능을 제공해야 합니다.
- 규칙이 여러 서비스에 나타나면 도메인 객체에 속할 가능성이 높습니다.
- 일자 기반 재고 / 요금은 (room_type_id, date) 단위 도메인 객체로 모델링합니다.
- 각 기능에 대한 책임과 결합도에 대해 개발자의 의도를 확인하고 개발을 진행합니다.

### 예약 도메인 핵심 모델 (설계 완료 / volume-3 구현 진행 중)

현재 코드에 **실제 구현된 도메인은 `example`, `member` 두 개뿐**이며, 숙박 예약 도메인(`Property`, `RoomType`, `DailyRoomInventory`, `DailyRoomRate`, `Wishlist`, `Reservation`)은 `docs/design/`에 설계만 존재합니다. 현재 작업 브랜치(`volume-3`, `docs/session03/`)가 이 도메인을 코드로 구현하는 퀘스트입니다. 구현 시 다음 원칙을 따릅니다 (상세: `docs/design/06`, `07`).

- **숙소 통합 모델**: 모든 숙소는 `Property → RoomType → DailyRoomInventory(stock, held)` 단일 구조로 표현. 독채는 RoomType 1개 + `stock=1`인 특수 케이스로, 별도 모델 분기를 만들지 않습니다.
- **재고 차감**: `UPDATE held = held + 1 WHERE stock - held >= 1` — stock 절댓값과 무관하게 동작.
- **기간 의미론**: 체크인 inclusive, 체크아웃 exclusive. `Reservation.stayDates()` = `checkIn.datesUntil(checkOut)` (체크아웃 당일 미점유).
- **PENDING 자동 만료**: Kafka 지연 메시지(정시 처리) + 배치(안전망) 하이브리드. 진실의 근원은 DB `expires_at`.

## 아키텍처, 패키지 구성 전략
- 본 프로젝트는 레이어드 아키텍처를 따르며, DIP (의존성 역전 원칙) 을 준수합니다.
- API request, response DTO와 응용 레이어의 DTO는 분리해 작성하도록 합니다.
- 패키징 전략은 4개 레이어 패키지를 두고, 하위에 도메인 별로 패키징하는 형태로 작성합니다.
    - 예시
      > /interfaces/api (presentation 레이어 - API)
      /application/.. (application 레이어 - 도메인 레이어를 조합해 사용 가능한 기능을 제공)
      /domain/.. (domain 레이어 - 도메인 객체 및 엔티티, Repository 인터페이스가 위치)
      /infrastructure/.. (infrastructure 레이어 - JPA, Redis 등을 활용해 Repository 구현체를 제공)