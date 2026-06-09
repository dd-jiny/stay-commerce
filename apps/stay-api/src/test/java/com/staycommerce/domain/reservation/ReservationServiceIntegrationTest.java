package com.staycommerce.domain.reservation;

import com.staycommerce.support.error.CoreException;
import com.staycommerce.support.error.ErrorType;
import com.staycommerce.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class ReservationServiceIntegrationTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final Long MEMBER_ID = 1L;
    private static final Long ROOM_TYPE_ID = 10L;
    private static final LocalDate TODAY = LocalDate.now(KST);

    private Reservation createPending(Long memberId) {
        return reservationService.createPending(
            memberId, ROOM_TYPE_ID, TODAY.plusDays(10), TODAY.plusDays(12),
            2, 200000, ZonedDateTime.now(KST).plusMinutes(15));
    }

    @DisplayName("예약을 생성할 때,")
    @Nested
    class CreatePending {

        @DisplayName("PENDING 예약이 저장되고 id로 조회된다.")
        @Test
        void savesPending() {
            Reservation saved = createPending(MEMBER_ID);

            Reservation found = reservationService.findById(saved.getId());
            assertThat(found.getStatus()).isEqualTo(ReservationStatus.PENDING);
            assertThat(found.getMemberId()).isEqualTo(MEMBER_ID);
        }
    }

    @DisplayName("소유자 검증 조회를 할 때,")
    @Nested
    class FindOwned {

        @DisplayName("본인 예약이면, 반환한다.")
        @Test
        void returnsOwnedReservation() {
            Reservation saved = createPending(MEMBER_ID);

            Reservation found = reservationService.findOwned(saved.getId(), MEMBER_ID);

            assertThat(found.getId()).isEqualTo(saved.getId());
        }

        @DisplayName("타인 예약이면, NOT_FOUND 예외가 발생한다.")
        @Test
        void throwsException_whenNotOwner() {
            Reservation saved = createPending(MEMBER_ID);

            CoreException e = assertThrows(CoreException.class, () ->
                reservationService.findOwned(saved.getId(), 999L));

            assertThat(e.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        }
    }

    @DisplayName("내 예약 목록을 조회할 때,")
    @Nested
    class FindMyReservations {

        @DisplayName("해당 회원의 예약만 반환한다.")
        @Test
        void returnsOnlyMyReservations() {
            createPending(MEMBER_ID);
            createPending(MEMBER_ID);
            createPending(2L);

            List<ReservationInfo> mine = reservationService.findMyReservations(MEMBER_ID);

            assertThat(mine).hasSize(2);
            assertThat(mine).extracting(ReservationInfo::memberId).containsOnly(MEMBER_ID);
        }
    }
}
