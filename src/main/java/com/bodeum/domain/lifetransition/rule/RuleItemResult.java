package com.bodeum.domain.lifetransition.rule;

import com.bodeum.domain.lifetransition.enumtype.EligibilityLevel;
import com.bodeum.domain.lifetransition.enumtype.PriorityLevel;
import com.bodeum.domain.lifetransition.enumtype.ProcedureType;
import java.util.List;

/**
 * 개별 룰이 산출하는 리포트 항목 결과(엔티티 변환 전의 순수 값).
 */
public record RuleItemResult(
        ProcedureType procedureType,
        EligibilityLevel eligibilityLevel,
        PriorityLevel priorityLevel,
        String title,
        String reason,
        String deadlineText,
        String officialUrl,
        List<RuleDocument> requiredDocuments
) {
}
