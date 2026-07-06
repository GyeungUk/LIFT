package com.lift.domain.lifetransition.rule;

import com.lift.domain.lifetransition.enumtype.CurrentIncomeStatus;
import com.lift.domain.lifetransition.enumtype.LifeEventType;
import com.lift.domain.lifetransition.enumtype.NextJobStatus;
import com.lift.domain.lifetransition.enumtype.ResignationReason;
import com.lift.domain.lifetransition.model.LifeAssessment;

/**
 * 룰 평가에 필요한 사용자 입력만 추려낸 불변 컨텍스트.
 * 엔티티(LifeAssessment)에 직접 의존하지 않고 값만 다루므로, 룰을 순수 함수로 유지하고
 * 추후 rules.json / DB 기반 룰로 옮길 때 입력 계약을 그대로 재사용할 수 있다.
 */
public record RuleContext(
        LifeEventType eventType,
        ResignationReason resignationReason,
        NextJobStatus nextJobStatus,
        Integer employmentInsuranceMonths,
        CurrentIncomeStatus currentIncomeStatus,
        String regionSido,
        String regionSigungu
) {

    public static RuleContext from(LifeAssessment assessment) {
        return new RuleContext(
                assessment.getEventType(),
                assessment.getResignationReason(),
                assessment.getNextJobStatus(),
                assessment.getEmploymentInsuranceMonths(),
                assessment.getCurrentIncomeStatus(),
                assessment.getRegionSido(),
                assessment.getRegionSigungu()
        );
    }

    public int employmentInsuranceMonthsOrZero() {
        return employmentInsuranceMonths == null ? 0 : employmentInsuranceMonths;
    }

    /**
     * 고용관계 종료가 수반되는 이벤트인지. 세금/퇴직금 체크 등 공통 항목 판단에 사용.
     */
    public boolean involvesEmploymentEnd() {
        return eventType == LifeEventType.RETIREMENT
                || eventType == LifeEventType.UNEMPLOYMENT
                || eventType == LifeEventType.JOB_CHANGE;
    }
}
