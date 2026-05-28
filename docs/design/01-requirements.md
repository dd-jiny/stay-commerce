# 01. 요구사항 명세

## 1. 개요

숙박 커머스 서비스의 핵심 도메인(숙소, 객실 타입, 일자별 재고/요금, 찜, 예약)에 대한 기능 요구사항을 정의한다. 본 문서는 다음 주차 개발의 기준이 되며, 도메인 모델·상태 머신·핵심 정책 결정을 포함한다.

### 범위

| 포함 | 제외 |
|------|------|
| 숙소 검색, 객실 타입 조회 | 결제 연동 (다음 스프린트) |
| 일자별 재고/요금 관리 | 환불·위약금 정책 |
| 예약 생성/취소/만료, 상태 머신 | 가변 가격(주말/성수기 자동 계산) |
| 재고 홀딩 (stock/held) | 그룹 예약, 복수 객실타입 예약 |
| 찜 (회원 전용, Property 단위) | 비로그인 찜, RoomType 단위 찜 |
| Kafka 기반 예약 만료 처리 | 외부 알림(SMS/메일) 발송 |

### 회원 도메인

회원 도메인(가입·로그인·내 정보 조회·비밀번호 수정)은 1주차에 완료되어 있으므로 본 문서에서는 인증된 사용자가 행위 주체임을 전제로 한다. 인증은 `X-Loopers-LoginId`/`X-Loopers-LoginPw` 헤더로 처리된다.

---

## 2. 핵심 도메인 용어

| 용어 | 설명 |
|------|------|
| **Property** | 숙소. 호텔/펜션/리조트/독채 등을 모두 포함하는 단위 |
| **RoomType** | Property에 속하는 객실 타입. 기준/최대 인원, 추가 인원 요금, 총 객실 수 보유 |
| **DailyRoomInventory** | `(room_type_id, date)` 단위 재고. 총 재고(`stock`)와 홀딩 수량(`held`)으로 구성. 예약 가능 수량 = `stock - held` |
| **DailyRoomRate** | `(room_type_id, date)` 단위 요금. 가격은 일자별 관리자 설정 |
| **Reservation** | 예약. 1예약 = 1객실타입 × 1개. 체크인~체크아웃 기간의 모든 일자 재고를 점유 |
| **Wishlist** | 회원의 Property 찜 기록. `(member_id, property_id)` 유니크 |

### 핵심 설계 원칙

- **호텔과 독채를 동일 구조로 표현**: 독채는 RoomType 1개 + `stock=1`인 특수 케이스. 별도 모델 분기 없음
- **재고와 요금 분리**: 변경 주기가 다르므로 테이블 분리. 같은 키 `(room_type_id, date)` 공유
- **홀딩 우선 차감**: PENDING 시 `held` 증가, CONFIRMED 시 `held → stock` 이전

---

## 3. 기능 요구사항

### 3.0 기간 처리 공통 규칙

모든 일자 기반 API(검색·예약·취소·체크인·체크아웃)에 공통 적용되는 기간 정책. 상세 규칙·동시성 시나리오·경계 케이스는 `07-period-handling.md` 참조.

**날짜 의미론**
- 체크인 inclusive, **체크아웃 exclusive** (호텔 업계 표준)
- 점유 일자 리스트: `[checkIn, checkIn+1, …, checkOut-1]`
- 박수(`nights`) = `checkOut - checkIn` 일수
- 모든 일자는 **KST(Asia/Seoul) 기준**으로 해석

**기간 유효성 검증** (위반 시 `BAD_REQUEST`)

| 규칙 | 값 |
|------|-----|
| 체크인 ≥ 오늘 | `checkIn >= LocalDate.now(KST)` (과거 예약 불가) |
| 최소 1박 | `checkOut > checkIn` |
| **최대 30박** ⚠ 정책 가정 | `nights <= 30` (트랜잭션 비대화 방지) |
| **사전 예약 범위 365일** ⚠ 정책 가정 | `checkIn <= today + 365` (재고 사전 적재 범위와 일치) |
| 인원수 1명 이상 | `guests >= 1` |

**기간 단위 데이터 무결성**
- 기간 내 모든 일자에 inventory 행과 rate 행이 모두 존재해야 함
- 한 날짜라도 누락되면 **검색에서 제외, 예약 시 `CONFLICT`**
- 사전 적재는 stay-batch 일배치에서 보장

**기간 단위 동시성**
- 다일자 차감은 항상 `date ASC` 순서로 수행 → 데드락 회피
- 한 날짜라도 실패 시 트랜잭션 롤백으로 자동 복원

---

### 3.1 숙소 검색

**유저 시나리오**
> 사용자는 도시·체크인·체크아웃·인원수를 입력하여 예약 가능한 숙소를 검색하고, 해당 기간의 합산 가격과 1박 평균 가격을 확인한다.

**요청 파라미터**
- `city`: 도시명 (필수)
- `checkIn`, `checkOut`: 체크인/체크아웃 날짜 (필수, **§3.0 기간 유효성 검증 적용**)
- `guests`: 투숙 인원수 (필수, 1 이상)

**필터 조건**
- Property가 해당 도시에 위치
- RoomType의 `maxOccupancy >= guests`
- 체크인~체크아웃 사이 **모든 일자**의 재고가 `stock - held >= 1`
- 기간 내 누락된 inventory/rate 행이 없을 것 (§3.0)

**응답**
- 각 결과 항목: 숙소명, 객실 타입명, **합산 가격**, **1박 평균 가격**, 기준/최대 인원
- 합산 가격 = `Σ(일자별 요금) + max(0, guests - standardOccupancy) × extraPersonFee × 박수`
- 평균 가격 = 합산 가격 / 박수

**비기능 요구사항**
- 검색은 읽기 전용 트랜잭션
- 향후 검색 엔진(ES) 분리를 고려해 인터페이스로 추상화

---

### 3.2 예약 생성

**유저 시나리오**
> 사용자는 객실 타입을 선택해 예약을 생성한다. 예약 시 체크인~체크아웃 사이 모든 일자별 재고가 홀딩되며, 한 날짜라도 재고가 부족하면 전체 예약이 실패한다. 예약은 15분간 PENDING 상태로 유지되고, 시간 내 확정되지 않으면 자동 취소된다.

**요청 파라미터**
- `roomTypeId`: 객실 타입 ID
- `checkIn`, `checkOut`: 체크인/체크아웃 날짜 (**§3.0 기간 유효성 검증 적용**)
- `guests`: 투숙 인원수

**처리 규칙**
- 인증된 회원만 예약 가능
- `guests <= maxOccupancy` 검증
- 체크인~체크아웃 사이 **모든 일자에 대해 `date ASC` 순서로 `held += 1`을 원자적으로 수행** (§3.0 동시성 규칙)
- 한 날짜라도 `stock - held < 1`이면 전체 트랜잭션 롤백
- 기간 내 inventory/rate 누락 시 `CONFLICT` (§3.0)
- 예약 생성 시점의 `total_amount`를 Reservation에 **고정 저장** (이후 요금 변경 영향 없음)
- 초기 상태: `PENDING`, `expires_at = now() + 15분`
- 트랜잭션 커밋 후 Kafka로 만료 처리 메시지 발행

**응답**: 예약 ID, 상태, 만료 시각, 합산 금액

---

### 3.3 예약 확정 (CONFIRMED 전이)

**유저 시나리오**
> 결제가 없는 MVP 단계에서는 별도 확정 API로 PENDING → CONFIRMED 전이를 수행한다. (다음 스프린트에서 결제 콜백으로 대체)

**처리 규칙**
- PENDING 상태에서만 가능
- `expires_at > now()` 검증 (만료된 예약은 확정 불가)
- 모든 일자에 대해 `held -= 1, stock -= 1` 원자적 수행
- 상태 → `CONFIRMED`

---

### 3.4 예약 취소

**유저 시나리오**
> 사용자는 본인의 PENDING 또는 CONFIRMED 예약을 취소할 수 있다. 체크인 이후에는 취소 불가하다.

**처리 규칙**
- 본인 예약만 취소 가능 (`reservation.member_id == 인증 회원 ID`)
- 상태별 분기:
  - PENDING → 각 일자 `held -= 1`
  - CONFIRMED → 각 일자 `stock += 1`
  - CHECKED_IN, CHECKED_OUT, CANCELLED → `CONFLICT` 에러
- 상태 변경 + 재고 복원은 **동일 트랜잭션**

---

### 3.5 체크인 / 체크아웃

**처리 규칙**
- CHECKED_IN: CONFIRMED 상태에서만 가능, `check_in <= today`
- CHECKED_OUT: CHECKED_IN 상태에서만 가능
- 이 단계에서는 재고 변동 없음 (이미 CONFIRMED 시점에 stock 차감 완료)

---

### 3.6 PENDING 자동 만료

**유저 시나리오**
> 예약 생성 후 15분 내 확정되지 않으면 시스템이 자동으로 CANCELLED 처리하고 재고를 복원한다.

**처리 흐름**
- 예약 생성 시 Kafka로 지연 메시지 발행 (`delay=15분`)
- stay-streamer가 메시지 수신 → `expireIfStillPending(reservationId)` 호출
- 멱등 처리: 이미 CONFIRMED/CANCELLED면 무시
- PENDING이면 → 각 일자 `held -= 1`, 상태 → CANCELLED

**안전망**
- Kafka 메시지 유실 대비: `expires_at < now() AND status = 'PENDING'`인 예약을 stay-batch가 주기적으로 정리

---

### 3.7 찜 (Wishlist)

**유저 시나리오**
> 인증된 사용자는 Property를 찜하거나 찜을 해제할 수 있고, 본인이 찜한 Property 목록을 조회할 수 있다.

**처리 규칙**
- 회원 전용 (비로그인 불가)
- 동일 회원이 동일 Property를 중복 찜 불가 (`(member_id, property_id)` 유니크)
- 찜하기는 멱등 (이미 찜한 상태에서 다시 호출해도 에러 아님)
- 찜 해제는 본인 찜만 가능

---

## 4. 예약 상태 머신

```
                    [예약 생성]
                        │
                        ▼
                  ┌─────────┐
       ┌──────────│ PENDING │──────────┐
       │          └─────────┘          │
       │ 확정 API/결제      취소 / 15분 만료
       ▼                                ▼
  ┌───────────┐                  ┌───────────┐
  │ CONFIRMED │─────취소────────▶│ CANCELLED │
  └───────────┘                  └───────────┘
       │
       │ 체크인 (체크인 당일 이후)
       ▼
  ┌────────────┐
  │ CHECKED_IN │
  └────────────┘
       │
       │ 체크아웃
       ▼
  ┌─────────────┐
  │ CHECKED_OUT │
  └─────────────┘
```

| 현재 상태 | 가능한 전이 | 조건 |
|-----------|-----------|------|
| `PENDING` | `CONFIRMED` | `expires_at > now()` |
| `PENDING` | `CANCELLED` | 사용자 취소 또는 15분 경과 |
| `CONFIRMED` | `CHECKED_IN` | `check_in <= today` |
| `CONFIRMED` | `CANCELLED` | 사용자 취소 (체크인 전) |
| `CHECKED_IN` | `CHECKED_OUT` | 항상 가능 |
| `CHECKED_OUT`, `CANCELLED` | (종결 상태) | 전이 불가 |

### 상태별 재고 영향

| 전이 | held | stock |
|------|------|-------|
| 생성 → PENDING | `+= 1` | - |
| PENDING → CONFIRMED | `-= 1` | `-= 1` |
| PENDING → CANCELLED | `-= 1` | - |
| CONFIRMED → CANCELLED | - | `+= 1` |
| CONFIRMED → CHECKED_IN → CHECKED_OUT | - | - |

---

## 5. 비기능 요구사항 / 정책

### 5.1 동시성

- 다일자 재고 차감은 **원자적 UPDATE**로 처리: `UPDATE daily_room_inventory SET held = held + 1 WHERE room_type_id = ? AND date = ? AND stock - held >= 1`
- 영향행 수 = 차감 대상 날짜 수가 되어야 성공. 하나라도 부족하면 트랜잭션 롤백
- 데드락 방지를 위해 다일자 락은 항상 `date ASC` 순서로 획득

### 5.2 트랜잭션 경계

- 예약 생성: 재고 홀딩 + Reservation 저장 = 동일 트랜잭션
- Kafka 메시지 발행: **트랜잭션 커밋 후** (TransactionalEventListener AFTER_COMMIT 또는 트랜잭셔널 아웃박스)
- 예약 취소: 상태 변경 + 재고 복원 = 동일 트랜잭션

### 5.3 가격 일관성

- 예약 생성 시점의 `total_amount`를 reservation 테이블에 고정
- 이후 관리자가 DailyRoomRate를 변경해도 기존 예약 금액은 불변

### 5.4 인증/인가

- 모든 예약·찜 API는 `X-Loopers-LoginId`/`X-Loopers-LoginPw` 헤더 필수
- 본인 자원만 조회·수정·삭제 가능

---

## 6. 결정사항 요약

| # | 항목 | 결정 |
|---|------|------|
| 1 | 재고 모델 | `stock` / `held` 분리 필드 |
| 2 | 숙소 유형 | 호텔 + 독채 통합 (`stock=1` = 독채) |
| 3 | 홀딩 정책 | 숙소 유형 불문 통일 (PENDING + held → CONFIRMED + stock 차감) |
| 4 | 요금/재고 모델 | `daily_room_inventory` + `daily_room_rate` 분리 |
| 5 | 예약 단위 | 1예약 = 1객실타입 × 1개 |
| 6 | 취소 가능 상태 | PENDING, CONFIRMED만 |
| 7 | 결제 연동 | MVP에서 제외 |
| 8 | 홀딩 타임아웃 | 15분, Kafka 지연 메시지 (stay-streamer) |
| 9 | 인원수 모델 | `standardOccupancy` + `maxOccupancy` 분리, 초과 시 추가 요금 |
| 10 | 취소 시 재고 복원 | 자동 (동일 트랜잭션) |
| 11 | 찜 | 회원 전용, Property 단위 |
| 12 | 날짜 의미론 | 체크인 inclusive, 체크아웃 exclusive (KST 기준) |
| 13 | 최대 숙박 일수 ⚠ | 30박 (정책 가정, 운영팀 확정 필요) |
| 14 | 사전 예약 범위 ⚠ | 오늘 + 365일 (정책 가정, 재고 적재 범위와 일치) |
| 15 | 기간 내 데이터 누락 시 | 검색 제외 / 예약 시 `CONFLICT` |
| 16 | 다일자 차감 순서 | `date ASC` (데드락 회피) |
