package com.bodeum.domain.lifetransition.rule.rules;

import com.bodeum.domain.lifetransition.enumtype.CurrentIncomeStatus;
import com.bodeum.domain.lifetransition.enumtype.EligibilityLevel;
import com.bodeum.domain.lifetransition.enumtype.PriorityLevel;
import com.bodeum.domain.lifetransition.enumtype.ProcedureType;
import com.bodeum.domain.lifetransition.rule.LifeTransitionRule;
import com.bodeum.domain.lifetransition.rule.RuleContext;
import com.bodeum.domain.lifetransition.rule.RuleDocument;
import com.bodeum.domain.lifetransition.rule.RuleItemResult;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 국민연금 납부예외 검토 규칙.
 * 현재 소득이 없는 경우, 소득이 없는 기간 동안 연금 보험료 납부예외를 신청할 수 있음을 안내한다.
 */
@Component
public class NationalPensionExceptionRule implements LifeTransitionRule {

    @Override
    public List<RuleItemResult> evaluate(RuleContext context) {
        if (context.currentIncomeStatus() != CurrentIncomeStatus.NONE) {
            return List.of();
        }

        RuleItemResult item = new RuleItemResult(
                ProcedureType.NATIONAL_PENSION_EXCEPTION,
                EligibilityLevel.NEEDS_CHECK,
                PriorityLevel.MEDIUM,
                "국민연금 납부예외 검토",
                "현재 소득이 없는 상태로, 소득이 없는 기간 동안 국민연금 납부예외를 신청하면 보험료 부담을 줄일 수 있습니다. 다만 납부예외 기간은 가입기간에 포함되지 않으므로, 추후 추납 여부와 함께 검토하세요.",
                "소득이 없어진 시점에 맞춰 신청하는 것이 좋습니다. 신청 전 기간은 소급 적용이 제한될 수 있습니다.",
                ProcedureType.NATIONAL_PENSION_EXCEPTION.getDefaultOfficialUrl(),
                List.of(
                        new RuleDocument("납부예외 신청서", "국민연금공단 지사/전자민원(내연금) 접수", "국민연금공단", true),
                        new RuleDocument("신분증", "본인 확인용", "본인", true)
                )
        );

        return List.of(item);
    }
}
