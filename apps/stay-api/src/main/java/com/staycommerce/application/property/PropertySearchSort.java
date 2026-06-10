package com.staycommerce.application.property;

/**
 * 검색 결과 정렬 기준.
 * - RECOMMENDED: 기본값. 정책 미정으로 찜 수 내림차순으로 갈음.
 * - PRICE_ASC: 총요금 오름차순.
 * - WISHES_DESC: 찜 수 내림차순.
 * (rating_desc는 평점 미모델링으로 미지원)
 */
public enum PropertySearchSort {
    RECOMMENDED,
    PRICE_ASC,
    WISHES_DESC
}
