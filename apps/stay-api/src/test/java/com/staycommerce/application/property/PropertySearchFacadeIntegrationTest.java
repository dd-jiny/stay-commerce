package com.staycommerce.application.property;

import com.staycommerce.domain.inventory.DailyRoomInventory;
import com.staycommerce.domain.inventory.DailyRoomInventoryRepository;
import com.staycommerce.domain.property.PropertyInfo;
import com.staycommerce.domain.property.PropertyService;
import com.staycommerce.domain.property.PropertyType;
import com.staycommerce.domain.rate.DailyRoomRateService;
import com.staycommerce.domain.roomtype.RoomTypeInfo;
import com.staycommerce.domain.roomtype.RoomTypeService;
import com.staycommerce.domain.wishlist.WishlistService;
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
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest
class PropertySearchFacadeIntegrationTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final String SEOUL = "서울";
    private static final LocalDate CHECK_IN = LocalDate.now(KST).plusDays(10);
    private static final LocalDate CHECK_OUT = CHECK_IN.plusDays(2); // 2박: [+10, +11]
    private static final List<LocalDate> STAY_DATES = CHECK_IN.datesUntil(CHECK_OUT).toList();

    @Autowired
    private PropertySearchFacade facade;

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

    private Long registerRoomType(Long propertyId, int standard, int max, int extraFee) {
        RoomTypeInfo info = roomTypeService.register(propertyId, "스탠다드", standard, max, extraFee, 5);
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
    private Long registerBookableRoomType(Long propertyId, int standard, int max, int extraFee,
                                          int pricePerNight, int stockPerNight) {
        Long roomTypeId = registerRoomType(propertyId, standard, max, extraFee);
        registerRates(roomTypeId, pricePerNight);
        saveInventory(roomTypeId, stockPerNight);
        return roomTypeId;
    }

    private PropertySearchCriteria criteria(int guests, PropertySearchSort sort) {
        return new PropertySearchCriteria(SEOUL, CHECK_IN, CHECK_OUT, guests, sort);
    }

    @DisplayName("정상 검색이면,")
    @Nested
    class Success {

        @DisplayName("총요금은 일자별 요금 합이고, 평균가는 총요금/박수다.")
        @Test
        void computesTotalAndAvg() {
            // arrange: 1박 100000 x 2박 = 200000, 기준 2인 / 2인 투숙 → 추가요금 0
            Long propertyId = registerProperty("스테이호텔", SEOUL);
            registerBookableRoomType(propertyId, 2, 4, 10000, 100000, 2);

            // act
            List<PropertySearchInfo> result = facade.search(criteria(2, PropertySearchSort.RECOMMENDED));

            // assert
            assertThat(result).hasSize(1);
            PropertySearchInfo info = result.get(0);
            assertAll(
                () -> assertThat(info.propertyId()).isEqualTo(propertyId),
                () -> assertThat(info.totalAmount()).isEqualTo(200000),
                () -> assertThat(info.avgPerNight()).isEqualTo(100000)
            );
        }

        @DisplayName("기준 인원을 초과하면, 추가 인원 요금이 박수만큼 반영된다.")
        @Test
        void appliesExtraPersonFee_whenOverStandard() {
            // arrange: 기준 2인 / 3인 투숙, 추가요금 10000/박 x 2박 = 20000, 기본 200000
            Long propertyId = registerProperty("스테이호텔", SEOUL);
            registerBookableRoomType(propertyId, 2, 4, 10000, 100000, 2);

            // act
            List<PropertySearchInfo> result = facade.search(criteria(3, PropertySearchSort.RECOMMENDED));

            // assert
            assertThat(result).hasSize(1);
            assertThat(result.get(0).totalAmount()).isEqualTo(220000);
        }
    }

    @DisplayName("후보가 걸러질 때,")
    @Nested
    class Filtering {

        @DisplayName("도시에 숙소가 없으면, 빈 결과를 반환한다.")
        @Test
        void returnsEmpty_whenNoPropertyInCity() {
            registerProperty("부산호텔", "부산");

            List<PropertySearchInfo> result = facade.search(criteria(2, PropertySearchSort.RECOMMENDED));

            assertThat(result).isEmpty();
        }

        @DisplayName("최대 수용 인원을 초과하는 객실만 있으면, 결과에서 제외된다.")
        @Test
        void excludesRoomType_whenGuestsExceedMaxOccupancy() {
            Long propertyId = registerProperty("스테이호텔", SEOUL);
            registerBookableRoomType(propertyId, 2, 2, 10000, 100000, 2); // max=2

            List<PropertySearchInfo> result = facade.search(criteria(4, PropertySearchSort.RECOMMENDED));

            assertThat(result).isEmpty();
        }

        @DisplayName("한 날짜라도 가용 재고가 0이면, 해당 객실은 제외된다.")
        @Test
        void excludesRoomType_whenAnyDateSoldOut() {
            Long propertyId = registerProperty("스테이호텔", SEOUL);
            Long roomTypeId = registerRoomType(propertyId, 2, 4, 10000);
            registerRates(roomTypeId, 100000);
            // 첫날 재고 1, 둘째날 재고 0
            inventoryRepository.save(new DailyRoomInventory(roomTypeId, STAY_DATES.get(0), 1));
            inventoryRepository.save(new DailyRoomInventory(roomTypeId, STAY_DATES.get(1), 0));

            List<PropertySearchInfo> result = facade.search(criteria(2, PropertySearchSort.RECOMMENDED));

            assertThat(result).isEmpty();
        }

        @DisplayName("한 날짜라도 요금이 누락되면, 해당 객실은 제외된다.")
        @Test
        void excludesRoomType_whenAnyDateRateMissing() {
            Long propertyId = registerProperty("스테이호텔", SEOUL);
            Long roomTypeId = registerRoomType(propertyId, 2, 4, 10000);
            saveInventory(roomTypeId, 2);
            // 첫날 요금만 등록, 둘째날 누락
            rateService.register(roomTypeId, STAY_DATES.get(0), 100000);

            List<PropertySearchInfo> result = facade.search(criteria(2, PropertySearchSort.RECOMMENDED));

            assertThat(result).isEmpty();
        }
    }

    @DisplayName("정렬할 때,")
    @Nested
    class Sorting {

        @DisplayName("price_asc는 총요금 오름차순으로 정렬한다.")
        @Test
        void sortsByPriceAsc() {
            Long cheap = registerProperty("저가호텔", SEOUL);
            registerBookableRoomType(cheap, 2, 4, 0, 50000, 2);   // total 100000
            Long pricey = registerProperty("고가호텔", SEOUL);
            registerBookableRoomType(pricey, 2, 4, 0, 150000, 2); // total 300000

            List<PropertySearchInfo> result = facade.search(criteria(2, PropertySearchSort.PRICE_ASC));

            assertThat(result).extracting(PropertySearchInfo::totalAmount)
                .containsExactly(100000, 300000);
        }

        @DisplayName("wishes_desc는 찜 수 내림차순으로 정렬한다.")
        @Test
        void sortsByWishesDesc() {
            Long popular = registerProperty("인기호텔", SEOUL);
            registerBookableRoomType(popular, 2, 4, 0, 100000, 2);
            Long quiet = registerProperty("한산호텔", SEOUL);
            registerBookableRoomType(quiet, 2, 4, 0, 100000, 2);
            // 인기호텔 2명 찜, 한산호텔 0명
            wishlistService.addIfAbsent(1L, popular);
            wishlistService.addIfAbsent(2L, popular);

            List<PropertySearchInfo> result = facade.search(criteria(2, PropertySearchSort.WISHES_DESC));

            assertAll(
                () -> assertThat(result).extracting(PropertySearchInfo::propertyId)
                    .containsExactly(popular, quiet),
                () -> assertThat(result).extracting(PropertySearchInfo::wishCount)
                    .containsExactly(2L, 0L)
            );
        }
    }
}
