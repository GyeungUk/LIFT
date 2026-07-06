package com.lift.domain.lifetransition.rule;

import java.util.List;

/**
 * 룰 엔진 전체 분석 결과. 우선순위로 정렬된 항목과 요약/점수를 담는다.
 */
public record RuleEngineResult(
        String summaryTitle,
        String summaryMessage,
        int totalPriorityScore,
        List<RuleItemResult> items
) {
}
