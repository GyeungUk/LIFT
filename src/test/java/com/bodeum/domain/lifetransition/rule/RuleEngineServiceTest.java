package com.bodeum.domain.lifetransition.rule;

import static org.assertj.core.api.Assertions.assertThat;

import com.bodeum.domain.lifetransition.enumtype.CurrentIncomeStatus;
import com.bodeum.domain.lifetransition.enumtype.EligibilityLevel;
import com.bodeum.domain.lifetransition.enumtype.LifeEventType;
import com.bodeum.domain.lifetransition.enumtype.NextJobStatus;
import com.bodeum.domain.lifetransition.enumtype.ProcedureType;
import com.bodeum.domain.lifetransition.enumtype.ResignationReason;
import com.bodeum.domain.lifetransition.rule.rules.HealthInsuranceContinuationRule;
import com.bodeum.domain.lifetransition.rule.rules.NationalPensionExceptionRule;
import com.bodeum.domain.lifetransition.rule.rules.TaxAndSeverancePayRule;
import com.bodeum.domain.lifetransition.rule.rules.UnemploymentBenefitRule;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 룰 엔진 단위 테스트. Spring 컨텍스트 없이 룰을 직접 조립해 결정적 계산을 검증한다.
 */
class RuleEngineServiceTest {

    private final RuleEngineService ruleEngineService = new RuleEngineService(List.of(
            new UnemploymentBenefitRule(),
            new HealthInsuranceContinuationRule(),
            new NationalPensionExceptionRule(),
            new TaxAndSeverancePayRule()
    ));

    private RuleContext context(
            LifeEventType eventType,
            ResignationReason resignationReason,
            NextJobStatus nextJobStatus,
            Integer insuranceMonths,
            CurrentIncomeStatus incomeStatus
    ) {
        return new RuleContext(eventType, resignationReason, nextJobStatus, insuranceMonths, incomeStatus, "서울특별시", "강남구");
    }

    private Optional<RuleItemResult> findItem(RuleEngineResult result, ProcedureType procedureType) {
        return result.items().stream()
                .filter(item -> item.procedureType() == procedureType)
                .findFirst();
    }

    @Test
    void 계약만료_퇴직에_고용보험_6개월이상이면_실업급여_HIGH() {
        RuleEngineResult result = ruleEngineService.analyze(context(
                LifeEventType.RETIREMENT,
                ResignationReason.CONTRACT_EXPIRED,
                NextJobStatus.NOT_CONFIRMED,
                12,
                CurrentIncomeStatus.NONE
        ));

        Optional<RuleItemResult> benefit = findItem(result, ProcedureType.UNEMPLOYMENT_BENEFIT);
        assertThat(benefit).isPresent();
        assertThat(benefit.get().eligibilityLevel()).isEqualTo(EligibilityLevel.HIGH);
    }

    @Test
    void 고용보험_6개월미만이면_실업급여_LOW() {
        RuleEngineResult result = ruleEngineService.analyze(context(
                LifeEventType.UNEMPLOYMENT,
                ResignationReason.CONTRACT_EXPIRED,
                NextJobStatus.CONFIRMED,
                3,
                CurrentIncomeStatus.HAS_INCOME
        ));

        Optional<RuleItemResult> benefit = findItem(result, ProcedureType.UNEMPLOYMENT_BENEFIT);
        assertThat(benefit).isPresent();
        assertThat(benefit.get().eligibilityLevel()).isEqualTo(EligibilityLevel.LOW);
    }

    @Test
    void 자발적_사유는_실업급여_NEEDS_CHECK() {
        RuleEngineResult result = ruleEngineService.analyze(context(
                LifeEventType.RETIREMENT,
                ResignationReason.PERSONAL_REASON,
                NextJobStatus.CONFIRMED,
                24,
                CurrentIncomeStatus.HAS_INCOME
        ));

        Optional<RuleItemResult> benefit = findItem(result, ProcedureType.UNEMPLOYMENT_BENEFIT);
        assertThat(benefit).isPresent();
        assertThat(benefit.get().eligibilityLevel()).isEqualTo(EligibilityLevel.NEEDS_CHECK);
    }

    @Test
    void 다음일자리_미확정이면_건강보험_임의계속가입_항목_추가() {
        RuleEngineResult withUnconfirmed = ruleEngineService.analyze(context(
                LifeEventType.JOB_CHANGE,
                ResignationReason.PERSONAL_REASON,
                NextJobStatus.NOT_CONFIRMED,
                10,
                CurrentIncomeStatus.HAS_INCOME
        ));
        assertThat(findItem(withUnconfirmed, ProcedureType.HEALTH_INSURANCE_CONTINUATION)).isPresent();

        RuleEngineResult withConfirmed = ruleEngineService.analyze(context(
                LifeEventType.JOB_CHANGE,
                ResignationReason.PERSONAL_REASON,
                NextJobStatus.CONFIRMED,
                10,
                CurrentIncomeStatus.HAS_INCOME
        ));
        assertThat(findItem(withConfirmed, ProcedureType.HEALTH_INSURANCE_CONTINUATION)).isEmpty();
    }

    @Test
    void 현재소득이_없으면_국민연금_납부예외_항목_추가() {
        RuleEngineResult noIncome = ruleEngineService.analyze(context(
                LifeEventType.UNEMPLOYMENT,
                ResignationReason.COMPANY_CLOSURE,
                NextJobStatus.NOT_CONFIRMED,
                8,
                CurrentIncomeStatus.NONE
        ));
        assertThat(findItem(noIncome, ProcedureType.NATIONAL_PENSION_EXCEPTION)).isPresent();

        RuleEngineResult hasIncome = ruleEngineService.analyze(context(
                LifeEventType.UNEMPLOYMENT,
                ResignationReason.COMPANY_CLOSURE,
                NextJobStatus.NOT_CONFIRMED,
                8,
                CurrentIncomeStatus.HAS_INCOME
        ));
        assertThat(findItem(hasIncome, ProcedureType.NATIONAL_PENSION_EXCEPTION)).isEmpty();
    }

    @Test
    void 모든_퇴직케이스에_세금과_퇴직금_체크가_포함된다() {
        for (LifeEventType eventType : LifeEventType.values()) {
            RuleEngineResult result = ruleEngineService.analyze(context(
                    eventType,
                    ResignationReason.UNKNOWN,
                    NextJobStatus.UNKNOWN,
                    5,
                    CurrentIncomeStatus.UNKNOWN
            ));

            assertThat(findItem(result, ProcedureType.TAX_CHECK))
                    .as("%s 에 세금 체크 포함", eventType)
                    .isPresent();
            assertThat(findItem(result, ProcedureType.SEVERANCE_PAY))
                    .as("%s 에 퇴직금 체크 포함", eventType)
                    .isPresent();
        }
    }

    @Test
    void HIGH_항목이_우선순위와_점수에_반영되어_상위에_정렬된다() {
        RuleEngineResult result = ruleEngineService.analyze(context(
                LifeEventType.RETIREMENT,
                ResignationReason.RECOMMENDED_RESIGNATION,
                NextJobStatus.NOT_CONFIRMED,
                12,
                CurrentIncomeStatus.NONE
        ));

        // 실업급여(HIGH/HIGH)가 첫 항목이어야 한다.
        assertThat(result.items().get(0).procedureType()).isEqualTo(ProcedureType.UNEMPLOYMENT_BENEFIT);
        assertThat(result.totalPriorityScore()).isPositive();
        // sortOrder는 서비스에서 부여되므로 여기서는 정렬 결과만 확인한다.
        assertThat(result.items()).isNotEmpty();
    }
}
