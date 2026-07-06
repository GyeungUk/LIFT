package com.bodeum.domain.lifetransition.rule.rules;

import com.bodeum.domain.lifetransition.enumtype.EligibilityLevel;
import com.bodeum.domain.lifetransition.enumtype.NextJobStatus;
import com.bodeum.domain.lifetransition.enumtype.PriorityLevel;
import com.bodeum.domain.lifetransition.enumtype.ProcedureType;
import com.bodeum.domain.lifetransition.rule.LifeTransitionRule;
import com.bodeum.domain.lifetransition.rule.RuleContext;
import com.bodeum.domain.lifetransition.rule.RuleDocument;
import com.bodeum.domain.lifetransition.rule.RuleItemResult;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 건강보험 임의계속가입 검토 규칙.
 * 다음 일자리가 확정되지 않은 경우, 지역가입자 전환 시 보험료 부담이 커질 수 있어 검토 항목을 추가한다.
 */
@Component
public class HealthInsuranceContinuationRule implements LifeTransitionRule {

    @Override
    public List<RuleItemResult> evaluate(RuleContext context) {
        if (context.nextJobStatus() != NextJobStatus.NOT_CONFIRMED) {
            return List.of();
        }

        RuleItemResult item = new RuleItemResult(
                ProcedureType.HEALTH_INSURANCE_CONTINUATION,
                EligibilityLevel.NEEDS_CHECK,
                PriorityLevel.MEDIUM,
                "건강보험 임의계속가입 검토",
                "다음 일자리가 확정되지 않아 직장가입자에서 지역가입자로 전환됩니다. 임의계속가입을 신청하면 최대 36개월간 기존 직장 보험료 수준으로 납부할 수 있어, 지역가입자 보험료와 비교해 유리한 쪽을 선택하세요.",
                "지역가입자 최초 보험료 납부기한이 지난 날부터 2개월 이내에 신청해야 합니다.",
                ProcedureType.HEALTH_INSURANCE_CONTINUATION.getDefaultOfficialUrl(),
                List.of(
                        new RuleDocument("임의계속(가입) 신청서", "국민건강보험공단 지사 방문/우편/팩스 접수", "국민건강보험공단", true),
                        new RuleDocument("신분증", "본인 확인용", "본인", true)
                )
        );

        return List.of(item);
    }
}
