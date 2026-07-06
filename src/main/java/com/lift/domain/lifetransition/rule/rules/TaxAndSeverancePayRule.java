package com.lift.domain.lifetransition.rule.rules;

import com.lift.domain.lifetransition.enumtype.EligibilityLevel;
import com.lift.domain.lifetransition.enumtype.PriorityLevel;
import com.lift.domain.lifetransition.enumtype.ProcedureType;
import com.lift.domain.lifetransition.rule.LifeTransitionRule;
import com.lift.domain.lifetransition.rule.RuleContext;
import com.lift.domain.lifetransition.rule.RuleDocument;
import com.lift.domain.lifetransition.rule.RuleItemResult;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 세금/퇴직금 체크 규칙.
 * 고용관계 종료가 수반되는 모든 케이스(퇴직/이직/실직)에 세금 정산과 퇴직금 확인 항목을 추가한다.
 */
@Component
public class TaxAndSeverancePayRule implements LifeTransitionRule {

    @Override
    public List<RuleItemResult> evaluate(RuleContext context) {
        if (!context.involvesEmploymentEnd()) {
            return List.of();
        }

        RuleItemResult severance = new RuleItemResult(
                ProcedureType.SEVERANCE_PAY,
                EligibilityLevel.NEEDS_CHECK,
                PriorityLevel.MEDIUM,
                "퇴직금 정산 확인",
                "계속근로기간 1년 이상이면 퇴직금이 발생합니다. 퇴직일로부터 14일 이내에 지급되어야 하며, 지급액과 퇴직소득세가 올바르게 계산됐는지 확인하세요.",
                "퇴직일로부터 14일 이내 지급이 원칙입니다. 미지급 시 고용노동부에 진정 제기가 가능합니다.",
                ProcedureType.SEVERANCE_PAY.getDefaultOfficialUrl(),
                List.of(
                        new RuleDocument("퇴직금 산정 내역서", "지급액/산정 기준 확인", "이전 직장", false),
                        new RuleDocument("근로계약서", "근속기간 및 급여 확인", "이전 직장 / 본인", false)
                )
        );

        RuleItemResult tax = new RuleItemResult(
                ProcedureType.TAX_CHECK,
                EligibilityLevel.NEEDS_CHECK,
                PriorityLevel.LOW,
                "세금 정산 체크(연말정산/종합소득세)",
                "중도 퇴직 시 연말정산이 자동으로 마무리되지 않을 수 있습니다. 재취업하면 새 직장에서 합산 정산하고, 연내 재취업하지 않으면 다음 해 5월 종합소득세 신고로 정산해야 환급/추가납부를 확정할 수 있습니다.",
                "연내 미취업 시 다음 해 5월 종합소득세 신고 기간에 정산하세요.",
                ProcedureType.TAX_CHECK.getDefaultOfficialUrl(),
                List.of(
                        new RuleDocument("원천징수영수증", "이전 직장 근로소득 확인", "이전 직장", false),
                        new RuleDocument("소득·세액공제 증빙", "의료비/보험료/기부금 등", "본인", false)
                )
        );

        return List.of(severance, tax);
    }
}
