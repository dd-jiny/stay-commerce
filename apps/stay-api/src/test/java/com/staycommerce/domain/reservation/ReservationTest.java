package com.staycommerce.domain.reservation;

import com.staycommerce.support.error.CoreException;
import com.staycommerce.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReservationTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final Long MEMBER_ID = 1L;
    private static final Long ROOM_TYPE_ID = 10L;
    private static final LocalDate TODAY = LocalDate.now(KST);

    private ZonedDateTime future() {
        return ZonedDateTime.now(KST).plusMinutes(15);
    }

    private ZonedDateTime past() {
        return ZonedDateTime.now(KST).minusMinutes(1);
    }

    /** PENDING 예약 (체크인 today+10, 2박, 만료 미래). */
    private Reservation pending() {
        return new Reservation(MEMBER_ID, ROOM_TYPE_ID, TODAY.plusDays(10), TODAY.plusDays(12), 2, 200000, future());
    }

    @DisplayName("예약을 생성할 때,")
    @Nested
    class Create {

        @DisplayName("유효한 정보면, PENDING 상태로 생성된다.")
        @Test
        void createsPending_whenValid() {
            Reservation r = pending();

            assertThat(r.getStatus()).isEqualTo(ReservationStatus.PENDING);
            assertThat(r.getTotalAmount()).isEqualTo(200000);
        }

        @DisplayName("체크아웃이 체크인 이하이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenCheckOutNotAfterCheckIn() {
            CoreException e = assertThrows(CoreException.class, () ->
                new Reservation(MEMBER_ID, ROOM_TYPE_ID, TODAY.plusDays(5), TODAY.plusDays(5), 2, 100000, future()));

            assertThat(e.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("체크인이 과거이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenCheckInIsPast() {
            CoreException e = assertThrows(CoreException.class, () ->
                new Reservation(MEMBER_ID, ROOM_TYPE_ID, TODAY.minusDays(1), TODAY.plusDays(1), 2, 100000, future()));

            assertThat(e.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("숙박 일수가 30박을 초과하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenNightsExceedMax() {
            CoreException e = assertThrows(CoreException.class, () ->
                new Reservation(MEMBER_ID, ROOM_TYPE_ID, TODAY.plusDays(1), TODAY.plusDays(32), 2, 100000, future()));

            assertThat(e.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("체크인이 1년을 초과하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenCheckInTooFarAhead() {
            CoreException e = assertThrows(CoreException.class, () ->
                new Reservation(MEMBER_ID, ROOM_TYPE_ID, TODAY.plusDays(366), TODAY.plusDays(367), 2, 100000, future()));

            assertThat(e.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("투숙 인원이 0이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenGuestsIsZero() {
            CoreException e = assertThrows(CoreException.class, () ->
                new Reservation(MEMBER_ID, ROOM_TYPE_ID, TODAY.plusDays(1), TODAY.plusDays(2), 0, 100000, future()));

            assertThat(e.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @DisplayName("기간을 계산할 때,")
    @Nested
    class Period {

        @DisplayName("stayDates는 체크아웃 당일을 제외한 일자 리스트를 반환한다.")
        @Test
        void stayDatesExcludesCheckOut() {
            Reservation r = new Reservation(MEMBER_ID, ROOM_TYPE_ID, TODAY.plusDays(1), TODAY.plusDays(4), 2, 300000, future());

            assertThat(r.stayDates()).containsExactly(TODAY.plusDays(1), TODAY.plusDays(2), TODAY.plusDays(3));
            assertThat(r.nights()).isEqualTo(3);
        }
    }

    @DisplayName("예약을 확정할 때,")
    @Nested
    class Confirm {

        @DisplayName("PENDING이고 만료 전이면, CONFIRMED로 전이된다.")
        @Test
        void confirmsSuccessfully_whenPendingAndNotExpired() {
            Reservation r = pending();

            r.confirm();

            assertThat(r.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        }

        @DisplayName("만료 시각을 지났으면, CONFLICT 예외가 발생한다.")
        @Test
        void throwsException_whenExpired() {
            Reservation r = new Reservation(MEMBER_ID, ROOM_TYPE_ID, TODAY.plusDays(10), TODAY.plusDays(12), 2, 200000, past());

            CoreException e = assertThrows(CoreException.class, r::confirm);

            assertThat(e.getErrorType()).isEqualTo(ErrorType.CONFLICT);
        }

        @DisplayName("이미 CONFIRMED이면, CONFLICT 예외가 발생한다.")
        @Test
        void throwsException_whenAlreadyConfirmed() {
            Reservation r = pending();
            r.confirm();

            CoreException e = assertThrows(CoreException.class, r::confirm);

            assertThat(e.getErrorType()).isEqualTo(ErrorType.CONFLICT);
        }
    }

    @DisplayName("예약을 취소할 때,")
    @Nested
    class Cancel {

        @DisplayName("PENDING이면, CANCELLED로 전이된다.")
        @Test
        void cancelsPending() {
            Reservation r = pending();

            r.cancel();

            assertThat(r.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        }

        @DisplayName("CONFIRMED이면, CANCELLED로 전이된다.")
        @Test
        void cancelsConfirmed() {
            Reservation r = pending();
            r.confirm();

            r.cancel();

            assertThat(r.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        }

        @DisplayName("CHECKED_IN이면, CONFLICT 예외가 발생한다.")
        @Test
        void throwsException_whenCheckedIn() {
            Reservation r = new Reservation(MEMBER_ID, ROOM_TYPE_ID, TODAY, TODAY.plusDays(1), 2, 100000, future());
            r.confirm();
            r.checkIn();

            CoreException e = assertThrows(CoreException.class, r::cancel);

            assertThat(e.getErrorType()).isEqualTo(ErrorType.CONFLICT);
        }
    }

    @DisplayName("체크인할 때,")
    @Nested
    class CheckIn {

        @DisplayName("CONFIRMED이고 체크인 날짜가 되면, CHECKED_IN으로 전이된다.")
        @Test
        void checksIn_whenConfirmedAndDateReached() {
            Reservation r = new Reservation(MEMBER_ID, ROOM_TYPE_ID, TODAY, TODAY.plusDays(1), 2, 100000, future());
            r.confirm();

            r.checkIn();

            assertThat(r.getStatus()).isEqualTo(ReservationStatus.CHECKED_IN);
        }

        @DisplayName("PENDING이면, CONFLICT 예외가 발생한다.")
        @Test
        void throwsException_whenPending() {
            Reservation r = new Reservation(MEMBER_ID, ROOM_TYPE_ID, TODAY, TODAY.plusDays(1), 2, 100000, future());

            CoreException e = assertThrows(CoreException.class, r::checkIn);

            assertThat(e.getErrorType()).isEqualTo(ErrorType.CONFLICT);
        }
    }

    @DisplayName("체크아웃할 때,")
    @Nested
    class CheckOut {

        @DisplayName("CHECKED_IN이면, CHECKED_OUT으로 전이된다.")
        @Test
        void checksOut_whenCheckedIn() {
            Reservation r = new Reservation(MEMBER_ID, ROOM_TYPE_ID, TODAY, TODAY.plusDays(1), 2, 100000, future());
            r.confirm();
            r.checkIn();

            r.checkOut();

            assertThat(r.getStatus()).isEqualTo(ReservationStatus.CHECKED_OUT);
        }

        @DisplayName("CONFIRMED이면, CONFLICT 예외가 발생한다.")
        @Test
        void throwsException_whenConfirmed() {
            Reservation r = pending();
            r.confirm();

            CoreException e = assertThrows(CoreException.class, r::checkOut);

            assertThat(e.getErrorType()).isEqualTo(ErrorType.CONFLICT);
        }
    }

    @DisplayName("만료 처리할 때,")
    @Nested
    class ExpireIfPending {

        @DisplayName("PENDING이면, CANCELLED로 전이되고 true를 반환한다.")
        @Test
        void expiresPending() {
            Reservation r = pending();

            boolean expired = r.expireIfPending();

            assertThat(expired).isTrue();
            assertThat(r.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        }

        @DisplayName("CONFIRMED이면, 상태 변화 없이 false를 반환한다. (멱등)")
        @Test
        void noOp_whenNotPending() {
            Reservation r = pending();
            r.confirm();

            boolean expired = r.expireIfPending();

            assertThat(expired).isFalse();
            assertThat(r.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        }
    }

    @DisplayName("소유자를 확인할 때,")
    @Nested
    class IsOwnedBy {

        @DisplayName("본인이면 true, 타인이면 false를 반환한다.")
        @Test
        void checksOwner() {
            Reservation r = pending();

            assertThat(r.isOwnedBy(MEMBER_ID)).isTrue();
            assertThat(r.isOwnedBy(999L)).isFalse();
        }
    }
}
