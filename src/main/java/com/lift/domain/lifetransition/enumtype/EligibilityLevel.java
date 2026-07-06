package com.lift.domain.lifetransition.enumtype;

/**
 * 특정 행정 절차에 대한 신청 가능성 수준.
 * AI가 아닌 룰 엔진이 사용자 입력을 기반으로 계산한다.
 */
public enum EligibilityLevel {
    HIGH,
    NEEDS_CHECK,
    LOW
}
