package com.lift.domain.lifetransition.enumtype;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 리포트 항목의 우선순위. weight는 totalPriorityScore 계산에 사용된다.
 */
@Getter
@RequiredArgsConstructor
public enum PriorityLevel {
    HIGH(3),
    MEDIUM(2),
    LOW(1);

    private final int weight;
}
