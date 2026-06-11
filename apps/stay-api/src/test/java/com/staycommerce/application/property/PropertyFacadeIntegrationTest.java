package com.staycommerce.application.property;

import com.staycommerce.application.property.PropertyDetailInfo.RoomTypeDetail;
import com.staycommerce.domain.inventory.DailyRoomInventory;
import com.staycommerce.domain.inventory.DailyRoomInventoryRepository;
import com.staycommerce.domain.property.PropertyInfo;
import com.staycommerce.domain.property.PropertyService;
import com.staycommerce.domain.property.PropertyType;
import com.staycommerce.domain.rate.DailyRoomRateService;
import com.staycommerce.domain.roomtype.RoomTypeInfo;
import com.staycommerce.domain.roomtype.RoomTypeService;
import com.staycommerce.domain.wishlist.WishlistService;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest
class PropertyFacadeIntegrationTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final String SEOUL = "서울";
    private static final LocalDate CHECK_IN = LocalDate.now(KST).plusDays(10);
    private static final LocalDate CHECK_OUT = CHECK_IN.plusDays(2); // 2박: [+10, +11]
    private static final List<LocalDate> STAY_DATES = CHECK_IN.datesUntil(CHECK_OUT).toList();

    @Autowired
    private PropertyFacade facade;

    @Autowired
    private PropertyService propertyService;

    @Autowired
    private RoomTypeService roomTypeService;

    @Autowired
    private DailyRoomRateService rateService;

    @Autowired
    private DailyRoomInventoryRepository inventoryRepository;

    @Autowired
    private WishlistService wishlistService;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    private Long registerProperty(String name, String city) {
        PropertyInfo info = propertyService.register(name, city, city + " 주소", null, PropertyType.HOTEL);
        return info.id();
    }

    private Long registerRoomType(Long propertyId, String name, int standard, int max, int extraFee) {
        RoomTypeInfo info = roomTypeService.register(propertyId, name, standard, max, extraFee, 5);
        return info.id();
    }

    private void registerRates(Long roomTypeId, int pricePerNight) {
        for (LocalDate date : STAY_DATES) {
            rateService.register(roomTypeId, date, pricePerNight);
        }
    }

    private void saveInventory(Long roomTypeId, int stockPerNight) {
        for (LocalDate date : STAY_DATES) {
            inventoryRepository.save(new DailyRoomInventory(roomTypeId, date, stockPerNight));
        }
    }

    /** 전일자 요금/재고가 갖춰진 예약 가능한 객실 타입을 만든다. */
    private Long registerBookableRoomType(Long propertyId, String name, int standard, int max, int extraFee,
                                          int pricePerNight, int stockPerNight) {
        Long roomTypeId = registerRoomType(propertyId, name, standard, max, extraFee);
        registerRates(roomTypeId, pricePerNight);
        saveInventory(roomTypeId, stockPerNight);
        return roomTypeId;
    }

    @DisplayName("존재하지 않는 숙소를 조회하면, NOT_FOUND 예외가 발생한다.")
    @Test
    void throwsNotFound_whenPropertyMissing() {
        assertThatThrownBy(() -> facade.getPropertyDetail(99999L))
            .isInstanceOf(CoreException.class)
            .extracting("errorType").isEqualTo(ErrorType.NOT_FOUND);
    }

    @DisplayName("기간 없이 조회하면,")
    @Nested
    class WithoutPeriod {

        @DisplayName("숙소 메타 + 객실 타입 + 찜 수를 제공하고, 가용/요금은 비어있다.")
        @Test
        void returnsMetadataAndWishCount() {
            Long propertyId = registerProperty("스테이호텔", SEOUL);
            registerRoomType(propertyId, "스탠다드", 2, 4, 10000);
            registerRoomType(propertyId, "디럭스", 2, 6, 20000);
            wishlistService.addIfAbsent(1L, propertyId);
            wishlistService.addIfAbsent(2L, propertyId);

            PropertyDetailInfo detail = facade.getPropertyDetail(propertyId);

            assertAll(
                () -> assertThat(detail.propertyId()).isEqualTo(propertyId),
                () -> assertThat(detail.city()).isEqualTo(SEOUL),
                () -> assertThat(detail.wishCount()).isEqualTo(2L),
                () -> assertThat(detail.roomTypes()).hasSize(2),
                () -> assertThat(detail.roomTypes())
                    .extracting(RoomTypeDetail::name)
                    .containsExactlyInAnyOrder("스탠다드", "디럭스"),
                () -> assertThat(detail.roomTypes())
                    .allSatisfy(rt -> assertAll(
                        () -> assertThat(rt.available()).isNull(),
                        () -> assertThat(rt.totalAmount()).isNull(),
                        () -> assertThat(rt.avgPerNight()).isNull()))
            );
        }
    }

    @DisplayName("기간/인원을 주면,")
    @Nested
    class WithPeriod {

        private RoomTypeDetail findByName(PropertyDetailInfo detail, String name) {
            return detail.roomTypes().stream()
                .filter(rt -> rt.name().equals(name))
                .findFirst().orElseThrow();
        }

        @DisplayName("예약 가능한 객실은 가용=true, 총요금=일자별 요금 합, 평균가=총요금/박수를 채운다.")
        @Test
        void fillsPreview_whenBookable() {
            // 1박 100000 x 2박 = 200000, 기준 2인 / 2인 투숙 → 추가요금 0
            Long propertyId = registerProperty("스테이호텔", SEOUL);
            registerBookableRoomType(propertyId, "스탠다드", 2, 4, 10000, 100000, 2);

            PropertyDetailInfo detail = facade.getPropertyDetail(propertyId, CHECK_IN, CHECK_OUT, 2);
            RoomTypeDetail standard = findByName(detail, "스탠다드");

            assertAll(
                () -> assertThat(standard.available()).isTrue(),
                () -> assertThat(standard.totalAmount()).isEqualTo(200000),
                () -> assertThat(standard.avgPerNight()).isEqualTo(100000)
            );
        }

        @DisplayName("기준 인원을 초과하면, 추가 인원 요금이 박수만큼 반영된다.")
        @Test
        void appliesExtraPersonFee_whenOverStandard() {
            // 기준 2인 / 3인 투숙, 추가요금 10000/박 x 2박 = 20000, 기본 200000
            Long propertyId = registerProperty("스테이호텔", SEOUL);
            registerBookableRoomType(propertyId, "스탠다드", 2, 4, 10000, 100000, 2);

            PropertyDetailInfo detail = facade.getPropertyDetail(propertyId, CHECK_IN, CHECK_OUT, 3);

            assertThat(findByName(detail, "스탠다드").totalAmount()).isEqualTo(220000);
        }

        @DisplayName("최대 수용 인원을 초과하는 객실은 가용=false, 요금은 비어있다.")
        @Test
        void marksUnavailable_whenGuestsExceedMaxOccupancy() {
            Long propertyId = registerProperty("스테이호텔", SEOUL);
            registerBookableRoomType(propertyId, "스탠다드", 2, 2, 10000, 100000, 2); // max=2

            PropertyDetailInfo detail = facade.getPropertyDetail(propertyId, CHECK_IN, CHECK_OUT, 4);
            RoomTypeDetail standard = findByName(detail, "스탠다드");

            assertAll(
                () -> assertThat(standard.available()).isFalse(),
                () -> assertThat(standard.totalAmount()).isNull(),
                () -> assertThat(standard.avgPerNight()).isNull()
            );
        }

        @DisplayName("한 날짜라도 재고가 0이면, 해당 객실은 가용=false다.")
        @Test
        void marksUnavailable_whenAnyDateSoldOut() {
            Long propertyId = registerProperty("스테이호텔", SEOUL);
            Long roomTypeId = registerRoomType(propertyId, "스탠다드", 2, 4, 10000);
            registerRates(roomTypeId, 100000);
            inventoryRepository.save(new DailyRoomInventory(roomTypeId, STAY_DATES.get(0), 1));
            inventoryRepository.save(new DailyRoomInventory(roomTypeId, STAY_DATES.get(1), 0));

            PropertyDetailInfo detail = facade.getPropertyDetail(propertyId, CHECK_IN, CHECK_OUT, 2);

            assertThat(findByName(detail, "스탠다드").available()).isFalse();
        }

        @DisplayName("한 날짜라도 요금이 누락되면, 해당 객실은 가용=false다.")
        @Test
        void marksUnavailable_whenAnyDateRateMissing() {
            Long propertyId = registerProperty("스테이호텔", SEOUL);
            Long roomTypeId = registerRoomType(propertyId, "스탠다드", 2, 4, 10000);
            saveInventory(roomTypeId, 2);
            rateService.register(roomTypeId, STAY_DATES.get(0), 100000); // 둘째날 누락

            PropertyDetailInfo detail = facade.getPropertyDetail(propertyId, CHECK_IN, CHECK_OUT, 2);

            assertThat(findByName(detail, "스탠다드").available()).isFalse();
        }

        @DisplayName("가용/불가 객실이 섞여 있어도, 모든 객실을 가용 여부와 함께 반환한다.")
        @Test
        void returnsAllRoomTypes_withMixedAvailability() {
            Long propertyId = registerProperty("스테이호텔", SEOUL);
            registerBookableRoomType(propertyId, "예약가능", 2, 4, 0, 100000, 2);
            registerRoomType(propertyId, "매진", 2, 4, 0); // 재고/요금 없음 → 불가

            PropertyDetailInfo detail = facade.getPropertyDetail(propertyId, CHECK_IN, CHECK_OUT, 2);

            assertAll(
                () -> assertThat(detail.roomTypes()).hasSize(2),
                () -> assertThat(findByName(detail, "예약가능").available()).isTrue(),
                () -> assertThat(findByName(detail, "매진").available()).isFalse()
            );
        }
    }
}
