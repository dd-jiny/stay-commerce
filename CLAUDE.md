# CLAUDE.md

이 파일은 Claude Code (claude.ai/code)가 이 저장소에서 작업할 때 참고하는 가이드입니다.

## 프로젝트 개요

숙박 커머스 도메인의 Spring Boot 3.4 + Java 21 기반 Gradle 멀티 모듈 프로젝트입니다 (Gradle 8.13). 도메인 모델링과 에러 메시지는 한국어로 작성합니다.

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

# 단일 테스트 메서드 실행
./gradlew :apps:stay-api:test --tests "com.staycommerce.domain.example.ExampleServiceIntegrationTest.Get.returnsExampleInfo_whenValidIdIsProvided"

# stay-api 애플리케이션 실행 (infra docker-compose 필요)
./gradlew :apps:stay-api:bootRun
```

테스트는 `spring.profiles.active=test`, `user.timezone=Asia/Seoul`로 실행됩니다 (root build.gradle.kts에서 설정). Testcontainers(MySQL, Redis)를 사용하므로 Docker가 필요합니다.

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

- `stay-api` — REST API 서버 (Spring MVC, JPA, Redis, Swagger/OpenAPI)
- `stay-batch` — Spring Batch 잡. `@ConditionalOnProperty(name = "spring.batch.job.name")`으로 활성화 제어.
- `stay-streamer` — Kafka 컨슈머 애플리케이션

각 모듈 설정은 Spring config import 방식으로 가져옵니다 (`jpa.yml`, `redis.yml`, `logging.yml`, `monitoring.yml`).

## 레이어드 아키텍처 (각 앱 내부)

각 앱은 `com.staycommerce` 패키지 아래에서 다음 레이어 구조를 따릅니다:

- **`interfaces/api/`** — Controller, DTO(`*V1Dto`), API 스펙 인터페이스(`*V1ApiSpec`, Swagger 어노테이션 분리). Controller는 `ApiResponse<T>`를 반환.
- **`application/`** — Facade 클래스. 도메인 서비스를 조합하고, 도메인 모델을 `*Info` record로 변환.
- **`domain/`** — 도메인 모델(JPA 엔티티, `BaseEntity` 상속), 도메인 서비스, 리포지토리 인터페이스 (Spring Data 아님).
- **`infrastructure/`** — 리포지토리 구현체. Spring Data `JpaRepository`를 감싸서 도메인 리포지토리 인터페이스를 구현.
- **`support/error/`** — `CoreException` + `ErrorType` enum. 모든 비즈니스 에러는 `CoreException`을 사용.

주요 패턴:
- **리포지토리 추상화**: 도메인이 리포지토리 인터페이스를 정의하고(예: `ExampleRepository`), infrastructure에서 `JpaRepository`를 감싸 구현. 도메인은 Spring Data에 직접 의존하지 않음.
- **DTO 변환 체인**: 도메인 모델 -> `*Info` record (application) -> `*Response` record (interfaces). 각 레이어마다 `from()` 정적 팩토리 메서드로 변환.
- **API 스펙 분리**: Swagger 어노테이션은 인터페이스(`*V1ApiSpec`)에 선언하고, Controller가 이를 구현.
- **전역 에러 처리**: `ApiControllerAdvice`가 `CoreException`을 잡아 `ErrorType`의 HTTP 상태 코드와 한국어 에러 메시지로 매핑.

## 엔티티 규약

모든 JPA 엔티티는 `BaseEntity`(`modules/jpa`)를 상속하며, 다음을 제공합니다:
- 자동 생성 `id` (IDENTITY 전략)
- `createdAt`, `updatedAt` (`@PrePersist`/`@PreUpdate`로 자동 관리)
- `deletedAt`을 활용한 소프트 삭제 (`delete()`/`restore()`는 멱등)
- `guard()` 훅: persist/update 시점에 엔티티 유효성 검증용

## 테스트 규약

- **단위 테스트**: JUnit 5, 도메인 모델 불변식 검증
- **통합 테스트**: `@SpringBootTest`, Testcontainers 사용 (MySQL: `MySqlTestContainersConfig`, Redis: `RedisTestContainersConfig`). `@AfterEach`에서 `DatabaseCleanUp.truncateAllTables()`로 정리.
- **E2E 테스트**: `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `TestRestTemplate`. HTTP 요청/응답 전체 흐름 검증.
- 테스트 라이브러리: AssertJ, Mockito, SpringMockK, Instancio (테스트 데이터 생성)
- 모듈 프로젝트의 테스트 픽스처는 Gradle `java-test-fixtures` 플러그인으로 공유

## DataSource 설정

DataSource는 표준 `spring.datasource.*`가 아닌 커스텀 `datasource.mysql-jpa.main.*` 접두사로 HikariCP를 설정합니다. `test` 프로필에서는 `MySqlTestContainersConfig`가 Testcontainers URL을 시스템 프로퍼티로 동적 주입합니다.
