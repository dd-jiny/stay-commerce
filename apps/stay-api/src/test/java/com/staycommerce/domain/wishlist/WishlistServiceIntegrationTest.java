package com.staycommerce.domain.wishlist;

import com.staycommerce.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest
class WishlistServiceIntegrationTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long OTHER_MEMBER_ID = 2L;
    private static final Long PROPERTY_ID = 100L;

    @Autowired
    private WishlistService wishlistService;

    @Autowired
    private WishlistRepository wishlistRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("찜을 등록할 때,")
    @Nested
    class AddIfAbsent {

        @DisplayName("처음 찜하면, 1건이 저장된다.")
        @Test
        void savesWishlist_whenFirstTime() {
            // act
            WishlistInfo info = wishlistService.addIfAbsent(MEMBER_ID, PROPERTY_ID);

            // assert
            assertAll(
                () -> assertThat(info.id()).isNotNull(),
                () -> assertThat(info.memberId()).isEqualTo(MEMBER_ID),
                () -> assertThat(info.propertyId()).isEqualTo(PROPERTY_ID),
                () -> assertThat(wishlistRepository.findByMemberId(MEMBER_ID)).hasSize(1)
            );
        }

        @DisplayName("같은 숙소를 두 번 찜해도, 1건만 존재한다. (멱등)")
        @Test
        void isIdempotent_whenAddedTwice() {
            // act
            WishlistInfo first = wishlistService.addIfAbsent(MEMBER_ID, PROPERTY_ID);
            WishlistInfo second = wishlistService.addIfAbsent(MEMBER_ID, PROPERTY_ID);

            // assert
            assertAll(
                () -> assertThat(second.id()).isEqualTo(first.id()),
                () -> assertThat(wishlistRepository.findByMemberId(MEMBER_ID)).hasSize(1)
            );
        }
    }

    @DisplayName("찜을 해제할 때,")
    @Nested
    class RemoveOwned {

        @DisplayName("내가 찜한 숙소를 해제하면, 삭제된다.")
        @Test
        void removesWishlist_whenOwned() {
            // arrange
            wishlistService.addIfAbsent(MEMBER_ID, PROPERTY_ID);

            // act
            wishlistService.removeOwned(MEMBER_ID, PROPERTY_ID);

            // assert
            assertThat(wishlistRepository.existsByMemberIdAndPropertyId(MEMBER_ID, PROPERTY_ID)).isFalse();
        }

        @DisplayName("찜하지 않은 숙소를 해제해도, 에러가 없다. (멱등)")
        @Test
        void isIdempotent_whenNotWished() {
            assertThatCode(() -> wishlistService.removeOwned(MEMBER_ID, PROPERTY_ID))
                .doesNotThrowAnyException();
        }

        @DisplayName("타인의 회원 ID로 해제하면, 내 찜은 보존된다.")
        @Test
        void preservesMyWishlist_whenRemovedByOtherMember() {
            // arrange
            wishlistService.addIfAbsent(MEMBER_ID, PROPERTY_ID);

            // act: 타인이 같은 숙소를 해제 시도
            wishlistService.removeOwned(OTHER_MEMBER_ID, PROPERTY_ID);

            // assert
            assertThat(wishlistRepository.existsByMemberIdAndPropertyId(MEMBER_ID, PROPERTY_ID)).isTrue();
        }
    }

    @DisplayName("숙소별 찜 수를 집계할 때,")
    @Nested
    class CountByProperty {

        @DisplayName("3명이 찜하면, 찜 수는 3이다.")
        @Test
        void returnsCount_whenMultipleMembersWished() {
            // arrange
            wishlistService.addIfAbsent(1L, PROPERTY_ID);
            wishlistService.addIfAbsent(2L, PROPERTY_ID);
            wishlistService.addIfAbsent(3L, PROPERTY_ID);

            // act & assert
            assertThat(wishlistService.countByProperty(PROPERTY_ID)).isEqualTo(3);
        }

        @DisplayName("여러 숙소의 찜 수를 한 번에 조회하면, propertyId별 Map으로 반환한다.")
        @Test
        void returnsCountMap_whenQueriedInBatch() {
            // arrange: 숙소 100 → 2명, 숙소 200 → 1명, 숙소 300 → 0명
            wishlistService.addIfAbsent(1L, 100L);
            wishlistService.addIfAbsent(2L, 100L);
            wishlistService.addIfAbsent(1L, 200L);

            // act
            Map<Long, Long> counts = wishlistRepository.countByPropertyIdIn(List.of(100L, 200L, 300L));

            // assert: 찜 0건인 300L은 Map에 포함되지 않는다
            assertAll(
                () -> assertThat(counts).containsEntry(100L, 2L),
                () -> assertThat(counts).containsEntry(200L, 1L),
                () -> assertThat(counts).doesNotContainKey(300L)
            );
        }
    }
}
