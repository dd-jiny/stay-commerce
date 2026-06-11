package com.staycommerce.domain.wishlist;

import com.staycommerce.support.error.CoreException;
import com.staycommerce.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WishlistTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long PROPERTY_ID = 100L;

    @DisplayName("찜 엔티티를 생성할 때,")
    @Nested
    class Create {

        @DisplayName("회원 ID와 숙소 ID가 유효하면, 찜이 생성된다.")
        @Test
        void createsSuccessfully_whenValidIdsAreProvided() {
            // act
            Wishlist wishlist = new Wishlist(MEMBER_ID, PROPERTY_ID);

            // assert
            assertAll(
                () -> assertThat(wishlist.getMemberId()).isEqualTo(MEMBER_ID),
                () -> assertThat(wishlist.getPropertyId()).isEqualTo(PROPERTY_ID)
            );
        }

        @DisplayName("회원 ID가 null이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenMemberIdIsNull() {
            CoreException exception = assertThrows(CoreException.class, () ->
                new Wishlist(null, PROPERTY_ID));

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("숙소 ID가 null이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenPropertyIdIsNull() {
            CoreException exception = assertThrows(CoreException.class, () ->
                new Wishlist(MEMBER_ID, null));

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }
}
