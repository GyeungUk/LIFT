package com.bodeum.domain.lifetransition.rule;

import java.util.List;

/**
 * 하나의 행정 절차 판단 규칙.
 *
 * <p>AI가 자격을 판단하지 않는다. 각 규칙은 사용자 입력({@link RuleContext})만 보고
 * 결정적으로 항목을 산출한다. 초기에는 Java 코드로 구현하지만, 이 인터페이스 단위로
 * 추후 rules.json 또는 DB 기반 룰 로더로 대체할 수 있도록 분리해 둔다.
 */
public interface LifeTransitionRule {

    /**
     * 규칙을 평가한다. 해당 상황과 무관하면 빈 리스트를 반환한다.
     * (하나의 규칙이 복수 항목을 낼 수 있어 List로 반환한다.)
     */
    List<RuleItemResult> evaluate(RuleContext context);
}
