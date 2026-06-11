package com.staycommerce.application.property;

import java.time.LocalDate;

/**
 * 숙소 검색 조건. 기간 의미론: 체크인 inclusive / 체크아웃 exclusive.
 * sort가 null이면 RECOMMENDED로 간주한다.
 */
public record PropertySearchCriteria(
    String city,
    LocalDate checkIn,
    LocalDate checkOut,
    int guests,
    PropertySearchSort sort
) {
    public PropertySearchSort sortOrDefault() {
        return sort == null ? PropertySearchSort.RECOMMENDED : sort;
    }
}
